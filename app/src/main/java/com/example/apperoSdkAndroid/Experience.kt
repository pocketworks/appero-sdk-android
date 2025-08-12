package com.example.apperoSdkAndroid

/**
 * Experience levels using 5-point Likert scale for user experience tracking
 * Each level represents user satisfaction and is sent directly to the API
 */
@Suppress("detekt:MagicNumber")
enum class Experience(val rating: Int) {
    /**
     * Very Negative (1) - e.g., "500 error", critical bugs, system failures
     */
    VERY_NEGATIVE(1),
    
    /**
     * Negative (2) - e.g., slow performance, confusing UI
     */
    NEGATIVE(2),
    
    /**
     * Neutral (3) - e.g., average experience, neither good nor bad
     */
    NEUTRAL(3),
    
    /**
     * Positive (4) - e.g., smooth flow, good performance
     */
    POSITIVE(4),
    
    /**
     * Very Positive (5) - e.g., "user started subscription", excellent experience
     */
    VERY_POSITIVE(5)
} 