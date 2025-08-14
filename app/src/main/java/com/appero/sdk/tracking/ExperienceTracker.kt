package com.appero.sdk.tracking

import com.appero.sdk.domain.model.Experience
import com.appero.sdk.domain.repository.ExperienceRepository
import com.appero.sdk.domain.repository.UserRepository
import com.appero.sdk.domain.repository.ExperienceSubmissionResult
import com.appero.sdk.data.remote.dto.FeedbackUI
import com.appero.sdk.ui.config.FeedbackPromptConfig
import com.appero.sdk.ui.components.FeedbackStep
import com.appero.sdk.Appero
import com.appero.sdk.util.DateTimeUtils
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
    private val experienceRepository: ExperienceRepository,
    private val scope: CoroutineScope
) {

    /**
     * Log an experience event using predefined Experience enum
     * @param experience The experience level to log
     */
    fun log(experience: Experience) {
        val currentPoints = userRepository.getExperiencePoints()
        val newPoints = currentPoints + experience.rating
        userRepository.setExperiencePoints(newPoints)
        
        // Submit experience to backend
        submitExperienceToBackend(experience.rating, experience.name)
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
     * Trigger feedback prompt with server-provided configuration
     */
    private fun triggerFeedbackPrompt(
        feedbackUI: FeedbackUI?,
        flowType: String?
    ) {
        // Switch to main thread to show UI
        scope.launch(Dispatchers.Main) {
            try {
                // Create feedback prompt configuration from server response
                val config = FeedbackPromptConfig(
                    title = feedbackUI?.title ?: "How was your experience?",
                    subtitle = feedbackUI?.subtitle ?: "We'd love to hear your thoughts",
                    followUpQuestion = feedbackUI?.prompt ?: "What made your experience positive?",
                    placeholder = "Share your thoughts here",
                    submitText = "Send feedback"
                )
                
                // Determine initial step based on flow type
                val initialStep = when (flowType) {
                    "frustration" -> FeedbackStep.Frustration
                    else -> FeedbackStep.Rating
                }
                
                // Show feedback prompt using Appero's UI system with the correct initial step
                Appero.showFeedbackPrompt(config, initialStep)
                
            } catch (e: Exception) {
                android.util.Log.e("Appero", "Error triggering feedback prompt", e)
            }
        }
    }
    
    /**
     * Submit experience to backend asynchronously
     */
    private fun submitExperienceToBackend(value: Int, context: String) {
        scope.launch(Dispatchers.IO) {
            try {
                // Get client ID from Appero singleton
                val clientId = Appero.getClientId()
                
                if (clientId != null) {
                    val result = experienceRepository.submitExperience(
                        clientId = clientId,
                        value = value,
                        context = context,
                        sentAt = DateTimeUtils.getCurrentTimestamp()
                    )
                    
                    when (result) {
                        is ExperienceSubmissionResult.Success -> {
                            // Experience submitted successfully
                            
                            // Check if we should show feedback prompt
                            if (result.shouldShowFeedback) {
                                // Trigger feedback prompt with server-provided UI configuration
                                triggerFeedbackPrompt(result.feedbackUI, result.flowType)
                            }
                        }
                        is ExperienceSubmissionResult.Error -> {
                            // Log error but don't fail the local tracking
                            android.util.Log.w("Appero", "Failed to submit experience: ${result.message}")
                        }
                    }
                } else {
                    android.util.Log.w("Appero", "Cannot submit experience: client ID not available")
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