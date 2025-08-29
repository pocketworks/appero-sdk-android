package com.appero.sdk.ui.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration for the feedback prompt UI
 * All text content must be provided by the developer using the SDK
 */
@Parcelize
data class FeedbackPromptConfig(
    /**
     * Main title shown at the top of the prompt
     * Example: "We're happy to see that you're using our app 🎉"
     */
    val title: String,
    
    /**
     * Subtitle shown below the title
     * Example: "Let us know how we're doing"
     */
    val subtitle: String,
    
    /**
     * Question shown after user selects a rating
     * Example: "What made your experience positive?"
     */
    val followUpQuestion: String,
    
    /**
     * Placeholder text for the feedback input field
     * Example: "Share your thoughts here"
     */
    val placeholder: String,
    
    /**
     * Text shown on the submit button
     * Example: "Send feedback"
     */
    val submitText: String,
    
    /**
     * Maximum number of characters allowed in the feedback text
     * Default: 120
     */
    val maxCharacters: Int = 120,
    
    /**
     * Text shown on the secondary button (e.g., "Not now" button in frustration flow)
     * Default: "Not now"
     */
    val secondaryButtonText: String = "Not now"
) : Parcelable

/**
 * Configuration for the post-feedback flow (Thank You message)
 */
data class FeedbackFlowConfig(
    /**
     * Thank you title shown after feedback submission
     * Example: "Thank you for your feedback!"
     */
    val thankYouTitle: String = "Thank you for your feedback!",

    /**
     * Thank you subtitle shown after feedback submission
     * Example: "We appreciate your input"
     */
    val thankYouSubtitle: String = "We appreciate your input",

    /**
     * CTA text for the thank you close button
     * Example: "Close"
     */
    val thankYouCtaText: String = "Close"
) 