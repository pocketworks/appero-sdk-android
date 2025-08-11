package com.example.apperoSdkAndroid

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.apperoSdkAndroid.domain.FeedbackRepository
import com.example.apperoSdkAndroid.domain.FeedbackSubmissionResult
import com.example.apperoSdkAndroid.domain.ClientRepository
import com.example.apperoSdkAndroid.domain.UserRepository
import com.example.apperoSdkAndroid.domain.UserRepository.Companion.DEFAULT_RATING_THRESHOLD
import com.example.apperoSdkAndroid.ui.ApperoTheme
import com.example.apperoSdkAndroid.ui.DefaultTheme
import com.example.apperoSdkAndroid.ui.FeedbackFlowConfig
import com.example.apperoSdkAndroid.ui.FeedbackPrompt
import com.example.apperoSdkAndroid.ui.FeedbackPromptConfig
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.google.android.play.core.review.ReviewInfo
import com.google.android.gms.tasks.Task

/**
 * Main Appero SDK class - singleton instance for global access
 * Provides intelligent in-app feedback collection and user experience tracking
 */
object Appero {

    private const val PREFS_NAME = "appero_sdk_prefs"

    private var clientRepository: ClientRepository? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var experienceTracker: ExperienceTracker? = null
    private var feedbackRepository: FeedbackRepository? = null
    private var offlineFeedbackQueue: OfflineFeedbackQueue? = null

    private var apiKey: String? = null
    private var clientId: String? = null
    private var isInitialized: Boolean = false

    // Network monitoring
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Theming system
    var theme: ApperoTheme = DefaultTheme()

    // Analytics integration
    private var analyticsListener: ApperoAnalyticsListener? = null

    /**
     * Set the analytics listener for Appero events
     * @param listener Your implementation of ApperoAnalyticsListener (or null to remove)
     */
    fun setAnalyticsListener(listener: ApperoAnalyticsListener?) {
        analyticsListener = listener
    }

    // UI state for feedback prompt
    private var _showFeedbackPrompt: MutableState<Boolean> = mutableStateOf(false)
    private var _feedbackPromptConfig: MutableState<FeedbackPromptConfig?> = mutableStateOf(null)

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
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        experienceTracker = ExperienceTracker(UserRepository(sharedPreferences))
        feedbackRepository = FeedbackRepository(sharedPreferences).also {
            offlineFeedbackQueue = OfflineFeedbackQueue(it, scope)
        }

        initializeClient(apiKey, clientId, sharedPreferences)
        setupNetworkMonitoring(context)

