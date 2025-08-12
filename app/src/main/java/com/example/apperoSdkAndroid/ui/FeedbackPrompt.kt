package com.example.apperoSdkAndroid.ui

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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.apperoSdkAndroid.ApperoAnalyticsListener
import kotlinx.coroutines.delay

/**
 * Enhanced keyboard visibility detection using ViewTreeObserver approach from Medium article
 * https://medium.com/@zekromvishwa56789/handling-keyboard-overlap-in-modalbottomsheet-with-jetpack-compose-a-practical-approach-e68db28ff66e
 */
@Composable
fun rememberImeState(): State<Boolean> {
    val view = LocalView.current
    var isImeVisible by remember { mutableStateOf(false) }

    // Observe keyboard visibility using ViewTreeObserver (from Medium article)
    DisposableEffect(Unit) {
        val listener = android.view.ViewTreeObserver.OnPreDrawListener {
            // Check if the IME (keyboard) is visible using the exact approach from the article
            isImeVisible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            true
        }
        view.viewTreeObserver.addOnPreDrawListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }

    return remember { derivedStateOf { isImeVisible } }
}

// Add sealed class for feedback steps
sealed class FeedbackStep {
    object Rating : FeedbackStep()
    object Frustration : FeedbackStep()  // New step for frustration flow
    object RateUs : FeedbackStep()
    object ThankYou : FeedbackStep()
}

/**
 * Feedback prompt composable that displays a modal bottom sheet with emoji rating
 * and text feedback input, matching the Figma design.
 */
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

    // Update current step when initialStep changes
    LaunchedEffect(initialStep) {
        if (initialStep != null) {
            currentStep = initialStep
        }
    }

    if (visible) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            verticalArrangement = Arrangement.Bottom
        ) {
            @Suppress("detekt:MagicNumber")
            ModalBottomSheet(
                windowInsets = WindowInsets.ime,
                onDismissRequest = onDismiss,
                modifier = modifier.then(
                    if (imeState.value)
                        Modifier.fillMaxHeight(1.0F)
                    else
                        Modifier.fillMaxHeight(0.73F)
                )
            ) {
                // Auto-scroll when keyboard appears (for Rating step)
                LaunchedEffect(key1 = imeState.value) {
                    if (imeState.value && currentStep == FeedbackStep.Rating) {
                        @Suppress("detekt:MagicNumber")
                        delay(100)
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }

                when (currentStep) {
                    is FeedbackStep.Rating -> {
                        // --- Step 1: Feedback Rating UI (existing) ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Close button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(24.dp)
                                ) {
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = config.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = if (theme.textColor != Color.Unspecified) theme.textColor
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = config.subtitle,
                                fontSize = 16.sp,
                                color = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            EmojiRatingScale(
                                selectedRating = selectedRating,
                                theme = theme,
                                onRatingSelected = { rating ->
                                    selectedRating = rating
                                    analyticsListener?.onRatingSelected(rating)
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            if (selectedRating > 0) {
                                Text(
                                    text = config.followUpQuestion,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (theme.textColor != Color.Unspecified) theme.textColor
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = feedbackText,
                                    onValueChange = {
                                        if (it.length <= config.maxCharacters) {
                                            feedbackText = it
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            text = config.placeholder,
                                            color = if (theme.secondaryTextColor != Color.Unspecified)
                                                theme.secondaryTextColor
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = theme.textColor,
                                        unfocusedTextColor = theme.textColor,
                                        focusedBorderColor = theme.accentColor,
                                        unfocusedBorderColor = theme.dividerColor
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    maxLines = 4
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "${feedbackText.length}/${config.maxCharacters}",
                                        fontSize = 12.sp,
                                        color = if (theme.secondaryTextColor != Color.Unspecified)
                                            theme.secondaryTextColor
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        onSubmit(selectedRating, feedbackText)
                                        // Transition to RateUs or ThankYou based on rating
                                        if (selectedRating >= reviewPromptThreshold) {
                                            currentStep = FeedbackStep.RateUs
                                        } else {
                                            currentStep = FeedbackStep.ThankYou
                                        }
                                    },
                                    enabled = feedbackText.isNotBlank() && selectedRating > 0,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                                ) {
                                    Text(
                                        text = config.submitText,
                                        color = theme.buttonTextColor
                                    )
                                }
                            }
                        }
                    }

                    is FeedbackStep.Frustration -> {
                        // --- Step 1b: Frustration Flow UI (feedback only, no rating) ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Close button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(24.dp)
                                ) {
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = config.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = if (theme.textColor != Color.Unspecified) theme.textColor
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = config.subtitle,
                                fontSize = 16.sp,
                                color = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = feedbackText,
                                onValueChange = {
                                    if (it.length <= config.maxCharacters) {
                                        feedbackText = it
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = config.placeholder,
                                        color = if (theme.secondaryTextColor != Color.Unspecified)
                                            theme.secondaryTextColor
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = theme.textColor,
                                    unfocusedTextColor = theme.textColor,
                                    focusedBorderColor = theme.accentColor,
                                    unfocusedBorderColor = theme.dividerColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 4
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "${feedbackText.length}/${config.maxCharacters}",
                                    fontSize = 12.sp,
                                    color = if (theme.secondaryTextColor != Color.Unspecified)
                                        theme.secondaryTextColor
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            // Primary CTA - Send Feedback
                            Button(
                                onClick = {
                                    onSubmit(0, feedbackText) // No rating for frustration flow
                                    currentStep = FeedbackStep.ThankYou
                                },
                                enabled = feedbackText.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                            ) {
                                Text(
                                    text = config.submitText,
                                    color = theme.buttonTextColor
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // Secondary CTA - Not Now
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Text(
                                    text = "Not now",
                                    color = if (theme.textColor != Color.Unspecified) theme.textColor
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    is FeedbackStep.RateUs -> {
                        // --- Step 2a: Rate Us Screen ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Close button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        currentStep = FeedbackStep.ThankYou
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = flowConfig.rateUsTitle,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = if (theme.textColor != Color.Unspecified) theme.textColor
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = flowConfig.rateUsSubtitle,
                                fontSize = 16.sp,
                                color = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    onRequestReview()
                                    currentStep = FeedbackStep.ThankYou
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                            ) {
                                Text(
                                    text = flowConfig.rateUsCtaText,
                                    color = theme.buttonTextColor
                                )
                            }
                        }
                    }

                    is FeedbackStep.ThankYou -> {
                        // --- Step 2b: Thank You Screen ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = flowConfig.thankYouMessage,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = if (theme.textColor != Color.Unspecified) theme.textColor
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                            ) {
                                Text(
                                    text = "Close",
                                    color = theme.buttonTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Emoji rating scale component with 5 emoji faces
 */
@Composable
internal fun EmojiRatingScale(
    selectedRating: Int,
    theme: ApperoTheme,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val emojis = listOf("😢", "😕", "😐", "😊", "😍")
    val colors = listOf(
        theme.veryNegativeColor,  // Red for very sad 😢
        theme.negativeColor,      // Orange for sad 😕
        theme.neutralColor,       // Yellow for neutral 😐
        theme.positiveColor,      // Blue for happy 😊
        theme.veryPositiveColor   // Green for very happy 😍
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        emojis.forEachIndexed { index, emoji ->
            val rating = index + 1
            val isSelected = selectedRating == rating
            val backgroundColor = if (isSelected) colors[index] else Color.LightGray.copy(alpha = 0.3f)

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(backgroundColor)
                    .clickable { onRatingSelected(rating) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 24.sp
                )
            }
        }
    }
} 