package com.appero.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.appero.sdk.analytics.ApperoAnalyticsListener
import com.appero.sdk.ui.config.FeedbackFlowConfig
import com.appero.sdk.ui.config.FeedbackPromptConfig
import com.appero.sdk.ui.theme.ApperoTheme
import com.appero.sdk.ui.theme.DefaultTheme

// Consistent spacing constants
private object FeedbackSpacing {
    val tiny = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val xlarge = 32.dp
    val inputHeight = 88.dp
    val iconSize = 24.dp
    val ratingSize = 50.dp
}

// Consistent text styles
private object FeedbackTextStyles {
    val titleColor = Color(0xFF003143)
    val cornerRadius = 8.dp
    val inputBackgroundColor = Color(0xFFF5F5F5)
}

@Composable
fun rememberImeState(): State<Boolean> {
    val view = LocalView.current
    var isImeVisible by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = android.view.ViewTreeObserver.OnPreDrawListener {
            isImeVisible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            true
        }
        view.viewTreeObserver.addOnPreDrawListener(listener)
        onDispose { view.viewTreeObserver.removeOnPreDrawListener(listener) }
    }

    return remember { derivedStateOf { isImeVisible } }
}

sealed class FeedbackStep {
    object Rating : FeedbackStep()
    object Frustration : FeedbackStep()
    object ThankYou : FeedbackStep()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackPrompt(
    visible: Boolean,
    config: FeedbackPromptConfig,
    theme: ApperoTheme = DefaultTheme(),
    analyticsListener: ApperoAnalyticsListener? = null,
    onSubmit: (rating: Int, feedback: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    flowConfig: FeedbackFlowConfig = FeedbackFlowConfig(),
    reviewPromptThreshold: Int = 4,
    onRequestReview: () -> Unit = {},
    initialStep: FeedbackStep? = null
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var feedbackText by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf<FeedbackStep>(initialStep ?: FeedbackStep.Rating) }
    val imeState = rememberImeState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(initialStep) { if (initialStep != null) currentStep = initialStep }
    
    // Expand bottom sheet when rating is selected to ensure CTA is visible
    LaunchedEffect(selectedRating) {
        if (selectedRating > 0) {
            bottomSheetState.expand()
        }
    }
    
    // Expand bottom sheet for frustration flow to ensure all content is visible
    LaunchedEffect(currentStep) {
        if (currentStep is FeedbackStep.Frustration) {
            bottomSheetState.expand()
        }
    }
    
    // Keep sheet expanded when keyboard hides or user tries to collapse
    LaunchedEffect(bottomSheetState.targetValue, selectedRating, currentStep) {
        if ((selectedRating > 0 || currentStep is FeedbackStep.Frustration) && 
            bottomSheetState.targetValue != SheetValue.Expanded) {
            bottomSheetState.expand()
        }
    }

    if (visible) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            verticalArrangement = Arrangement.Bottom
        ) {
            ModalBottomSheet(
                windowInsets = WindowInsets.ime,
                onDismissRequest = onDismiss,
                dragHandle = null,
                modifier = modifier,
                containerColor = Color.White,
                sheetState = bottomSheetState
            ) {
                when (currentStep) {
                    is FeedbackStep.Rating -> {
                        RatingStepContent(
                            config = config,
                            theme = theme,
                            imeState = imeState,
                            selectedRating = selectedRating,
                            feedbackText = feedbackText,
                            onRatingSelected = { rating ->
                                selectedRating = rating
                                analyticsListener?.onRatingSelected(rating)
                            },
                            onFeedbackTextChanged = { text ->
                                if (text.length <= config.maxCharacters) feedbackText = text
                            },
                            onSubmit = {
                                onSubmit(selectedRating, feedbackText)
                                currentStep = FeedbackStep.ThankYou
                            },
                            onDismiss = onDismiss
                        )
                    }
                    is FeedbackStep.Frustration -> {
                        FrustrationStepContent(
                            config = config,
                            theme = theme,
                            imeState = imeState,
                            feedbackText = feedbackText,
                            onFeedbackTextChanged = { text ->
                                if (text.length <= config.maxCharacters) feedbackText = text
                            },
                            onSubmit = {
                                onSubmit(0, feedbackText)
                                currentStep = FeedbackStep.ThankYou
                            },
                            onDismiss = onDismiss
                        )
                    }
                    is FeedbackStep.ThankYou -> {
                        ThankYouStepContent(
                            flowConfig = flowConfig,
                            theme = theme,
                            selectedRating = selectedRating,
                            reviewPromptThreshold = reviewPromptThreshold,
                            onRequestReview = onRequestReview,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

// Reusable Close Button Component
@Composable
private fun CloseButton(
    theme: ApperoTheme,
    onDismiss: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = onDismiss, modifier = Modifier.size(FeedbackSpacing.iconSize)) {
            Icon(
                Icons.Default.Close, 
                contentDescription = "Close", 
                tint = if (theme.secondaryTextColor != Color.Unspecified) 
                    theme.secondaryTextColor 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Reusable Text Input Component
@Composable
private fun FeedbackTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    theme: ApperoTheme,
    maxCharacters: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { 
                Text(
                    text = placeholder, 
                    color = if (theme.secondaryTextColor != Color.Unspecified) 
                        theme.secondaryTextColor 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(FeedbackSpacing.inputHeight)
                .background(
                    color = FeedbackTextStyles.inputBackgroundColor,
                    shape = RoundedCornerShape(FeedbackTextStyles.cornerRadius)
                ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textColor,
                unfocusedTextColor = theme.textColor,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(FeedbackTextStyles.cornerRadius),
            maxLines = 4
        )
        
        // Character counter
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = "${value.length}/$maxCharacters", 
                fontSize = 12.sp, 
                color = if (theme.secondaryTextColor != Color.Unspecified) 
                    theme.secondaryTextColor 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant, 
                modifier = Modifier.padding(top = FeedbackSpacing.tiny)
            )
        }
    }
}

// Reusable Primary Button Component
@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    theme: ApperoTheme,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
    ) { 
        Text(text = text, color = theme.buttonTextColor) 
    }
}

// Reusable Secondary Button Component
@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    theme: ApperoTheme,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) { 
        Text(
            text = text, 
            color = if (theme.textColor != Color.Unspecified) 
                theme.textColor 
            else 
                MaterialTheme.colorScheme.onSurface
        ) 
    }
}

// Rating Step Content
@Composable
private fun RatingStepContent(
    config: FeedbackPromptConfig,
    theme: ApperoTheme,
    imeState: State<Boolean>,
    selectedRating: Int,
    feedbackText: String,
    onRatingSelected: (Int) -> Unit,
    onFeedbackTextChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FeedbackSpacing.large, vertical = FeedbackSpacing.large)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CloseButton(theme = theme, onDismiss = onDismiss)
        
        // Only show title and subtitle when keyboard is not visible
        if (!imeState.value) {
            Spacer(modifier = Modifier.height(FeedbackSpacing.small))
            Text(
                text = config.title, 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold, 
                textAlign = TextAlign.Center, 
                color = FeedbackTextStyles.titleColor, 
                modifier = Modifier.padding(horizontal = FeedbackSpacing.medium),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
            )
            Spacer(modifier = Modifier.height(FeedbackSpacing.small))
            Text(
                text = config.subtitle, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Normal,
                color = FeedbackTextStyles.titleColor, 
                textAlign = TextAlign.Center,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
            )
            Spacer(modifier = Modifier.height(FeedbackSpacing.large))
        }
        
        // Only show emoji rating when keyboard is not visible
        if (!imeState.value) {
            EmojiRatingScale(
                selectedRating = selectedRating,
                theme = theme,
                onRatingSelected = onRatingSelected
            )
            Spacer(modifier = Modifier.height(FeedbackSpacing.large))
        }
        
        // Only show feedback input and CTA after a rating is selected
        if (selectedRating > 0) {
            // Show follow-up question (always visible when input is shown)
            Text(
                text = config.followUpQuestion, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Normal, 
                color = FeedbackTextStyles.titleColor, 
                textAlign = TextAlign.Start, 
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FeedbackSpacing.medium),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
            )
            Spacer(modifier = Modifier.height(FeedbackSpacing.medium))
            
            FeedbackTextInput(
                value = feedbackText,
                onValueChange = onFeedbackTextChanged,
                placeholder = config.placeholder,
                theme = theme,
                maxCharacters = config.maxCharacters
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            PrimaryButton(
                text = config.submitText,
                onClick = onSubmit,
                theme = theme,
                enabled = feedbackText.isNotBlank() && selectedRating > 0
            )
        }
    }
}

