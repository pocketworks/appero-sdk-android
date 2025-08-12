package com.example.apperoSdkAndroid

/**
 * Interface for integrating third-party analytics with Appero SDK
 * 
 * Implement this interface to receive analytics events from the Appero SDK
 * and forward them to your analytics platform (Firebase, Mixpanel, etc.)
 * 
 * Example integration:
 * ```kotlin
 * class MyAnalyticsListener : ApperoAnalyticsListener {
 *     override fun onApperoFeedbackSubmitted(rating: Int, feedback: String) {
 *         FirebaseAnalytics.getInstance().logEvent("appero_feedback_submitted") {
 *             param("rating", rating.toLong())
 *             param("feedback_length", feedback.length.toLong())
 *         }
 *     }
 *     
 *     override fun onRatingSelected(rating: Int) {
 *         FirebaseAnalytics.getInstance().logEvent("appero_rating_selected") {
 *             param("rating", rating.toLong())
 *         }
 *     }
 * }
 * 
 * // Set the listener
 * Appero.setAnalyticsListener(MyAnalyticsListener())
 * ```
 */
interface ApperoAnalyticsListener {
    /**
     * Called when the user submits complete feedback with rating and text
     * @param rating The rating selected by the user (1-5)
     * @param feedback The text feedback provided by the user (may be empty)
     */
    fun onApperoFeedbackSubmitted(rating: Int, feedback: String)
    /**
     * Called when the user selects a rating (before submitting)
     * @param rating The rating selected by the user (1-5)
     */
    fun onRatingSelected(rating: Int)
} 