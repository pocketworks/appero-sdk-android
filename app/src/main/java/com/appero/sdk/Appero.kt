package com.appero.sdk

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.appero.sdk.analytics.ApperoAnalyticsListener
import com.appero.sdk.data.local.queue.OfflineFeedbackQueue
import com.appero.sdk.data.local.queue.OfflineExperienceQueue
import com.appero.sdk.data.remote.ApperoApiService
import com.appero.sdk.debug.ApperoDebugMode
import com.appero.sdk.debug.ApperoLogger
import com.appero.sdk.domain.model.Experience
import com.appero.sdk.domain.repository.ClientRepository
import com.appero.sdk.domain.repository.ExperienceRepository
import com.appero.sdk.domain.repository.FeedbackRepository
import com.appero.sdk.domain.repository.FeedbackSubmissionResult
import com.appero.sdk.domain.repository.UserRepository
import com.appero.sdk.domain.repository.UserRepository.Companion.DEFAULT_RATING_THRESHOLD
import com.appero.sdk.tracking.ExperienceState
import com.appero.sdk.tracking.ExperienceTracker
import com.appero.sdk.ui.components.FeedbackPrompt
import com.appero.sdk.ui.components.FeedbackStep
import com.appero.sdk.ui.config.FeedbackFlowConfig
import com.appero.sdk.ui.config.FeedbackPromptConfig
import com.appero.sdk.ui.theme.ApperoTheme
import com.appero.sdk.ui.theme.DefaultTheme
import com.appero.sdk.util.PlayStoreReviewManager
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Appero SDK class - singleton instance for global access
 * Provides intelligent in-app feedback collection and user experience tracking
 */
object Appero {

    private const val PREFS_NAME = "appero_sdk_prefs"

    /**
     * Result of a Play Store review request
     */
    sealed class PlayStoreReviewResult {
        object InAppReviewShown : PlayStoreReviewResult()
        object InAppReviewCompleted : PlayStoreReviewResult()
        object FallbackTriggered : PlayStoreReviewResult()
        data class Failed(val reason: String) : PlayStoreReviewResult()
    }

    private var clientRepository: ClientRepository? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var experienceTracker: ExperienceTracker? = null
    private var feedbackRepository: FeedbackRepository? = null
    private var experienceRepository: ExperienceRepository? = null
    private var offlineFeedbackQueue: OfflineFeedbackQueue? = null
    private var offlineExperienceQueue: OfflineExperienceQueue? = null

    private var apiKey: String? = null
    private var clientId: String? = null
    private var isInitialized: Boolean = false
    private var appContext: Context? = null

    // Network monitoring
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Theming system
    var theme: ApperoTheme = DefaultTheme()

    // Analytics integration
    private var analyticsListener: ApperoAnalyticsListener? = null

    // Play Store review manager
    private var playStoreReviewManager: PlayStoreReviewManager? = null

    /**
     * Set the analytics listener for Appero events
     * @param listener Your implementation of ApperoAnalyticsListener (or null to remove)
     */
    fun setAnalyticsListener(listener: ApperoAnalyticsListener?) {
        analyticsListener = listener
        // Recreate Play Store review manager with new listener
        playStoreReviewManager = PlayStoreReviewManager(analyticsListener)
    }

    // UI state for feedback prompt
    private var _showFeedbackPrompt: MutableState<Boolean> = mutableStateOf(false)
    private var _feedbackPromptConfig: MutableState<FeedbackPromptConfig?> = mutableStateOf(null)
    private var _initialFeedbackStep: MutableState<FeedbackStep?> = mutableStateOf(null)

    // Callback for feedback submission results
    private var onFeedbackSubmissionResult: ((Boolean, String) -> Unit)? = null

    /**
     * Initialize the Appero SDK with API key and client ID
     * Should be called in Application.onCreate() or MainActivity.onCreate()
     *
     * @param context Application or Activity context
     * @param apiKey Your Appero API key (UUID format)
     * @param clientId Your Appero client ID (UUID format)
     */
    fun start(context: Context, apiKey: String, clientId: String?) {
        start(context, apiKey, clientId, ApperoDebugMode.PRODUCTION)
    }

