package com.example.appero_sdk_android

import android.content.Context
import android.content.SharedPreferences

/**
 * Main Appero SDK class - singleton instance for global access
 * Provides intelligent in-app feedback collection and user experience tracking
 */
object Appero {
    
    private const val PREFS_NAME = "appero_sdk_prefs"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_CLIENT_ID = "client_id"
    private const val KEY_IS_INITIALIZED = "is_initialized"
    
    private var context: Context? = null
    private var sharedPreferences: SharedPreferences? = null
    private var experienceTracker: ExperienceTracker? = null
    
    private var apiKey: String? = null
    private var clientId: String? = null
    private var isInitialized: Boolean = false
    
    /**
     * Initialize the Appero SDK with API key and client ID
     * Should be called in Application.onCreate() or MainActivity.onCreate()
     * 
     * @param context Application or Activity context
     * @param apiKey Your Appero API key (UUID format)
     * @param clientId Your Appero client ID (UUID format)
     */
    fun start(context: Context, apiKey: String, clientId: String) {
        this.context = context.applicationContext
        this.sharedPreferences = this.context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Validate input parameters
        require(apiKey.isNotBlank()) { "API key cannot be blank" }
        require(clientId.isNotBlank()) { "Client ID cannot be blank" }
        
        // Store credentials securely
        this.apiKey = apiKey
        this.clientId = clientId
        
        // Persist initialization state and credentials
        sharedPreferences?.edit()?.apply {
            putString(KEY_API_KEY, apiKey)
            putString(KEY_CLIENT_ID, clientId)
            putBoolean(KEY_IS_INITIALIZED, true)
            apply()
        }
        
        this.isInitialized = true
        
        // Initialize core SDK components
        initializeCoreComponents()
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
     * Get/set the rating threshold for when to prompt for feedback
     */
    var ratingThreshold: Int
        get() {
            requireInitialized()
            return experienceTracker?.ratingThreshold ?: 5
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
     * Get the application context (for internal use)
     */
    internal fun getContext(): Context? {
        return context
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
        // Initialize experience tracking
        sharedPreferences?.let { prefs ->
            experienceTracker = ExperienceTracker(prefs)
        }
        
        // TODO: Initialize networking components
        // TODO: Initialize user session management
        // TODO: Initialize UI components
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
        sharedPreferences?.let { prefs ->
            if (prefs.getBoolean(KEY_IS_INITIALIZED, false)) {
                apiKey = prefs.getString(KEY_API_KEY, null)
                clientId = prefs.getString(KEY_CLIENT_ID, null)
                isInitialized = true
            }
        }
    }
    
    init {
        // Attempt to restore state on object creation
        // This will be properly initialized when start() is called
    }
} 