// Frustration Step Content
@Composable
private fun FrustrationStepContent(
    config: FeedbackPromptConfig,
    theme: ApperoTheme,
    imeState: State<Boolean>,
    feedbackText: String,
    onFeedbackTextChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FeedbackSpacing.large, vertical = FeedbackSpacing.large)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CloseButton(theme = theme, onDismiss = onDismiss)
        
        // Only show title and subtitle when keyboard is not visible
        if (!imeState.value) {
            Spacer(modifier = Modifier.height(FeedbackSpacing.small))
            Text(
                text = config.title, 
                fontSize = 20.sp, 
                fontWeight = FontWeight.SemiBold, 
                textAlign = TextAlign.Center, 
                color = if (theme.textColor != Color.Unspecified) theme.textColor else MaterialTheme.colorScheme.onSurface, 
                modifier = Modifier.padding(horizontal = FeedbackSpacing.medium)
            )
            Spacer(modifier = Modifier.height(FeedbackSpacing.small))
            Text(
                text = config.subtitle, 
                fontSize = 16.sp, 
                textAlign = TextAlign.Center, 
                color = if (theme.textColor != Color.Unspecified) theme.textColor else MaterialTheme.colorScheme.onSurface, 
                modifier = Modifier.padding(horizontal = FeedbackSpacing.medium)
            )
            Spacer(modifier = Modifier.height(FeedbackSpacing.large))
        }
        
        FeedbackTextInput(
            value = feedbackText,
            onValueChange = onFeedbackTextChanged,
            placeholder = config.placeholder,
            theme = theme,
            maxCharacters = config.maxCharacters
        )
        
        Spacer(modifier = Modifier.height(FeedbackSpacing.large))
        
        PrimaryButton(
            text = config.submitText,
            onClick = onSubmit,
            theme = theme,
            enabled = feedbackText.isNotBlank()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SecondaryButton(
            text = config.secondaryButtonText,
            onClick = onDismiss,
            theme = theme
        )
    }
}

