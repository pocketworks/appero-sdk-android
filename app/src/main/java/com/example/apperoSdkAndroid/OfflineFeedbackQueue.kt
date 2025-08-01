package com.example.apperoSdkAndroid

import com.example.apperoSdkAndroid.domain.FeedbackRepository
import com.example.apperoSdkAndroid.domain.FeedbackSubmissionResult
import com.example.apperoSdkAndroid.utils.DateTimeUtils.getCurrentTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.UUID
import kotlin.concurrent.timer

/**
 * Data model for queued feedback
 */
internal data class QueuedFeedback(
    val id: String = UUID.randomUUID().toString(),
    val apiKey: String,
    val clientId: String,
    val rating: Int,
    val feedback: String,
    val timestamp: String,
    val retryCount: Int = 0
)

/**
 * Manages offline feedback queuing and retry logic
 * Stores failed feedback submissions locally and retries when connectivity returns
 * Includes periodic retry timer
 */
internal class OfflineFeedbackQueue(
    private val feedbackRepository: FeedbackRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val MAX_QUEUE_SIZE = 100
        private const val RETRY_TIMER_INTERVAL_MS = 180_000L // 3 minutes
    }

    private var retryTimer: Timer? = null
    private var isNetworkAvailable = false

    init {
        // Start periodic retry timer
        startRetryTimer()
    }

    /**
     * Start periodic retry timer that attempts to process queue every 3 minutes
     */
    private fun startRetryTimer() {
        stopRetryTimer() // Ensure we don't have multiple timers

        retryTimer = timer(
            name = "ApperoRetryTimer",
            daemon = true,
            initialDelay = RETRY_TIMER_INTERVAL_MS,
            period = RETRY_TIMER_INTERVAL_MS
        ) {
            scope.launch {
                processQueue()
            }
        }
    }

    /**
     * Stop the periodic retry timer
     */
    private fun stopRetryTimer() {
        retryTimer?.cancel()
        retryTimer = null
    }

    /**
     * Called when network state changes
     * @param isAvailable true if network is available
     */
    fun onNetworkStateChanged(isAvailable: Boolean) {
        val wasUnavailable = !isNetworkAvailable
        isNetworkAvailable = isAvailable

        // If we regain connectivity and have queued items, process immediately
        if (isAvailable && wasUnavailable && getQueueSize() > 0) {
            scope.launch {
                processQueue()
            }
        }
    }

    /**
     * Add feedback to the offline queue
     * @param apiKey The API key for the feedback
     * @param clientId The client ID for the feedback
     * @param rating The rating (1-5)
     * @param feedback The feedback text
     */
    fun queueFeedback(apiKey: String, clientId: String, rating: Int, feedback: String) {
        val queuedFeedback = QueuedFeedback(
            apiKey = apiKey,
            clientId = clientId,
            rating = rating,
            feedback = feedback,
            timestamp = getCurrentTimestamp()
        )

        val currentQueue = feedbackRepository.getQueuedFeedback().toMutableList()

        // Prevent queue from growing too large
        if (currentQueue.size >= MAX_QUEUE_SIZE) {
            // Remove oldest items
            currentQueue.removeAt(0)
        }

        currentQueue.add(queuedFeedback)
        feedbackRepository.saveQueuedFeedback(currentQueue)
    }

    /**
     * Process all queued feedback submissions
     * Called when network connectivity is available or by periodic retry timer
     */
    fun processQueue() {
        if (!isNetworkAvailable) {
            return // Skip processing if no network
        }

        val queuedItems = feedbackRepository.getQueuedFeedback()
        if (queuedItems.isEmpty()) {
            return // No items to process
        }

        println("[Appero] Processing ${queuedItems.size} queued feedback items")

        scope.launch {
            val successfulSubmissions = mutableListOf<String>()
            val updatedQueue = mutableListOf<QueuedFeedback>()

            for (item in queuedItems) {
                try {
                    val result = feedbackRepository.submitFeedback(
                        apiKey = item.apiKey,
                        clientId = item.clientId,
                        rating = item.rating,
                        feedback = item.feedback
                    )

                    when (result) {
                        is FeedbackSubmissionResult.Success -> {
                            // Successful submission - remove from queue
                            successfulSubmissions.add(item.id)
                        }

                        is FeedbackSubmissionResult.Error -> {
                            // Failed submission - retry or remove based on retry count
                            if (item.retryCount < MAX_RETRY_ATTEMPTS) {
                                // Add back to queue with incremented retry count
                                updatedQueue.add(item.copy(retryCount = item.retryCount + 1))
                            }
                            // If max retries reached, item is dropped (not added to updatedQueue)
                        }
                    }
                } catch (e: Exception) {
                    // Network error - retry if under limit
                    if (item.retryCount < MAX_RETRY_ATTEMPTS) {
                        updatedQueue.add(item.copy(retryCount = item.retryCount + 1))
                    }
                }
            }

            // Update the queue with remaining items
            feedbackRepository.saveQueuedFeedback(updatedQueue)
        }
    }

    /**
     * Get the current number of queued feedback items
     */
    fun getQueueSize(): Int {
        return feedbackRepository.getQueuedFeedback().size
    }

    /**
     * Clear all queued feedback (for testing/reset purposes)
     */
    fun clearQueue() {
        feedbackRepository.saveQueuedFeedback(emptyList())
    }

    /**
     * Clean up resources (stop timer, etc.)
     * Should be called when SDK is being cleaned up
     */
    fun cleanup() {
        stopRetryTimer()
    }
}