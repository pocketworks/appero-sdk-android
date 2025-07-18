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
     * Initialize core SDK components
     */
    private fun initializeCoreComponents() {
        // TODO: Initialize networking components
        // TODO: Initialize user session management
        // TODO: Initialize experience tracking
        // TODO: Initialize UI components
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