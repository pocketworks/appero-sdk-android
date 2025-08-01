package com.example.apperoSdkAndroid

/**
 * Experience levels using Likert scale for user experience tracking
 * Each level has an associated point value that contributes to the overall experience score
 */
@Suppress("detekt:MagicNumber")
enum class Experience(val points: Int) {
    /**
     * Very positive experience - adds 5 points
     * Use for: Successful completion of important actions, positive outcomes
     */
    VERY_POSITIVE(5),
    
    /**
     * Positive experience - adds 4 points
     * Use for: Minor successes, smooth interactions
     */
    POSITIVE(1),
    
    /**
     * Neutral experience - adds 3 points
     * Use for: Standard interactions with no particular outcome
     */
    NEUTRAL(3),
    
    /**
     * Negative experience - adds 2 point
     * Use for: Minor issues, friction in user flow
     */
    NEGATIVE(2),
    
    /**
     * Very negative experience - adds 1 point
     * Use for: Major issues, failed important actions, errors
     */
    VERY_NEGATIVE(1)
} 