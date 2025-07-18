package com.example.appero_sdk_android.ui

/**
 * Configuration for the feedback prompt UI
 * All text content must be provided by the developer using the SDK
 */
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
    val maxCharacters: Int = 120
) 