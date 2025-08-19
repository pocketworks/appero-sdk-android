package com.appero.sdk.analytics

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
 *     
 *     override fun onPlayStoreReviewRequested() {
 *         FirebaseAnalytics.getInstance().logEvent("appero_play_store_review_requested")
 *     }
 *     
 *     override fun onPlayStoreReviewCompleted(successful: Boolean) {
 *         FirebaseAnalytics.getInstance().logEvent("appero_play_store_review_completed") {
 *             param("successful", successful)
 *         }
 *     }
 *     
 *     override fun onPlayStoreFallbackTriggered() {
 *         FirebaseAnalytics.getInstance().logEvent("appero_play_store_fallback_triggered")
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
    
    /**
     * Called when the Play Store in-app review is requested
     * This is called regardless of whether the review dialog actually appears
     */
    fun onPlayStoreReviewRequested()
    
    /**
     * Called when the Play Store in-app review flow completes
     * @param successful True if the in-app review was attempted, false if it failed
     */
    fun onPlayStoreReviewCompleted(successful: Boolean)
    
    /**
     * Called when the SDK falls back to opening the external Play Store
     * This happens when in-app review is not available or fails
     */
    fun onPlayStoreFallbackTriggered()
} 