package com.appero.appero_sample_android

import android.util.Log
import com.appero.sdk.analytics.ApperoAnalyticsListener

/**
 * Example implementation of ApperoAnalyticsListener
 */
class ExampleAnalyticsListener : ApperoAnalyticsListener {
    companion object {
        private const val TAG = "ApperoAnalytics"
    }
    override fun onApperoFeedbackSubmitted(rating: Int, feedback: String) {
        Log.i(TAG, "📊 Feedback submitted - Rating: $rating, Feedback length: ${feedback.length}")
        // ... analytics code ...
    }
    override fun onRatingSelected(rating: Int) {
        Log.i(TAG, "📊 Rating selected - Rating: $rating")
        // ... analytics code ...
    }
    
    override fun onPlayStoreReviewRequested() {
        Log.i(TAG, "📊 Play Store review requested")
        // Track when Play Store review is requested
        // Example: FirebaseAnalytics.getInstance().logEvent("appero_play_store_review_requested")
    }
    
    override fun onPlayStoreReviewCompleted(successful: Boolean) {
        Log.i(TAG, "📊 Play Store review completed - Successful: $successful")
        // Track Play Store review completion
        // Example: FirebaseAnalytics.getInstance().logEvent("appero_play_store_review_completed") {
        //     param("successful", successful)
        // }
    }
    
    override fun onPlayStoreFallbackTriggered() {
        Log.i(TAG, "📊 Play Store fallback triggered - Redirected to external Play Store")
        // Track fallback to external Play Store
        // Example: FirebaseAnalytics.getInstance().logEvent("appero_play_store_fallback_triggered")
    }
} 