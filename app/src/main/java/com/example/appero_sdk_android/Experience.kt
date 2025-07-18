package com.example.appero_sdk_android

/**
 * Experience levels using Likert scale for user experience tracking
 * Each level has an associated point value that contributes to the overall experience score
 */
enum class Experience(val points: Int) {
    /**
     * Very positive experience - adds 2 points
     * Use for: Successful completion of important actions, positive outcomes
     */
    VERY_POSITIVE(2),
    
    /**
     * Positive experience - adds 1 point
     * Use for: Minor successes, smooth interactions
     */
    POSITIVE(1),
    
    /**
     * Neutral experience - adds 0 points
     * Use for: Standard interactions with no particular outcome
     */
    NEUTRAL(0),
    
    /**
     * Negative experience - subtracts 1 point
     * Use for: Minor issues, friction in user flow
     */
    NEGATIVE(-1),
    
    /**
     * Very negative experience - subtracts 2 points
     * Use for: Major issues, failed important actions, errors
     */
    VERY_NEGATIVE(-2)
} 