// Thank You Step Content
@Composable
private fun ThankYouStepContent(
    flowConfig: FeedbackFlowConfig,
    theme: ApperoTheme,
    selectedRating: Int,
    reviewPromptThreshold: Int,
    onRequestReview: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FeedbackSpacing.large)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(FeedbackSpacing.large))
        Text(
            text = flowConfig.thankYouTitle, 
            fontSize = 18.sp, 
            fontWeight = FontWeight.Bold, 
            textAlign = TextAlign.Center, 
            color = if (theme.textColor != Color.Unspecified) theme.textColor else MaterialTheme.colorScheme.onSurface, 
            modifier = Modifier.padding(horizontal = FeedbackSpacing.medium)
        )
        Spacer(modifier = Modifier.height(FeedbackSpacing.small))
        Text(
            text = flowConfig.thankYouSubtitle, 
            fontSize = 16.sp, 
            textAlign = TextAlign.Center, 
            color = if (theme.textColor != Color.Unspecified) theme.textColor else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(FeedbackSpacing.xlarge))
        
        PrimaryButton(
            text = flowConfig.thankYouCtaText,
            onClick = { 
                // Trigger Play Store review if eligible before dismissing
                if (selectedRating >= reviewPromptThreshold) {
                    onRequestReview()
                }
                onDismiss() 
            },
            theme = theme
        )
    }
}

@Composable
internal fun EmojiRatingScale(
    selectedRating: Int,
    theme: ApperoTheme,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.SpaceEvenly, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { rating ->
            val isSelected = selectedRating == rating

            Box(
                modifier = Modifier
                    .size(FeedbackSpacing.ratingSize)
                    .clip(RoundedCornerShape(25.dp))
                    .clickable { onRatingSelected(rating) },
                contentAlignment = Alignment.Center
            ) {
                val drawableId = context.resources.getIdentifier(
                    when (rating) {
                        1 -> "ic_rating_very_negative"
                        2 -> "ic_rating_negative"
                        3 -> "ic_rating_neutral"
                        4 -> "ic_rating_positive"
                        5 -> "ic_rating_very_positive"
                        else -> "ic_rating_neutral"
                    },
                    "drawable",
                    context.packageName
                )
                
                // SVG icon with original colors
                Icon(
                    painter = painterResource(id = drawableId),
                    contentDescription = "Rating ${rating}",
                    modifier = Modifier.size(FeedbackSpacing.ratingSize),
                    tint = Color.Unspecified
                )
                
                // Semi-transparent overlay for unselected icons (only when a rating is selected)
                if (selectedRating > 0 && !isSelected) {
                    Box(
                        modifier = Modifier
                            .size(FeedbackSpacing.ratingSize)
                            .background(
                                color = Color.White.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(25.dp)
                            )
                    )
                }
            }
        }
    }
} 