package com.appero.sdk.debug

/**
 * Debug modes for the Appero SDK that control logging behavior.
 * 
 * The app sets this mode to tell the SDK which environment it's running in,
 * and the SDK uses this to decide whether to log information or not.
 */
enum class ApperoDebugMode {
    /**
     * Production mode - no debug logging.
     * Use this for release builds and production environments.
     */
    PRODUCTION,
    
    /**
     * Debug mode - essential logging for troubleshooting.
     * Use this for development and debugging SDK integration issues.
     * Logs API request/response errors and critical SDK operations.
     */
    DEBUG
} 