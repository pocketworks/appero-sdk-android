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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
    val scrollState = rememberScrollState()

    LaunchedEffect(initialStep) { if (initialStep != null) currentStep = initialStep }

    if (visible) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            verticalArrangement = Arrangement.Bottom
        ) {
            ModalBottomSheet(
                windowInsets = WindowInsets.ime,
                onDismissRequest = onDismiss,
                dragHandle = null,
                modifier = modifier.then(
                    if (selectedRating > 0) {
                        if (imeState.value) Modifier.fillMaxHeight(1.0F) else Modifier.fillMaxHeight(0.85F)
                    } else {
                        Modifier
                    }
                ),
                containerColor = Color.White
            ) {
                when (currentStep) {
                    is FeedbackStep.Rating -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            
                            // Only show title and subtitle when keyboard is not visible
                            if (!imeState.value) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = config.title, 
                                    fontSize = 18.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    textAlign = TextAlign.Center, 
                                    color = Color(0xFF003143), 
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = config.subtitle, 
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF003143), 
                                    textAlign = TextAlign.Center,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            
                            // Only show emoji rating when keyboard is not visible
                            if (!imeState.value) {
                                EmojiRatingScale(
                                    selectedRating = selectedRating,
                                    theme = theme,
                                    onRatingSelected = { rating ->
                                        selectedRating = rating
                                        analyticsListener?.onRatingSelected(rating)
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            
                            // Only show feedback input and CTA after a rating is selected
                            if (selectedRating > 0) {
                                // Show follow-up question (always visible when input is shown)
                                Text(
                                    text = config.followUpQuestion, 
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Normal, 
                                    color = Color(0xFF003143), 
                                    textAlign = TextAlign.Start, 
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                OutlinedTextField(
                                    value = feedbackText,
                                    onValueChange = { if (it.length <= config.maxCharacters) feedbackText = it },
                                    placeholder = { Text(text = config.placeholder, color = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor else MaterialTheme.colorScheme.onSurfaceVariant) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(88.dp)
                                        .background(
                                            color = Color(0xFFF5F5F5),
                                            shape = RoundedCornerShape(8.dp)
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
                                    shape = RoundedCornerShape(8.dp),
                                    maxLines = 4
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text(text = "${feedbackText.length}/${config.maxCharacters}", fontSize = 12.sp, color = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = {
                                        onSubmit(selectedRating, feedbackText)
                                        currentStep = FeedbackStep.ThankYou
                                    },
                                    enabled = feedbackText.isNotBlank() && selectedRating > 0,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                                ) { Text(text = config.submitText, color = theme.buttonTextColor) }
                            }
                        }
                    }
                    is FeedbackStep.Frustration -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 24.dp, vertical = 24.dp)
                                .padding(bottom = if (imeState.value) 16.dp else 0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            
                            // Only show title when keyboard is not visible (hide subtitle for frustration flow)
                            if (!imeState.value) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = config.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = if (theme.textColor != Color.Unspecified) theme.textColor else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 16.dp))
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            
                            OutlinedTextField(
                                value = feedbackText,
                                onValueChange = { if (it.length <= config.maxCharacters) feedbackText = it },
                                placeholder = { Text(text = "Would you mind telling us what went wrong?", color = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor else MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(88.dp)
                                    .background(
                                        color = Color(0xFFF5F5F5),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = theme.textColor,
                                    unfocusedTextColor = theme.textColor,
                                    focusedBorderColor = theme.accentColor,
                                    unfocusedBorderColor = theme.dividerColor,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 4
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text(text = "${feedbackText.length}/${config.maxCharacters}", fontSize = 12.sp, color = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { onSubmit(0, feedbackText); currentStep = FeedbackStep.ThankYou }, enabled = feedbackText.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)) { Text(text = config.submitText, color = theme.buttonTextColor) }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) { Text(text = "Not now", color = if (theme.textColor != Color.Unspecified) theme.textColor else MaterialTheme.colorScheme.onSurface) }
                        }
                    }

                    is FeedbackStep.ThankYou -> {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = flowConfig.thankYouTitle, 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold, 
                                textAlign = TextAlign.Center, 
                                color = if (theme.textColor != Color.Unspecified) theme.textColor else MaterialTheme.colorScheme.onSurface, 
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = flowConfig.thankYouSubtitle, 
                                fontSize = 16.sp, 
                                textAlign = TextAlign.Center, 
                                color = if (theme.textColor != Color.Unspecified) theme.textColor else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { 
                                    // Trigger Play Store review if eligible before dismissing
                                    if (selectedRating >= reviewPromptThreshold) {
                                        onRequestReview()
                                    }
                                    onDismiss() 
                                }, 
                                modifier = Modifier.fillMaxWidth(), 
                                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                            ) { Text(text = flowConfig.thankYouCtaText, color = theme.buttonTextColor) }
                        }
                    }
                }
            }
        }
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

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { rating ->
            val isSelected = selectedRating == rating

            Box(
                modifier = Modifier
                    .size(50.dp)
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
                    modifier = Modifier.size(50.dp),
                    tint = Color.Unspecified
                )
                
                // Semi-transparent overlay for unselected icons (only when a rating is selected)
                if (selectedRating > 0 && !isSelected) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
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