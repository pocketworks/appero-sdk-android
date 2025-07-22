package com.example.appero_sdk_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    onSubmit: (rating: Int, feedback: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var feedbackText by remember { mutableStateOf("") }
    
    if (visible) {
        // 🔑 Medium article approach: ViewTreeObserver + dynamic height
        val imeState = rememberImeState()
        
        // Apply to main content wrapper (like the Medium article)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            verticalArrangement = Arrangement.Bottom
        ) {
            ModalBottomSheet(
                windowInsets = WindowInsets.ime, // Handle keyboard insets (from article)
                onDismissRequest = onDismiss,
                modifier = modifier.then(
                    // 🔑 Key approach from Medium article: Dynamic height based on keyboard
                    if (imeState.value)
                        Modifier.fillMaxHeight(1.0F) // Full height when keyboard is visible
                    else
                        Modifier.fillMaxHeight(0.73F) // Partial height when keyboard is hidden
                )
            ) {
                val scrollState = rememberScrollState()
                
                // Auto-scroll when keyboard appears
                LaunchedEffect(key1 = imeState.value) {
                    if (imeState.value) {
                        delay(100)
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            
                // Content of the bottom sheet (from Medium article approach)
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
                                tint = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Title
                    Text(
                        text = config.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = if (theme.textColor != Color.Unspecified) theme.textColor else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Subtitle
                    Text(
                        text = config.subtitle,
                        fontSize = 16.sp,
                        color = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Emoji Rating Scale
                    EmojiRatingScale(
                        selectedRating = selectedRating,
                        theme = theme,
                        onRatingSelected = { selectedRating = it }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Follow-up question (only show if rating is selected)
                    if (selectedRating > 0) {
                        Text(
                            text = config.followUpQuestion,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (theme.textColor != Color.Unspecified) theme.textColor else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Feedback text input
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
                                    color = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor else MaterialTheme.colorScheme.onSurfaceVariant
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
                            maxLines = 4 // Limit lines to prevent too much expansion
                        )
                        
                        // Character count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${feedbackText.length}/${config.maxCharacters}",
                                fontSize = 12.sp,
                                color = if (theme.secondaryTextColor != Color.Unspecified) theme.secondaryTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Submit button
                        Button(
                            onClick = { 
                                if (selectedRating > 0) {
                                    onSubmit(selectedRating, feedbackText)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.buttonBackgroundColor
                            ),
                            shape = RoundedCornerShape(24.dp),
                            enabled = selectedRating > 0
                        ) {
                            Text(
                                text = config.submitText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = theme.buttonTextColor
                            )
                        }
                        
                        // Extra bottom space when keyboard is shown
                        Spacer(modifier = Modifier.height(48.dp))
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