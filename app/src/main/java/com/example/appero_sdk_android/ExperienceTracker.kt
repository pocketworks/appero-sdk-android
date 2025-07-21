package com.example.appero_sdk_android

import android.content.SharedPreferences

/**
 * Experience tracking state for debugging and monitoring
 */
data class ExperienceState(
    val userId: String,
    val experiencePoints: Int,
    val ratingThreshold: Int,
    val shouldShowPrompt: Boolean,
    val hasSubmittedFeedback: Boolean
)

/**
 * Handles user experience tracking with persistent storage
 * Uses UserSessionManager for per-user data management
 */
internal class ExperienceTracker(
    sharedPreferences: SharedPreferences
) {
    private val userSessionManager = UserSessionManager(sharedPreferences)
    
    /**
     * Log an experience event using predefined Experience enum
     * @param experience The experience level to log
     */
    fun log(experience: Experience) {
        val currentPoints = userSessionManager.getExperiencePoints()
        val newPoints = currentPoints + experience.points
        userSessionManager.setExperiencePoints(newPoints)
    }
    
    /**
     * Log experience points using custom scoring
     * @param points The number of points to add (can be negative)
     */
    fun log(points: Int) {
        val currentPoints = userSessionManager.getExperiencePoints()
        val newPoints = currentPoints + points
        userSessionManager.setExperiencePoints(newPoints)
    }
    
    /**
     * Check if the feedback prompt should be shown
     * @return true if experience score crosses threshold AND user hasn't submitted feedback
     */
    fun shouldShowAppero(): Boolean {
        val experiencePoints = userSessionManager.getExperiencePoints()
        val threshold = userSessionManager.getRatingThreshold()
        val hasSubmittedFeedback = userSessionManager.hasSubmittedFeedback()
        
        return experiencePoints >= threshold && !hasSubmittedFeedback
    }
    
    /**
     * Get/set the rating threshold for when to prompt for feedback
     */
    var ratingThreshold: Int
        get() = userSessionManager.getRatingThreshold()
        set(value) = userSessionManager.setRatingThreshold(value)
    
    /**
     * Mark that the user has submitted feedback
     */
    fun markFeedbackSubmitted() {
        userSessionManager.markFeedbackSubmitted()
    }
    
    /**
     * Reset experience points and feedback status
     */
    fun resetExperienceAndPrompt() {
        userSessionManager.resetExperienceAndPrompt()
    }
    
    /**
     * Get current experience tracking state for debugging
     */
    fun getExperienceState(): ExperienceState {
        return ExperienceState(
            userId = userSessionManager.getCurrentUserId(),
            experiencePoints = userSessionManager.getExperiencePoints(),
            ratingThreshold = userSessionManager.getRatingThreshold(),
            shouldShowPrompt = shouldShowAppero(),
            hasSubmittedFeedback = userSessionManager.hasSubmittedFeedback()
        )
    }
    
    /**
     * Get the current user ID
     */
    fun getCurrentUserId(): String {
        return userSessionManager.getCurrentUserId()
    }
    
    /**
     * Set a specific user ID (for account-based systems)
     */
    fun setUser(userId: String) {
        userSessionManager.setUser(userId)
    }
    
    /**
     * Reset the current user (for logout scenarios)
     */
    fun resetUser() {
        userSessionManager.resetUser()
    }
} 