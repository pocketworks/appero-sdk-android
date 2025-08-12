package com.example.apperoSdkAndroid

import com.example.apperoSdkAndroid.domain.FeedbackRepository
import com.example.apperoSdkAndroid.domain.UserRepository
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
    private val userRepository: UserRepository,
    private val feedbackRepository: FeedbackRepository,
    private val scope: CoroutineScope
) {

    /**
     * Log an experience event using predefined Experience enum
     * @param experience The experience level to log
     */
    fun log(experience: Experience) {
        val currentPoints = userRepository.getExperiencePoints()
        val newPoints = currentPoints + experience.points
        userRepository.setExperiencePoints(newPoints)
        
        // Submit experience to backend
        submitExperienceToBackend(experience.points, experience.name)
    }
    
    /**
     * Log experience points using custom scoring
     * @param points The number of points to add (can be negative)
     */
    fun log(points: Int) {
        val currentPoints = userRepository.getExperiencePoints()
        val newPoints = currentPoints + points
        userRepository.setExperiencePoints(newPoints)
        
        // Submit experience to backend
        submitExperienceToBackend(points, "custom")
    }
    
    /**
     * Submit experience to backend asynchronously
     */
    private fun submitExperienceToBackend(value: Int, context: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val result = feedbackRepository.submitExperience(
                    value = value,
                    context = context,
                    sentAt = com.example.apperoSdkAndroid.utils.DateTimeUtils.getCurrentTimestamp()
                )
                
                when (result) {
                    is com.example.apperoSdkAndroid.domain.ExperienceSubmissionResult.Success -> {
                        // Experience submitted successfully
                    }
                    is com.example.apperoSdkAndroid.domain.ExperienceSubmissionResult.Error -> {
                        // Log error but don't fail the local tracking
                        android.util.Log.w("Appero", "Failed to submit experience: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Appero", "Error submitting experience", e)
            }
        }
    }
    
    /**
     * Check if the feedback prompt should be shown
     * @return true if experience score crosses threshold AND user hasn't submitted feedback
     */
    fun shouldShowAppero(): Boolean {
        val experiencePoints = userRepository.getExperiencePoints()
        val threshold = userRepository.getRatingThreshold()
        val hasSubmittedFeedback = userRepository.hasSubmittedFeedback()
        
        return experiencePoints >= threshold && !hasSubmittedFeedback
    }
    
    /**
     * Get/set the rating threshold for when to prompt for feedback
     */
    var ratingThreshold: Int
        get() = userRepository.getRatingThreshold()
        set(value) = userRepository.setRatingThreshold(value)
    
    /**
     * Mark that the user has submitted feedback
     */
    fun markFeedbackSubmitted() {
        userRepository.markFeedbackSubmitted()
    }
    
    /**
     * Reset experience points and feedback status
     */
    fun resetExperienceAndPrompt() {
        userRepository.resetExperienceAndPrompt()
    }
    
    /**
     * Get current experience tracking state for debugging
     */
    fun getExperienceState(): ExperienceState {
        return ExperienceState(
            userId = userRepository.getCurrentUserId(),
            experiencePoints = userRepository.getExperiencePoints(),
            ratingThreshold = userRepository.getRatingThreshold(),
            shouldShowPrompt = shouldShowAppero(),
            hasSubmittedFeedback = userRepository.hasSubmittedFeedback()
        )
    }
    
    /**
     * Get the current user ID
     */
    fun getCurrentUserId(): String {
        return userRepository.getCurrentUserId()
    }
    
    /**
     * Set a specific user ID (for account-based systems)
     */
    fun setUser(userId: String) {
        userRepository.setUser(userId)
    }
    
    /**
     * Reset the current user (for logout scenarios)
     */
    fun resetUser() {
        userRepository.resetUser()
    }
} 