    /**
     * Initialize the Appero SDK with API key, client ID, and debug mode
     * Should be called in Application.onCreate() or MainActivity.onCreate()
     *
     * @param context Application or Activity context
     * @param apiKey Your Appero API key (UUID format)
     * @param clientId Your Appero client ID (UUID format)
     * @param debugMode The debug mode for the SDK (defaults to PRODUCTION)
     */
    fun start(context: Context, apiKey: String, clientId: String?, debugMode: ApperoDebugMode) {
        // Store context for string resources
        appContext = context.applicationContext
        
        // Set debug mode first so we can log initialization
        ApperoLogger.setDebugMode(debugMode)
        ApperoLogger.logCriticalOperation("SDK Initialization", "Starting with debug mode: $debugMode")
        
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Teardown any previous setup to avoid duplicate callbacks/queues
        try {
            networkCallback?.let { callback ->
                (connectivityManager ?: context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                    ?.unregisterNetworkCallback(callback)
            }
        } catch (_: Exception) { }
        networkCallback = null
        offlineFeedbackQueue?.cleanup()
        offlineExperienceQueue?.cleanup()
        offlineFeedbackQueue = null
        offlineExperienceQueue = null

        initializeClient(apiKey, clientId, sharedPreferences)
        
        // Create API service with authentication credentials from the initialized client
        val apiService = ApperoApiService.create(
            apiKey = getApiKey()
        )
        
        feedbackRepository = FeedbackRepository(sharedPreferences, apiService, context).also {
            offlineFeedbackQueue = OfflineFeedbackQueue(it, scope)
        }
        
        experienceRepository = ExperienceRepository(sharedPreferences, apiService, context)
        
        offlineExperienceQueue = experienceRepository?.let { OfflineExperienceQueue(it, scope) }
        
        experienceTracker = ExperienceTracker(
            UserRepository(sharedPreferences),
            experienceRepository!!,
            scope
        )

        setupNetworkMonitoring(context)

        // Initialize Play Store review manager
        playStoreReviewManager = PlayStoreReviewManager(analyticsListener)

        isInitialized = true
        ApperoLogger.logCriticalOperation("SDK Initialization", "Completed successfully")
    }

    private fun initializeClient(
        apiKey: String, clientId: String?,
        preferences: SharedPreferences
    ) {
        clientRepository = ClientRepository(preferences)

        // Auto-generate clientId if blank or null
        var resolvedClientId = clientId
        if (resolvedClientId.isNullOrBlank()) {
            // Try to load from prefs first
            resolvedClientId = clientRepository?.getClientId()
            if (resolvedClientId.isNullOrBlank()) {
                resolvedClientId = java.util.UUID.randomUUID().toString()
            }
        }

        clientRepository?.putApiKey(apiKey)
        clientRepository?.putClientId(resolvedClientId)
        clientRepository?.putIsApperoInitialized(true)
    }

    /**
     * Check if the SDK has been initialized
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }

    /**
     * Log an experience event using predefined Experience enum
     * @param experience The experience level to log
     */
    fun log(experience: Experience) {
        requireInitialized()
        experienceTracker?.log(experience)
        

    }

    /**
     * Log experience points using custom scoring
     * @param points The number of points to add (can be negative)
     */
    fun log(points: Int) {
        requireInitialized()
        experienceTracker?.log(points)
        

    }

    /**
     * Check if the feedback prompt should be shown
     * @return true if experience score crosses threshold AND user hasn't submitted feedback
     */
    fun shouldShowAppero(): Boolean {
        requireInitialized()
        return experienceTracker?.shouldShowAppero() ?: false
    }

    /**
     * Expose experience queue utilities (for debugging/testing)
     */
    fun getQueuedExperiencesCount(): Int {
        requireInitialized()
        return offlineExperienceQueue?.getQueueSize() ?: 0
    }

    fun processQueuedExperiences() {
        requireInitialized()
        offlineExperienceQueue?.processQueue()
    }

    fun clearQueuedExperiences() {
        requireInitialized()
        offlineExperienceQueue?.clearQueue()
    }

    internal fun queueExperience(value: Int, context: String) {
        offlineExperienceQueue?.queueExperience(value, context)
    }

    /**
     * Set a specific user ID (for account-based systems)
     * This will switch the active user and load their experience data
     *
     * @param userId The user ID to set as active
     */
    fun setUser(userId: String) {
        requireInitialized()
        experienceTracker?.setUser(userId)
    }

    /**
     * Reset the current user (for logout scenarios)
     * Clears user data and generates a new anonymous user ID
     */
    fun resetUser() {
        requireInitialized()
        experienceTracker?.resetUser()
    }

    /**
     * Get the current user ID
     * @return Current user ID (auto-generated UUID if none was set)
     */
    fun getCurrentUserId(): String? {
        requireInitialized()
        return experienceTracker?.getCurrentUserId()
    }

    /**
     * Show the feedback prompt UI
     * This will display the modal bottom sheet with emoji rating and text input
     *
     * @param config Configuration object containing all text content for the prompt
     * @param onResult Optional callback to receive feedback submission results
     */
    fun showFeedbackPrompt(
        config: FeedbackPromptConfig,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) {
        requireInitialized()
        _feedbackPromptConfig.value = config
        _initialFeedbackStep.value = null
        _showFeedbackPrompt.value = true
        onFeedbackSubmissionResult = onResult
    }

    /**
     * Show the feedback prompt UI with a specific initial step
     * This will display the modal bottom sheet starting from the specified step
     *
     * @param config Configuration object containing all text content for the prompt
     * @param initialStep The initial step to show (e.g., FeedbackStep.Frustration for frustration flow)
     * @param onResult Optional callback to receive feedback submission results
     */
    fun showFeedbackPrompt(
        config: FeedbackPromptConfig,
        initialStep: FeedbackStep,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) {
        requireInitialized()
        _feedbackPromptConfig.value = config
        _initialFeedbackStep.value = initialStep
        _showFeedbackPrompt.value = true
        onFeedbackSubmissionResult = onResult
    }

    /**
     * Show the feedback prompt UI (Compose)
     * Add this to your Compose UI hierarchy
     * @param config Configuration object for the feedback prompt
     * @param flowConfig Configuration for the two-step post-feedback flow (Rate Us + Thank You)
     * @param reviewPromptThreshold Minimum rating to show the Rate Us step (default 4)
     * @param onRequestReview Callback to trigger Play Store review prompt
     * @param onResult Optional callback to receive feedback submission results
     */
    @Composable
    fun FeedbackPromptUI(
        config: FeedbackPromptConfig,
        flowConfig: FeedbackFlowConfig = FeedbackFlowConfig(),
        reviewPromptThreshold: Int = 4,
        onRequestReview: () -> Unit = {},
        onResult: ((success: Boolean, message: String) -> Unit)? = null,
        activity: Activity? = null
    ) {
        requireInitialized()
        val currentConfig = _feedbackPromptConfig.value ?: config
        val initialStep = _initialFeedbackStep.value
        var serverResponseMessage by mutableStateOf<String?>(null)
        
        FeedbackPrompt(
            visible = _showFeedbackPrompt.value,
            config = currentConfig,
            theme = theme,
            analyticsListener = analyticsListener,
            onSubmit = { rating, feedback, onSuccess ->
                handleFeedbackSubmission(rating, feedback, { success, message ->
                    if (success) {
                        serverResponseMessage = message
                        onSuccess(message) // Call the callback to trigger thank you step
                        onResult?.invoke(success, message)
                    } else {
                        onResult?.invoke(success, message)
                    }
                }, activity)
                // Keep the bottom sheet open; content will change via FeedbackPrompt's state
            },
            onDismiss = {
                _showFeedbackPrompt.value = false
                _feedbackPromptConfig.value = null
                _initialFeedbackStep.value = null
                onFeedbackSubmissionResult = null
                serverResponseMessage = null
            },
            flowConfig = flowConfig,
            reviewPromptThreshold = reviewPromptThreshold,
            onRequestReview = onRequestReview,
            initialStep = initialStep,
            serverResponseMessage = serverResponseMessage,
            onSubmissionResult = onResult,
            onShowThankYou = { message ->
                android.util.Log.d("ApperoSDK", "Thank you callback triggered with message: $message")
            }
        )
    }

    /**
     * Request Play Store review with comprehensive fallback handling
     * 
     * This method follows Google's best practices:
     * 1. Attempts Google Play In-App Review API first
     * 2. Falls back to external Play Store if in-app review fails
     * 3. Provides analytics callbacks for all scenarios
     * 
     * @param activity The current Activity context
     * @param fallbackToExternalStore Whether to fallback to external Play Store (default: true)
     * @param onComplete Optional callback with the result of the review request
     */
    fun requestPlayStoreReview(
        activity: Activity, 
        fallbackToExternalStore: Boolean = true,
        onComplete: ((PlayStoreReviewResult) -> Unit)? = null
    ) {
        requireInitialized()
        
        val config = PlayStoreReviewManager.ReviewConfig(
            fallbackToExternalStore = fallbackToExternalStore,
            enableAnalytics = analyticsListener != null
        )
        
        playStoreReviewManager?.requestReview(activity, config) { result ->
            // Convert internal result to public result
            val publicResult = when (result) {
                is PlayStoreReviewManager.ReviewResult.InAppReviewShown -> PlayStoreReviewResult.InAppReviewShown
                is PlayStoreReviewManager.ReviewResult.InAppReviewCompleted -> PlayStoreReviewResult.InAppReviewCompleted
                is PlayStoreReviewManager.ReviewResult.FallbackTriggered -> PlayStoreReviewResult.FallbackTriggered
                is PlayStoreReviewManager.ReviewResult.Failed -> PlayStoreReviewResult.Failed(result.reason)
            }
            onComplete?.invoke(publicResult)
        } ?: run {
            ApperoLogger.logNetworkError("Play Store Review", "Review manager not initialized")
            onComplete?.invoke(PlayStoreReviewResult.Failed("Review manager not initialized"))
        }
    }

    /**
     * Request Play Store review based on rating threshold
     * Only shows review prompt if rating meets or exceeds the threshold
     * 
     * @param activity The current Activity context
     * @param rating The user's rating (1-5)
     * @param threshold The minimum rating to show the review prompt (default: 4)
     * @param fallbackToExternalStore Whether to fallback to external Play Store (default: true)
     * @param onComplete Optional callback with the result of the review request
     */
    fun requestPlayStoreReviewIfRating(
        activity: Activity,
        rating: Int,
        threshold: Int = 4,
        fallbackToExternalStore: Boolean = true,
        onComplete: ((PlayStoreReviewResult?) -> Unit)? = null
    ) {
        requireInitialized()
        
        if (rating >= threshold) {
            ApperoLogger.logCriticalOperation("Play Store Review", "Rating $rating meets threshold $threshold, requesting review")
            requestPlayStoreReview(activity, fallbackToExternalStore) { result ->
                onComplete?.invoke(result)
            }
        } else {
            ApperoLogger.debug("Play Store Review skipped - rating $rating below threshold $threshold")
            onComplete?.invoke(null) // null indicates review was not requested
        }
    }

    /**
     * Check if Google Play In-App Review is available on this device
     * This can be used to determine the best review strategy
     * 
     * @param activity The current Activity context
     * @return true if in-app review is likely available, false otherwise
     */
    fun isInAppReviewAvailable(activity: Activity): Boolean {
        requireInitialized()
        return playStoreReviewManager?.isInAppReviewAvailable(activity) ?: false
    }

    /**
     * Get/set the rating threshold for when to prompt for feedback
     */
    var ratingThreshold: Int
        get() {
            requireInitialized()
            return experienceTracker?.ratingThreshold ?: DEFAULT_RATING_THRESHOLD
        }
        set(value) {
            requireInitialized()
            experienceTracker?.ratingThreshold = value
        }

    /**
     * Get/set the rating threshold for triggering Play Store review
     * Only ratings >= this threshold will trigger the Play Store review flow
     * Default is 4 (matching iOS behavior)
     */
    var playStoreReviewThreshold: Int = 4

    /**
     * Reset experience points and feedback status
     * Use carefully - recommend tracking last prompt date
     */
    fun resetExperienceAndPrompt() {
        requireInitialized()
        experienceTracker?.resetExperienceAndPrompt()
    }

    /**
     * Get current experience tracking state for debugging
     */
    fun getExperienceState(): ExperienceState? {
        requireInitialized()
        return experienceTracker?.getExperienceState()
    }

    /**
     * Get the number of queued feedback submissions waiting to be sent
     * @return Number of feedback items in the offline queue
     */
    fun getQueuedFeedbackCount(): Int {
        requireInitialized()
        return offlineFeedbackQueue?.getQueueSize() ?: 0
    }

    /**
     * Manually process queued feedback submissions
     * Useful for retry when connectivity returns or manual sync
     * Similar to iOS SDK's immediate processing functionality
     */
    fun processQueuedFeedback() {
        requireInitialized()
        offlineFeedbackQueue?.processQueue()
    }



    /**
     * Clear all queued feedback (for testing/reset purposes)
     * WARNING: This will permanently delete all queued feedback
     */
    fun clearQueuedFeedback() {
        requireInitialized()
        offlineFeedbackQueue?.clearQueue()
    }

    /**
     * Handle feedback submission with API call and automatic Play Store review integration
     * This method implements the complete Task 12 flow:
     * 1. Submit feedback to backend
     * 2. If successful and rating >= threshold, trigger Play Store review
     * 3. Handle all analytics callbacks
     */
    private fun handleFeedbackSubmission(
        rating: Int,
        feedback: String,
        onResult: ((success: Boolean, message: String) -> Unit)? = null,
        activity: Activity? = null
    ) {
        ApperoLogger.debug("Submitting feedback: rating=$rating, feedback length=${feedback.length}")
        

        
        // Submit feedback to backend asynchronously using SDK scope
        scope.launch(Dispatchers.IO) {
            val result = submitFeedbackToBackend(rating, feedback)
            withContext(Dispatchers.Main) {
                when (result) {
                    is FeedbackSubmissionResult.Success -> {
                        markFeedbackSubmitted()
                        analyticsListener?.onApperoFeedbackSubmitted(rating, feedback)
                        onResult?.invoke(true, result.message)
                        onFeedbackSubmissionResult?.invoke(true, result.message)
                        ApperoLogger.logApiSuccess("/api/feedback", "POST", 200)
                        

                        
                        // Task 12: Automatic Play Store review integration
                        // This happens after successful feedback submission
                        triggerPlayStoreReviewIfEligible(rating, playStoreReviewThreshold, activity)
                    }
                    is FeedbackSubmissionResult.Error -> {
                        onResult?.invoke(false, result.message)
                        onFeedbackSubmissionResult?.invoke(false, result.message)
                        offlineFeedbackQueue?.queueFeedback(rating, feedback)
                        ApperoLogger.logApiError("/api/feedback", "POST", result.message)
                        
                        // Show debug toast for error
                        if (ApperoLogger.getDebugMode() == ApperoDebugMode.DEBUG) {
                            appContext?.let { context ->
                                android.widget.Toast.makeText(
                                    context,
                                    "❌ Feedback submission failed: ${result.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
                onFeedbackSubmissionResult = null
            }
        }
    }

    /**
     * Trigger Play Store review if the user's rating is eligible
     * This is called automatically after successful feedback submission (Task 12)
     * 
     * @param rating The user's submitted rating
     * @param reviewThreshold The minimum rating to trigger review (default: 4)
     */
    private fun triggerPlayStoreReviewIfEligible(
        rating: Int,
        reviewThreshold: Int = 4,
        activity: Activity? = null
    ) {
        // Use provided activity context or try to get from application context
        val activityContext = activity ?: (getContext() as? Activity)
        if (activityContext == null) {
            ApperoLogger.logNetworkError("Play Store Review", "Cannot trigger review: Activity context not available")
            return
        }

        if (rating >= reviewThreshold) {
            ApperoLogger.logCriticalOperation("Play Store Review", "Auto-triggering review for rating $rating (threshold: $reviewThreshold)")
            

            
                         requestPlayStoreReviewIfRating(
                 activity = activityContext,
                 rating = rating,
                 threshold = reviewThreshold,
                 fallbackToExternalStore = true
             ) { result ->
                 when (result) {
                     is PlayStoreReviewResult.InAppReviewShown -> {
                         ApperoLogger.logCriticalOperation("Play Store Review", "In-app review dialog shown")
                         

                     }
                     is PlayStoreReviewResult.InAppReviewCompleted -> {
                         ApperoLogger.logCriticalOperation("Play Store Review", "In-app review completed")
                         

                     }
                     is PlayStoreReviewResult.FallbackTriggered -> {
                         ApperoLogger.logCriticalOperation("Play Store Review", "Fallback to external Play Store triggered")
                         

                     }
                     is PlayStoreReviewResult.Failed -> {
                         ApperoLogger.logNetworkError("Play Store Review", "Review failed: ${result.reason}")
                         
                         // Show debug toast for failure
                         if (ApperoLogger.getDebugMode() == ApperoDebugMode.DEBUG) {
                             appContext?.let { ctx ->
                                 android.widget.Toast.makeText(
                                     ctx,
                                     "❌ Play Store review failed: ${result.reason}",
                                     android.widget.Toast.LENGTH_LONG
                                 ).show()
                             }
                         }
                     }
                     null -> {
                         // Rating below threshold - this shouldn't happen since we check above
                         ApperoLogger.debug("Play Store Review not triggered - rating below threshold")
                     }
                 }
             }
        } else {
            ApperoLogger.debug("Play Store Review not triggered - rating $rating below threshold $reviewThreshold")
            

        }
    }

    /**
     * Submit feedback to the backend
     */
    private suspend fun submitFeedbackToBackend(rating: Int, feedback: String): FeedbackSubmissionResult {
        val repository = feedbackRepository ?: return FeedbackSubmissionResult.Error("Repository not initialized")

        return repository.submitFeedback(
            rating = rating,
            feedback = feedback
        )
    }

    /**
     * Get the current API key (for internal use)
     */
    internal fun getApiKey(): String? {
        return clientRepository?.getApiKey()
    }

    /**
     * Get the current client ID (for internal use)
     */
    internal fun getClientId(): String? {
        return clientRepository?.getClientId()
    }

    /**
     * Get the application context (for internal use)
     */
    internal fun getContext(): Context? {
        return appContext
    }

    /**
     * Mark that the user has submitted feedback (for internal use)
     */
    internal fun markFeedbackSubmitted() {
        experienceTracker?.markFeedbackSubmitted()
    }
    
    /**
     * Setup network monitoring similar to iOS SDK's NWPathMonitor
     * Monitors network connectivity and triggers immediate queue processing when connectivity returns
     */
    private fun setupNetworkMonitoring(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Network became available - notify offline queue
                offlineFeedbackQueue?.onNetworkStateChanged(true)
                offlineExperienceQueue?.onNetworkStateChanged(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // Network lost - notify offline queue
                offlineFeedbackQueue?.onNetworkStateChanged(false)
                offlineExperienceQueue?.onNetworkStateChanged(false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                offlineFeedbackQueue?.onNetworkStateChanged(hasInternet)
                offlineExperienceQueue?.onNetworkStateChanged(hasInternet)
            }
        }

        // Register network callback
        networkCallback?.let { callback ->
            connectivityManager?.registerDefaultNetworkCallback(callback)
        }
    }

    /**
     * Clean up network monitoring and timers
     */
    fun stop() {
        ApperoLogger.logCriticalOperation("SDK Shutdown", "Stopping Appero SDK")
        try {
            networkCallback?.let { callback ->
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        } catch (_: Exception) { }
        networkCallback = null
        offlineFeedbackQueue?.cleanup()
        offlineExperienceQueue?.cleanup()
        scope.cancel()
    }

    // ==========================================
    // LEGACY XML LAYOUT SUPPORT
    // ==========================================

    /**
     * Show feedback dialog for legacy XML-based projects
     * This creates a DialogFragment with traditional Android Views
     * 
     * @param activity The current Activity (required for FragmentManager)
     * @param config Configuration object containing all text content for the dialog
     * @param onResult Optional callback to receive feedback submission results
     */
    fun showFeedbackDialog(
        activity: androidx.fragment.app.FragmentActivity,
        config: FeedbackPromptConfig,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) {
        requireInitialized()
        
        val dialogFragment = com.appero.sdk.ui.legacy.FeedbackDialogFragment.newInstance()
        dialogFragment.setConfig(config)
        dialogFragment.setAnalyticsListener(analyticsListener)
        dialogFragment.setFlowConfig(FeedbackFlowConfig())
        dialogFragment.setReviewPromptThreshold(playStoreReviewThreshold)
        dialogFragment.setOnRequestReview {
            requestPlayStoreReview(activity)
        }
        
        dialogFragment.setOnSubmitCallback { rating, feedback ->
            handleFeedbackSubmission(rating, feedback, onResult)
        }
        
        dialogFragment.setOnDismissCallback {
            // Dialog dismissed without submission
        }
        
        dialogFragment.show(activity.supportFragmentManager, "ApperoFeedbackDialog")
    }

    /**
     * Show feedback dialog with initial step for legacy XML-based projects
     * 
     * @param activity The current Activity (required for FragmentManager)
     * @param config Configuration object containing all text content for the dialog
     * @param initialStep The initial step to show (currently not implemented for legacy)
     * @param onResult Optional callback to receive feedback submission results
     */
    fun showFeedbackDialog(
        activity: androidx.fragment.app.FragmentActivity,
        config: FeedbackPromptConfig,
        initialStep: FeedbackStep,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) {
        requireInitialized()
        
        val dialogFragment = com.appero.sdk.ui.legacy.FeedbackDialogFragment.newInstance()
        dialogFragment.setConfig(config)
        dialogFragment.setInitialStep(initialStep)
        dialogFragment.setAnalyticsListener(analyticsListener)
        dialogFragment.setFlowConfig(FeedbackFlowConfig())
        dialogFragment.setReviewPromptThreshold(playStoreReviewThreshold)
        dialogFragment.setOnRequestReview {
            requestPlayStoreReview(activity)
        }
        
        dialogFragment.setOnSubmitCallback { rating, feedback ->
            handleFeedbackSubmission(rating, feedback, onResult)
        }
        
        dialogFragment.setOnDismissCallback {
            // Dialog dismissed without submission
        }
        
        dialogFragment.show(activity.supportFragmentManager, "ApperoFeedbackDialog")
    }

    /**
     * Create a feedback DialogFragment for manual management
     * This is for advanced users who want to control the dialog lifecycle
     * 
     * @param config Configuration object containing all text content for the dialog
     * @param onResult Optional callback to receive feedback submission results
     * @return Configured DialogFragment ready to show
     */
    fun createFeedbackDialogFragment(
        config: FeedbackPromptConfig,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ): androidx.fragment.app.DialogFragment {
        requireInitialized()
        
        val dialogFragment = com.appero.sdk.ui.legacy.FeedbackDialogFragment.newInstance()
        dialogFragment.setConfig(config)
        dialogFragment.setAnalyticsListener(analyticsListener)
        
        dialogFragment.setOnSubmitCallback { rating, feedback ->
            handleFeedbackSubmission(rating, feedback, onResult)
        }
        
        return dialogFragment
    }

    // Legacy XML Auto-Triggering Support
    
    /**
     * Register a FragmentActivity for automatic legacy XML dialog triggering
     * When experience thresholds are crossed (positive or frustration), the feedback dialog 
     * will be shown automatically using XML layouts instead of Compose
     * 
     * @param activity The FragmentActivity to use for showing dialogs
     */
    fun registerLegacyActivity(activity: androidx.fragment.app.FragmentActivity) {
        requireInitialized()
        experienceTracker?.registerLegacyActivity(activity)
    }
    
    /**
     * Unregister the current legacy activity (should be called in onDestroy)
     * After unregistering, auto-triggering will fall back to Compose dialogs
     */
    fun unregisterLegacyActivity() {
        experienceTracker?.unregisterLegacyActivity()
    }

    /**
     * Test in-app review functionality with a published app
     * This is useful for SDK development when your app isn't published yet
     * 
     * @param activity The current activity
     * @param testPackageName Package name of a published app to test with (e.g., "com.whatsapp", "com.instagram.android")
     * @param onComplete Optional callback with the result of the review request
     */
    fun testPlayStoreReviewWithPublishedApp(
        activity: Activity,
        testPackageName: String,
        onComplete: ((PlayStoreReviewResult) -> Unit)? = null
    ) {
        requireInitialized()
        
        if (ApperoLogger.getDebugMode() == ApperoDebugMode.DEBUG) {
            android.widget.Toast.makeText(activity, "🧪 Testing Play Store review with: $testPackageName", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        playStoreReviewManager?.testWithPublishedApp(activity, testPackageName) { result ->
            val playStoreResult = when (result) {
                is PlayStoreReviewManager.ReviewResult.InAppReviewShown -> PlayStoreReviewResult.InAppReviewShown
                is PlayStoreReviewManager.ReviewResult.InAppReviewCompleted -> PlayStoreReviewResult.InAppReviewCompleted
                is PlayStoreReviewManager.ReviewResult.FallbackTriggered -> PlayStoreReviewResult.FallbackTriggered
                is PlayStoreReviewManager.ReviewResult.Failed -> PlayStoreReviewResult.Failed(result.reason)
            }
            
            onComplete?.invoke(playStoreResult)
        }
    }

    /**
     * Ensure SDK is initialized before calling methods
     */
    private fun requireInitialized() {
        require(isInitialized) { "Appero SDK must be initialized before use. Call Appero.start() first." }
    }


} 