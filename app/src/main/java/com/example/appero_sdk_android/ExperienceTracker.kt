package com.example.appero_sdk_android

import android.content.SharedPreferences

/**
 * Manages user experience tracking and determines when to prompt for feedback
 * Tracks experience points and compares against configurable threshold
 */
internal class ExperienceTracker(private val sharedPreferences: SharedPreferences) {
    
    companion object {
        private const val KEY_EXPERIENCE_POINTS = "experience_points"
        private const val KEY_RATING_THRESHOLD = "rating_threshold"
        private const val KEY_HAS_SUBMITTED_FEEDBACK = "has_submitted_feedback"
        private const val KEY_USER_ID = "user_id"
        
        // Default threshold based on iOS pattern (5 points)
        private const val DEFAULT_RATING_THRESHOLD = 5
    }
    
    /**
     * Current experience points for the user
     */
    var experiencePoints: Int
        get() = sharedPreferences.getInt(KEY_EXPERIENCE_POINTS, 0)
        private set(value) {
            sharedPreferences.edit().putInt(KEY_EXPERIENCE_POINTS, value).apply()
        }
    
    /**
     * Configurable rating threshold - when to prompt for feedback
     */
    var ratingThreshold: Int
        get() = sharedPreferences.getInt(KEY_RATING_THRESHOLD, DEFAULT_RATING_THRESHOLD)
        set(value) {
            require(value > 0) { "Rating threshold must be positive" }
            sharedPreferences.edit().putInt(KEY_RATING_THRESHOLD, value).apply()
        }
    
    /**
     * Whether the current user has already submitted feedback
     */
    private var hasSubmittedFeedback: Boolean
        get() = sharedPreferences.getBoolean(KEY_HAS_SUBMITTED_FEEDBACK, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_HAS_SUBMITTED_FEEDBACK, value).apply()
        }
    
    /**
     * Log an experience event using predefined Experience enum
     * @param experience The experience level to log
     */
    fun log(experience: Experience) {
        log(experience.points)
    }
    
    /**
     * Log experience points using custom scoring
     * @param points The number of points to add (can be negative)
     */
    fun log(points: Int) {
        val newValue = experiencePoints + points
        // Ensure experience points don't go below 0
        experiencePoints = if (newValue < 0) 0 else newValue
    }
    
    /**
     * Check if the feedback prompt should be shown
     * @return true if experience score crosses threshold AND user hasn't submitted feedback
     */
    fun shouldShowAppero(): Boolean {
        return experiencePoints >= ratingThreshold && !hasSubmittedFeedback
    }
    
    /**
     * Mark that the user has submitted feedback
     * This prevents re-prompting the same user
     */
    fun markFeedbackSubmitted() {
        hasSubmittedFeedback = true
    }
    
    /**
     * Reset experience points and feedback status
     * Use carefully - recommend tracking last prompt date
     */
    fun resetExperienceAndPrompt() {
        experiencePoints = 0
        hasSubmittedFeedback = false
    }
    
    /**
     * Reset user data (for logout scenarios)
     * Clears experience points and feedback status
     */
    fun resetUser() {
        experiencePoints = 0
        hasSubmittedFeedback = false
        // Note: User ID management will be handled by UserSessionManager
    }
    
    /**
     * Get current experience tracking state for debugging
     */
    fun getExperienceState(): ExperienceState {
        return ExperienceState(
            experiencePoints = experiencePoints,
            ratingThreshold = ratingThreshold,
            hasSubmittedFeedback = hasSubmittedFeedback,
            shouldShowPrompt = shouldShowAppero()
        )
    }
}

/**
 * Data class representing the current experience tracking state
 */
data class ExperienceState(
    val experiencePoints: Int,
    val ratingThreshold: Int,
    val hasSubmittedFeedback: Boolean,
    val shouldShowPrompt: Boolean
) 