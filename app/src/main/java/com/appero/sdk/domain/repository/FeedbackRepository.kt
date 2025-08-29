package com.appero.sdk.domain.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.appero.sdk.data.local.queue.QueuedFeedback
import com.appero.sdk.data.remote.ApperoApiService
import com.appero.sdk.util.AppVersionUtils
import com.appero.sdk.util.DateTimeUtils.getCurrentTimestamp
import com.appero.sdk.util.HttpStatusCode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Repository for handling feedback submission to the Appero backend
 */
internal class FeedbackRepository(
    private val sharedPreferences: SharedPreferences,
    private val apiService: ApperoApiService,
    private val context: Context
) {

    companion object {
        private const val KEY_QUEED_FEEDBACK = "queued_feedback_list"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val TIMEOUT = 30L
    }

    private val gson = Gson()

    /**
     * Submit feedback to the backend with retry logic
     * API key and client ID are automatically added by the interceptor
     *
     * @param rating The rating (1-5)
     * @param feedback The feedback text
     * @return FeedbackSubmissionResult indicating success or failure
     */
    suspend fun submitFeedback(
        rating: Int,
        feedback: String
    ): FeedbackSubmissionResult {

        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                // Create RequestBody objects for multipart form data (raw values, no quotes)
                val mediaType = "text/plain".toMediaTypeOrNull()
                val ratingBody = rating.toString().toRequestBody(mediaType)
                val feedbackBody = feedback.toRequestBody(mediaType)
                val sentAtBody = getCurrentTimestamp().toRequestBody(mediaType)
                val sourceBody = AppVersionUtils.getSource().toRequestBody(mediaType)
                val buildVersionBody = AppVersionUtils.getBuildVersion(context).toRequestBody(mediaType)

                val response = try {
                    // Use withTimeout to prevent hanging indefinitely
                    @Suppress("detekt:MagicNumber")
                    withTimeout(TIMEOUT * 1000) {
                        apiService.feedbackApi.submitFeedback(
                            rating = ratingBody,
                            feedback = feedbackBody,
                            sentAt = sentAtBody,
                            source = sourceBody,
                            buildVersion = buildVersionBody
                        )
                    }
                } catch (e: TimeoutCancellationException) {
                    throw e
                }

                return if (response.isSuccessful) {
                    val body = response.body()

                    if (body == null) {
                        return FeedbackSubmissionResult.Error("Server returned empty response")
                    }

                    if (body.status == "success") {
                        FeedbackSubmissionResult.Success(body.message ?: "Feedback submitted successfully")
                    } else {
                        FeedbackSubmissionResult.Error(
                            "Server returned error: ${body.error ?: body.status}"
                        )
                    }
                } else {
                    val errorMessage = "HTTP ${response.code()}: ${response.message()}"
                    if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                        FeedbackSubmissionResult.Error(errorMessage)
                    } else {
                        // Retry for server errors (5xx) and some client errors
                        if (response.code() >= HttpStatusCode.INTERNAL_SERVER_ERROR.value
                            || response.code() == HttpStatusCode.TOO_MANY_REQUESTS.value
                        ) {
                            delay(RETRY_DELAY_MS * (attempt + 1))
                            return@repeat // Continue to next retry
                        } else {
                            // Don't retry for client errors like 400, 401, 403, 404
                            return FeedbackSubmissionResult.Error(errorMessage)
                        }
                    }
                }

            } catch (e: Exception) {
                // Determine error message based on exception type
                val errorMessage = when (e) {
                    is TimeoutCancellationException -> "Request timed out after 30 seconds"
                    is SocketTimeoutException -> "Connection timed out: ${e.message}"
                    is ConnectException -> "Could not connect to server: ${e.message}"
                    is IOException -> "Network IO error: ${e.message}"
                    else -> "Network error: ${e.message}"
                }

                // Return error if final attempt, otherwise retry
                if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                    return FeedbackSubmissionResult.Error(errorMessage)
                } else {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }

        return FeedbackSubmissionResult.Error("Failed to submit feedback after $MAX_RETRY_ATTEMPTS attempts")
    }


    
    /**
     * Generate current timestamp in ISO 8601 format
     * @return Formatted timestamp string
     */
    fun getQueuedFeedback(): List<QueuedFeedback> {
        val json = sharedPreferences.getString(KEY_QUEED_FEEDBACK, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<QueuedFeedback>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Save queued feedback
     */
    fun saveQueuedFeedback(queuedFeedback: List<QueuedFeedback>) {
        val json = gson.toJson(queuedFeedback)
        sharedPreferences.edit {
            putString(KEY_QUEED_FEEDBACK, json)
        }
    }
}

/**
 * Result of feedback submission
 */
internal sealed class FeedbackSubmissionResult {
    data class Success(val message: String) : FeedbackSubmissionResult()
    data class Error(val message: String) : FeedbackSubmissionResult()
} 