        isInitialized = true
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
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) {
        requireInitialized()
        val currentConfig = _feedbackPromptConfig.value ?: config
        FeedbackPrompt(
            visible = _showFeedbackPrompt.value,
            config = currentConfig,
            theme = theme,
            analyticsListener = analyticsListener,
            onSubmit = { rating, feedback ->
                handleFeedbackSubmission(rating, feedback, onResult)
                // Keep the bottom sheet open; content will change via FeedbackPrompt's state
            },
            onDismiss = {
                _showFeedbackPrompt.value = false
                _feedbackPromptConfig.value = null
                onFeedbackSubmissionResult = null
            },
            flowConfig = flowConfig,
            reviewPromptThreshold = reviewPromptThreshold,
            onRequestReview = onRequestReview
        )
    }

    /**
     * Trigger the Play Store review prompt using Play Core API
     * @param activity The current Activity
     * @param onComplete Optional callback when the review flow finishes
     */
    fun requestPlayStoreReview(activity: Activity, onComplete: (() -> Unit)? = null) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task: Task<ReviewInfo> ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    onComplete?.invoke()
                }
            } else {
                // Fallback: just call onComplete
                onComplete?.invoke()
            }
        }
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
     * Force offline mode for testing (similar to iOS SDK's forceOfflineMode)
     * When enabled, all feedback will be queued regardless of network status
     * @param forceOffline true to force offline behavior
     */
    fun setForceOfflineMode(forceOffline: Boolean) {
        requireInitialized()
        // Implementation would depend on offline queue supporting force offline mode
        // For now, this is a placeholder for the API
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
     * Handle feedback submission with API call
     * FIXED: Only mark as submitted after successful API response
     */
    private fun handleFeedbackSubmission(
        rating: Int,
        feedback: String,
        onResult: ((success: Boolean, message: String) -> Unit)? = null
    ) {
        // Submit feedback to backend asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            val result = submitFeedbackToBackend(rating, feedback)

            // Call the callback on the main thread
            CoroutineScope(Dispatchers.Main).launch {
                when (result) {
                    is FeedbackSubmissionResult.Success -> {
                        // ✅ Only mark as submitted after successful API response
                        markFeedbackSubmitted()

                        // 📊 Analytics: Log successful feedback submission
                        analyticsListener?.onApperoFeedbackSubmitted(rating, feedback)

                        onResult?.invoke(true, result.message)
                        onFeedbackSubmissionResult?.invoke(true, result.message)
                    }

                    is FeedbackSubmissionResult.Error -> {
                        // ❌ Don't mark as submitted on failure - user can be prompted again
                        onResult?.invoke(false, result.message)
                        onFeedbackSubmissionResult?.invoke(false, result.message)

                        // ✅ Queue for offline retry
                        apiKey?.let { key ->
                            clientId?.let { id ->
                                offlineFeedbackQueue?.  queueFeedback(key, id, rating, feedback)
                            }
                        }
                    }
                }
                onFeedbackSubmissionResult = null
            }
        }
    }

    /**
     * Submit feedback to the backend
     */
    private suspend fun submitFeedbackToBackend(rating: Int, feedback: String): FeedbackSubmissionResult {
        val repository = feedbackRepository ?: return FeedbackSubmissionResult.Error("Repository not initialized")
        val apiKey = getApiKey() ?: return FeedbackSubmissionResult.Error("API key not available")
        val clientId = getClientId() ?: return FeedbackSubmissionResult.Error("Client ID not available")

        return repository.submitFeedback(
            apiKey = apiKey,
            clientId = clientId,
            rating = rating,
            feedback = feedback
        )
    }

    /**
     * Get the current API key (for internal use)
     */
    internal fun getApiKey(): String? {
        return apiKey
    }

    /**
     * Get the current client ID (for internal use)
     */
    internal fun getClientId(): String? {
        return clientId
    }

    /**
     * Mark that the user has submitted feedback (for internal use)
     */
    internal fun markFeedbackSubmitted() {
        experienceTracker?.markFeedbackSubmitted()
    }

    /**
     * Initialize core SDK components
     */
    private fun initializeCoreComponents() {
        // Initialize experience tracking with user session management
        sharedPreferences?.let { prefs ->
            context?.let { ctx ->
                experienceTracker = ExperienceTracker(prefs, ctx)
            }
            
            // Initialize offline feedback queue with retry timer
            context?.let { ctx ->
                offlineFeedbackQueue = OfflineFeedbackQueue(ctx, prefs)
                // Set up network monitoring (similar to iOS NWPathMonitor)
                setupNetworkMonitoring(ctx)
                // Process any queued feedback from previous sessions
                offlineFeedbackQueue?.processQueue()
            }
        }
        
        // Initialize networking components
        feedbackRepository = FeedbackRepository()
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
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // Network lost - notify offline queue
                offlineFeedbackQueue?.onNetworkStateChanged(false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                offlineFeedbackQueue?.onNetworkStateChanged(hasInternet)
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
        networkCallback?.let { callback ->
            connectivityManager?.unregisterNetworkCallback(callback)
        }
        offlineFeedbackQueue?.cleanup()
        scope.cancel()
    }

    /**
     * Ensure SDK is initialized before calling methods
     */
    private fun requireInitialized() {
        require(isInitialized) { "Appero SDK must be initialized before use. Call Appero.start() first." }
    }

    /**
     * Restore SDK state from SharedPreferences if previously initialized
     */
    private fun restoreFromPreferences() {
        if (clientRepository?.getIsApperoInitialized() != true) {
            apiKey = clientRepository?.getApiKey()
            clientId = clientRepository?.getClientId()
            isInitialized = true
        }
    }
}