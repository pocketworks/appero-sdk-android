package com.appero.sdk.tracking

import com.appero.sdk.domain.model.Experience
import com.appero.sdk.domain.repository.ExperienceRepository
import com.appero.sdk.domain.repository.UserRepository
import com.appero.sdk.domain.repository.ExperienceSubmissionResult
import com.appero.sdk.data.remote.dto.FeedbackUI
import com.appero.sdk.ui.config.FeedbackPromptConfig
import com.appero.sdk.ui.components.FeedbackStep
import com.appero.sdk.Appero
import com.appero.sdk.debug.ApperoLogger
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

    // Legacy XML support for auto-triggering
    private var legacyActivity: androidx.fragment.app.FragmentActivity? = null

    /**
     * Log an experience event using predefined Experience enum
     * @param experience The experience level to log
     */
    fun log(experience: Experience) {
        val currentPoints = userRepository.getExperiencePoints()
        val newPoints = currentPoints + experience.rating
        userRepository.setExperiencePoints(newPoints)
        
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
        
        submitExperienceToBackend(points, "custom")
    }
    
    /**
     * Trigger feedback prompt with server-provided configuration
     * This method handles auto-triggering for different integration types:
     * - Flutter apps with registered callback will use the callback
     * - Legacy XML apps with registered activity will use XML dialog
     * - Native Compose apps will use Compose dialog
     */
    private fun triggerFeedbackPrompt(
        feedbackUI: FeedbackUI?,
        flowType: String?
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                // Check if we have a registered legacy activity for XML auto-triggering
                val activity = legacyActivity
                if (activity != null && !Appero.hasAutoTriggerCallback()) {
                    // Use legacy XML dialog for auto-triggering only if no Flutter callback is registered
                    val config = FeedbackPromptConfig(
                        title = feedbackUI?.title ?: "How was your experience?",
                        subtitle = feedbackUI?.subtitle ?: "We'd love to hear your thoughts",
                        followUpQuestion = feedbackUI?.prompt ?: "What made your experience positive?",
                        placeholder = "Share your thoughts here",
                        submitText = "Send feedback",
                        secondaryButtonText = "Not now"
                    )
                    val initialStep = when (flowType) {
                        "frustration" -> FeedbackStep.Frustration
                        else -> FeedbackStep.Rating
                    }
                    
                    Appero.showFeedbackDialog(activity, config, initialStep) { success, message ->
                        ApperoLogger.logCriticalOperation("Legacy auto-trigger result", "success=$success, message=$message")
                    }
                } else {
                    // Use the new auto-trigger method that handles Flutter callbacks or Compose UI
                    Appero.triggerAutoFeedbackPrompt(feedbackUI, flowType)
                }
            } catch (e: Exception) {
                android.util.Log.e("ApperoSDK", "Error triggering feedback prompt", e)
            }
        }
    }
    
    /**
     * Submit experience to backend asynchronously; on failure, queue for later
     */
    private fun submitExperienceToBackend(value: Int, context: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val clientId = Appero.getClientId()
                if (clientId != null) {
                    val result = experienceRepository.submitExperience(
                        clientId = clientId,
                        value = value,
                        context = context,
                        sentAt = DateTimeUtils.getCurrentTimestamp(),
                        allowRetry = false
                    )
                    when (result) {
                        is ExperienceSubmissionResult.Success -> {
                            if (result.shouldShowFeedback) {
                                triggerFeedbackPrompt(result.feedbackUI, result.flowType)
                            }
                        }
                        is ExperienceSubmissionResult.Error -> {
                            ApperoLogger.logApiError("/api/experience", "POST", result.message)
                            Appero.queueExperience(value, context)
                        }
                    }
                } else {
                    ApperoLogger.logNetworkError("Experience Submission", "No client ID available")
                    Appero.queueExperience(value, context)
                }
            } catch (e: Exception) {
                ApperoLogger.logNetworkError("Experience Submission", "Exception: ${e.message}")
                Appero.queueExperience(value, context)
            }
        }
    }
    
    fun shouldShowAppero(): Boolean {
        val experiencePoints = userRepository.getExperiencePoints()
        val threshold = userRepository.getRatingThreshold()
        val hasSubmittedFeedback = userRepository.hasSubmittedFeedback()
        
        return experiencePoints >= threshold && !hasSubmittedFeedback
    }
    
    var ratingThreshold: Int
        get() = userRepository.getRatingThreshold()
        set(value) = userRepository.setRatingThreshold(value)
    
    fun markFeedbackSubmitted() { 
        userRepository.markFeedbackSubmitted() 
    }
    
    fun resetExperienceAndPrompt() { 
        userRepository.resetExperienceAndPrompt() 
    }
    
    fun getExperienceState(): ExperienceState {
        return ExperienceState(
            userId = userRepository.getCurrentUserId(),
            experiencePoints = userRepository.getExperiencePoints(),
            ratingThreshold = userRepository.getRatingThreshold(),
            shouldShowPrompt = shouldShowAppero(),
            hasSubmittedFeedback = userRepository.hasSubmittedFeedback()
        )
    }
    
    fun getCurrentUserId(): String { 
        return userRepository.getCurrentUserId() 
    }
    
    fun setUser(userId: String) { 
        userRepository.setUser(userId) 
    }
    
    fun resetUser() { 
        userRepository.resetUser() 
    }
    
    /**
     * Register a FragmentActivity for legacy XML auto-triggering
     * When thresholds are crossed, the feedback dialog will be shown automatically
     * using the registered activity's FragmentManager
     */
    fun registerLegacyActivity(activity: androidx.fragment.app.FragmentActivity) {
        legacyActivity = activity
        ApperoLogger.logCriticalOperation("Legacy activity registered", "auto-triggering: ${activity.javaClass.simpleName}")
    }
    
    /**
     * Unregister the legacy activity (e.g., when activity is destroyed)
     */
    fun unregisterLegacyActivity() {
        legacyActivity?.let { activity ->
            ApperoLogger.logCriticalOperation("Legacy activity unregistered", activity.javaClass.simpleName)
        }
        legacyActivity = null
    }
} 