package com.example.appero_sdk_android

import android.content.SharedPreferences
import android.content.Context
import com.example.appero_sdk_android.api.ExperienceSubmissionResult
import com.example.appero_sdk_android.api.FeedbackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    sharedPreferences: SharedPreferences,
    private val context: Context // Pass context from Appero
) {
    private val userSessionManager = UserSessionManager(sharedPreferences)
    private val feedbackRepository = FeedbackRepository()
    private val offlineQueue = OfflineFeedbackQueue(context, sharedPreferences)
    
    /**
     * Log an experience event using predefined Experience enum
     * @param experience The experience level to log
     */
    fun log(experience: Experience) {
        val currentPoints = userSessionManager.getExperiencePoints()
        val newPoints = currentPoints + experience.points
        userSessionManager.setExperiencePoints(newPoints)
        sendExperienceEvent(experience.points)
    }
    
    /**
     * Log experience points using custom scoring
     * @param points The number of points to add (can be negative)
     */
    fun log(points: Int) {
        val currentPoints = userSessionManager.getExperiencePoints()
        val newPoints = currentPoints + points
        userSessionManager.setExperiencePoints(newPoints)
        sendExperienceEvent(points)
    }

    private fun sendExperienceEvent(value: Int) {
        val apiKey = Appero.getApiKey() ?: return
        val clientId = getCurrentUserId()
        val timestamp = getCurrentTimestamp()
        if (offlineQueue.isNetworkAvailable()) {
            CoroutineScope(Dispatchers.IO).launch {
                val result = feedbackRepository.submitExperience(
                    apiKey = apiKey,
                    clientId = clientId,
                    value = value,
                    context = "",
                    sentAt = timestamp
                )
                if (result is ExperienceSubmissionResult.Error) {
                    offlineQueue.queueExperience(apiKey, clientId, value, "")
                }
            }
        } else {
            offlineQueue.queueExperience(apiKey, clientId, value, "")
        }
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return dateFormat.format(java.util.Date())
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