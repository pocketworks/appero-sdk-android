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
} 