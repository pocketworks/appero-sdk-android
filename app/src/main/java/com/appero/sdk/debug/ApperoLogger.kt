package com.appero.sdk.debug

import android.util.Log

/**
 * Internal logger for the Appero SDK.
 * 
 * This class handles all logging based on the current debug mode.
 * Focuses on essential API request errors and critical SDK operations.
 */
internal object ApperoLogger {
    
    private const val TAG = "ApperoSDK"
    private var currentMode: ApperoDebugMode = ApperoDebugMode.PRODUCTION
    
    // String constants for better organization
    private object Strings {
        const val DEBUG_MODE_SET = "Debug mode set to: %s"
        const val API_ERROR_FORMAT = "API Error: %s %s (Status: %d) - %s"
        const val API_ERROR_FORMAT_NO_STATUS = "API Error: %s %s - %s"
        const val API_SUCCESS_FORMAT = "API Success: %s %s (Status: %d)"
        const val NETWORK_ERROR_FORMAT = "Network Error (%s): %s"
        const val CRITICAL_OPERATION_FORMAT = "%s: %s"
        const val CRITICAL_OPERATION_NO_DETAILS = "%s"
    }
    
    /**
     * Set the current debug mode for the SDK.
     * This should be called during SDK initialization.
     */
    fun setDebugMode(mode: ApperoDebugMode) {
        currentMode = mode
        if (shouldLog()) {
            Log.i(TAG, String.format(Strings.DEBUG_MODE_SET, mode.name))
        }
    }
    
    /**
     * Get the current debug mode.
     */
    fun getDebugMode(): ApperoDebugMode = currentMode
    
    /**
     * Log an info message.
     * Only logs if debug mode is enabled.
     */
    fun info(message: String) {
        if (shouldLog()) {
            Log.i(TAG, message)
        }
    }
    
    /**
     * Log a debug message.
     * Only logs if debug mode is enabled.
     */
    fun debug(message: String) {
        if (shouldLog()) {
            Log.d(TAG, message)
        }
    }
    
    /**
     * Log a warning message.
     * Always logs regardless of debug mode.
     */
    fun warning(message: String) {
        Log.w(TAG, message)
    }
    
    /**
     * Log an error message.
     * Always logs regardless of debug mode.
     */
    fun error(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }
    
    /**
     * Log API request errors.
     * Only logs in DEBUG mode.
     */
    fun logApiError(url: String, method: String, error: String, statusCode: Int? = null) {
        if (shouldLog()) {
            val message = if (statusCode != null) {
                String.format(Strings.API_ERROR_FORMAT, method, url, statusCode, error)
            } else {
                String.format(Strings.API_ERROR_FORMAT_NO_STATUS, method, url, error)
            }
            Log.e(TAG, message)
        }
    }
    
    /**
     * Log API request success for critical operations.
     * Only logs in DEBUG mode.
     */
    fun logApiSuccess(url: String, method: String, statusCode: Int) {
        if (shouldLog()) {
            val message = String.format(Strings.API_SUCCESS_FORMAT, method, url, statusCode)
            Log.d(TAG, message)
        }
    }
    
    /**
     * Log network connectivity issues.
     * Only logs in DEBUG mode.
     */
    fun logNetworkError(operation: String, error: String) {
        if (shouldLog()) {
            val message = String.format(Strings.NETWORK_ERROR_FORMAT, operation, error)
            Log.e(TAG, message)
        }
    }
    
    /**
     * Log critical SDK operations.
     * Only logs in DEBUG mode.
     */
    fun logCriticalOperation(operation: String, details: String? = null) {
        if (shouldLog()) {
            val message = if (details != null) {
                String.format(Strings.CRITICAL_OPERATION_FORMAT, operation, details)
            } else {
                String.format(Strings.CRITICAL_OPERATION_NO_DETAILS, operation)
            }
            Log.i(TAG, message)
        }
    }
    
    /**
     * Determine if logging should occur based on the current debug mode.
     */
    private fun shouldLog(): Boolean {
        return currentMode == ApperoDebugMode.DEBUG
    }
} 