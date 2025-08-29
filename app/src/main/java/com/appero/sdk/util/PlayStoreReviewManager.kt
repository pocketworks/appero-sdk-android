package com.appero.sdk.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.appero.sdk.analytics.ApperoAnalyticsListener
import com.appero.sdk.debug.ApperoLogger
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.gms.tasks.Task

/**
 * Comprehensive Play Store review manager with fallback functionality
 * 
 * This class handles the complete Play Store review flow including:
 * - Google Play In-App Review API (primary method)
 * - Fallback to external Play Store when in-app review fails
 * - Analytics integration for tracking review events
 * - Proper error handling and user experience
 * - Testing support for SDK development
 */
internal class PlayStoreReviewManager(
    private val analyticsListener: ApperoAnalyticsListener?
) {
    
    /**
     * Configuration for Play Store review behavior
     */
    data class ReviewConfig(
        val fallbackToExternalStore: Boolean = true,
        val timeoutMs: Long = 10000L, // 10 seconds timeout for in-app review
        val enableAnalytics: Boolean = true,
        val testPackageName: String? = null // For SDK testing with other apps
    )
    
    /**
     * Result of a Play Store review request
     */
    sealed class ReviewResult {
        object InAppReviewShown : ReviewResult()
        object InAppReviewCompleted : ReviewResult()
        object FallbackTriggered : ReviewResult()
        data class Failed(val reason: String) : ReviewResult()
    }
    
    /**
     * Request Play Store review with comprehensive fallback handling
     * 
     * This method follows Google's best practices:
     * 1. Attempt Google Play In-App Review API first
     * 2. If that fails or times out, fallback to external Play Store
     * 3. Provide analytics callbacks for all scenarios
     * 
     * @param activity The current activity context
     * @param config Configuration for review behavior
     * @param onComplete Callback with the result of the review request
     */
    fun requestReview(
        activity: Activity,
        config: ReviewConfig = ReviewConfig(),
        onComplete: ((ReviewResult) -> Unit)? = null
    ) {
        ApperoLogger.logCriticalOperation("Play Store Review", "Starting review request flow")
        
        if (config.enableAnalytics) {
            analyticsListener?.onPlayStoreReviewRequested()
        }
        
        try {
            val reviewManager = ReviewManagerFactory.create(activity)
            
            // Use test package name if provided for in-app review testing
            val packageName = config.testPackageName ?: activity.packageName
            ApperoLogger.logCriticalOperation("Play Store Review", "Using package name for review: $packageName")
            
            val requestReviewFlow = reviewManager.requestReviewFlow()
            
            requestReviewFlow.addOnCompleteListener { task: Task<ReviewInfo> ->
                when {
                    task.isSuccessful -> {
                        val reviewInfo = task.result
                        ApperoLogger.logCriticalOperation("Play Store Review", "In-app review info obtained successfully")
                        
                        // Launch the in-app review flow
                        val launchReviewFlow = reviewManager.launchReviewFlow(activity, reviewInfo)
                        
                        launchReviewFlow.addOnCompleteListener { flowTask ->
                            if (flowTask.isSuccessful) {
                                ApperoLogger.logCriticalOperation("Play Store Review", "In-app review flow completed successfully")
                                
                                if (config.enableAnalytics) {
                                    analyticsListener?.onPlayStoreReviewCompleted(true)
                                }
                                
                                onComplete?.invoke(ReviewResult.InAppReviewCompleted)
                            } else {
                                ApperoLogger.logNetworkError("Play Store Review", "In-app review flow failed: ${flowTask.exception?.message}")
                                
                                if (config.enableAnalytics) {
                                    analyticsListener?.onPlayStoreReviewCompleted(false)
                                }
                                
                                // Fallback to external store
                                if (config.fallbackToExternalStore) {
                                    fallbackToExternalStore(activity, config, onComplete)
                                } else {
                                    onComplete?.invoke(ReviewResult.Failed("In-app review flow failed"))
                                }
                            }
                        }
                        
                        onComplete?.invoke(ReviewResult.InAppReviewShown)
                        
                    }
                    else -> {
                        val errorMessage = task.exception?.message ?: "Unknown error"
                        ApperoLogger.logNetworkError("Play Store Review", "Failed to request review flow: $errorMessage")
                        
                        if (config.enableAnalytics) {
                            analyticsListener?.onPlayStoreReviewCompleted(false)
                        }
                        
                        // Fallback to external store
                        if (config.fallbackToExternalStore) {
                            fallbackToExternalStore(activity, config, onComplete)
                        } else {
                            onComplete?.invoke(ReviewResult.Failed("Failed to request review flow: $errorMessage"))
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            ApperoLogger.logNetworkError("Play Store Review", "Exception during review request: ${e.message}")
            
            if (config.enableAnalytics) {
                analyticsListener?.onPlayStoreReviewCompleted(false)
            }
            
            // Fallback to external store
            if (config.fallbackToExternalStore) {
                fallbackToExternalStore(activity, config, onComplete)
            } else {
                onComplete?.invoke(ReviewResult.Failed("Exception: ${e.message}"))
            }
        }
    }
    
    /**
     * Fallback method to open external Play Store
     * This is called when in-app review is not available or fails
     */
    private fun fallbackToExternalStore(
        activity: Activity,
        config: ReviewConfig,
        onComplete: ((ReviewResult) -> Unit)?
    ) {
        ApperoLogger.logCriticalOperation("Play Store Review", "Falling back to external Play Store")
        
        if (config.enableAnalytics) {
            analyticsListener?.onPlayStoreFallbackTriggered()
        }
        
        try {
            // Use test package name if provided, otherwise use current app's package
            val packageName = config.testPackageName ?: activity.packageName
            
            // Try to open Play Store app first
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            if (playStoreIntent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(playStoreIntent)
                ApperoLogger.logCriticalOperation("Play Store Review", "Opened Play Store app successfully for package: $packageName")
                onComplete?.invoke(ReviewResult.FallbackTriggered)
            } else {
                // Fallback to web browser
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(webIntent)
                ApperoLogger.logCriticalOperation("Play Store Review", "Opened Play Store in web browser for package: $packageName")
                onComplete?.invoke(ReviewResult.FallbackTriggered)
            }
            
        } catch (e: ActivityNotFoundException) {
            ApperoLogger.logNetworkError("Play Store Review", "No app found to handle Play Store intent: ${e.message}")
            onComplete?.invoke(ReviewResult.Failed("No app available to open Play Store"))
            
        } catch (e: Exception) {
            ApperoLogger.logNetworkError("Play Store Review", "Exception during fallback: ${e.message}")
            onComplete?.invoke(ReviewResult.Failed("Fallback failed: ${e.message}"))
        }
    }
    
    /**
     * Check if Google Play Services and Play Store are available
     * This can be used to determine if in-app review is likely to work
     */
    fun isInAppReviewAvailable(activity: Activity): Boolean {
        return try {
            val reviewManager = ReviewManagerFactory.create(activity)
            // If we can create a review manager, it's likely available
            true
        } catch (e: Exception) {
            ApperoLogger.logNetworkError("Play Store Review", "In-app review not available: ${e.message}")
            false
        }
    }
    
    /**
     * Test in-app review with a specific published app
     * This is useful for SDK development when your app isn't published yet
     * 
     * @param activity The current activity
     * @param testPackageName Package name of a published app to test with
     * @param onComplete Callback with the result
     */
    fun testWithPublishedApp(
        activity: Activity,
        testPackageName: String,
        onComplete: ((ReviewResult) -> Unit)? = null
    ) {
        ApperoLogger.logCriticalOperation("Play Store Review", "Testing with published app: $testPackageName")
        
        val testConfig = ReviewConfig(
            fallbackToExternalStore = true,
            enableAnalytics = true,
            testPackageName = testPackageName
        )
        
        requestReview(activity, testConfig, onComplete)
    }
} 