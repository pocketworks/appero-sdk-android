package com.example.appero_sdk_android

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.appero_sdk_android.api.FeedbackRepository
import com.example.appero_sdk_android.api.FeedbackSubmissionResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
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
 * Data model for queued experience
 */
internal data class QueuedExperience(
    val id: String = UUID.randomUUID().toString(),
    val apiKey: String,
    val clientId: String,
    val value: Int,
    val context: String,
    val timestamp: String,
    val retryCount: Int = 0
)

/**
 * Manages offline feedback queuing and retry logic
 * Stores failed feedback submissions locally and retries when connectivity returns
 * Includes periodic retry timer similar to iOS SDK implementation
 */
internal class OfflineFeedbackQueue(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_QUEUED_FEEDBACK = "queued_feedback_list"
        private const val KEY_QUEUED_EXPERIENCE = "queued_experience_list"
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val MAX_QUEUE_SIZE = 100
        private const val RETRY_TIMER_INTERVAL_MS = 180_000L // 3 minutes (same as iOS)
    }
    
    private val gson = Gson()
    private val feedbackRepository = FeedbackRepository()
    private var retryTimer: Timer? = null
    private var isNetworkAvailable = false
    
    init {
        // Initialize network state
        isNetworkAvailable = isNetworkAvailable()
        // Start periodic retry timer (similar to iOS implementation)
        startRetryTimer()
    }
    
    /**
     * Start periodic retry timer that attempts to process queue every 3 minutes
     * Similar to iOS SDK's retryTimerInterval implementation
     */
    private fun startRetryTimer() {
        stopRetryTimer() // Ensure we don't have multiple timers
        
        retryTimer = timer(
            name = "ApperoRetryTimer",
            daemon = true,
            initialDelay = RETRY_TIMER_INTERVAL_MS,
            period = RETRY_TIMER_INTERVAL_MS
        ) {
            CoroutineScope(Dispatchers.IO).launch {
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
     * Called when network state changes (similar to iOS networkMonitor.pathUpdateHandler)
     * @param isAvailable true if network is available
     */
    fun onNetworkStateChanged(isAvailable: Boolean) {
        val wasUnavailable = !isNetworkAvailable
        isNetworkAvailable = isAvailable
        
        // If we regain connectivity and have queued items, process immediately (like iOS)
        if (isAvailable && wasUnavailable && getQueueSize() > 0) {
            CoroutineScope(Dispatchers.IO).launch {
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
        
        val currentQueue = getQueuedFeedback().toMutableList()
        
        // Prevent queue from growing too large
        if (currentQueue.size >= MAX_QUEUE_SIZE) {
            // Remove oldest items
            currentQueue.removeAt(0)
        }
        
        currentQueue.add(queuedFeedback)
        saveQueuedFeedback(currentQueue)
    }
    
    /**
     * Add experience points to the offline queue
     * @param apiKey The API key for the experience
     * @param clientId The client ID for the experience
     * @param value The experience points value
     * @param context Additional context for the experience
     */
    fun queueExperience(apiKey: String, clientId: String, value: Int, context: String) {
        val queuedExperience = QueuedExperience(
            apiKey = apiKey,
            clientId = clientId,
            value = value,
            context = context,
            timestamp = getCurrentTimestamp()
        )
        
        val currentQueue = getQueuedExperience().toMutableList()
        
        // Prevent queue from growing too large
        if (currentQueue.size >= MAX_QUEUE_SIZE) {
            // Remove oldest items
            currentQueue.removeAt(0)
        }
        
        currentQueue.add(queuedExperience)
        saveQueuedExperience(currentQueue)
    }
    
    /**
     * Process all queued feedback submissions
     * Called when network connectivity is available or by periodic retry timer
     */
    fun processQueue() {
        // Update network state before processing
        isNetworkAvailable = isNetworkAvailable()
        
        if (!isNetworkAvailable) {
            return // Skip processing if no network
        }
        
        val queuedItems = getQueuedFeedback()
        if (queuedItems.isEmpty()) {
            return // No items to process
        }
        
        // Similar to iOS: Log processing start
        Log.i("Appero", "Processing ${queuedItems.size} queued feedback items")
        
        CoroutineScope(Dispatchers.IO).launch {
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
            // Remove successfully submitted items from the queue
            val remaining = queuedItems.filter { it.id !in successfulSubmissions } + updatedQueue
            saveQueuedFeedback(remaining)
        }
    }
    
    /**
     * Get the current number of queued feedback items
     */
    fun getQueueSize(): Int {
        return getQueuedFeedback().size
    }
    
    /**
     * Clear all queued feedback (for testing/reset purposes)
     */
    fun clearQueue() {
        saveQueuedFeedback(emptyList())
    }
    
    /**
     * Clean up resources (stop timer, etc.)
     * Should be called when SDK is being cleaned up
     */
    fun cleanup() {
        stopRetryTimer()
    }
    
    /**
     * Check if network is available
     * Uses modern network monitoring approach to avoid deprecation warnings
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * Get queued feedback from SharedPreferences
     */
    private fun getQueuedFeedback(): List<QueuedFeedback> {
        val json = sharedPreferences.getString(KEY_QUEUED_FEEDBACK, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<QueuedFeedback>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Save queued feedback to SharedPreferences
     */
    private fun saveQueuedFeedback(queuedFeedback: List<QueuedFeedback>) {
        val json = gson.toJson(queuedFeedback)
        sharedPreferences.edit()
            .putString(KEY_QUEUED_FEEDBACK, json)
            .apply()
    }

    /**
     * Get queued experience from SharedPreferences
     */
    private fun getQueuedExperience(): List<QueuedExperience> {
        val json = sharedPreferences.getString(KEY_QUEUED_EXPERIENCE, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<QueuedExperience>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Save queued experience to SharedPreferences
     */
    private fun saveQueuedExperience(queuedExperience: List<QueuedExperience>) {
        val json = gson.toJson(queuedExperience)
        sharedPreferences.edit()
            .putString(KEY_QUEUED_EXPERIENCE, json)
            .apply()
    }
    
    /**
     * Generate current timestamp in ISO 8601 format
     */
    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }
} 