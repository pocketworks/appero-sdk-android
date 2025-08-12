package com.example.apperoSdkAndroid.ui

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

/**
 * Configuration for the two-step post-feedback flow (Rate Us + Thank You)
 */
data class FeedbackFlowConfig(
    /**
     * Title for the "Rate Us" screen (step 2 if rating >= threshold)
     * Example: "Enjoying our app?"
     */
    val rateUsTitle: String = "Enjoying our app?",

    /**
     * Subtitle for the "Rate Us" screen
     * Example: "If you like our app, please consider rating us on the Play Store."
     */
    val rateUsSubtitle: String = "If you like our app, please consider rating us on the Play Store.",

    /**
     * CTA text for the "Rate Us" button
     * Example: "Rate on Play Store"
     */
    val rateUsCtaText: String = "Rate on Play Store",

    /**
     * Thank you message shown after feedback or after skipping the review prompt
     * Example: "Thank you for your feedback!"
     */
    val thankYouMessage: String = "Thank you for your feedback!"
) 