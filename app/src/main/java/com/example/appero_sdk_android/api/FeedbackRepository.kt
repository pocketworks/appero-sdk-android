package com.example.appero_sdk_android.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for handling feedback submission to the Appero backend
 */
internal class FeedbackRepository {
    
    companion object {
        private const val BASE_URL = "https://app.appero.co.uk/"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    private val apiService: FeedbackApiService
    
    init {
        // Create HTTP client with logging for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        
        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(FeedbackApiService::class.java)
    }
    
    /**
     * Submit feedback to the backend with retry logic
     * 
     * @param apiKey The API key for authentication
     * @param clientId The client ID for identification
     * @param rating The rating (1-5)
     * @param feedback The feedback text
     * @return FeedbackSubmissionResult indicating success or failure
     */
    suspend fun submitFeedback(
        apiKey: String,
        clientId: String,
        rating: Int,
        feedback: String
    ): FeedbackSubmissionResult {
        
        val sentAt = getCurrentTimestamp()
        


        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                // Create RequestBody objects for multipart form data (raw values, no quotes)
                val mediaType = "text/plain".toMediaTypeOrNull()
                val apiKeyBody = apiKey.toRequestBody(mediaType)
                val clientIdBody = clientId.toRequestBody(mediaType)
                val ratingBody = rating.toString().toRequestBody(mediaType)
                val feedbackBody = feedback.toRequestBody(mediaType)
                val sentAtBody = sentAt.toRequestBody(mediaType)
                
                val response = try {
                    // Use withTimeout to prevent hanging indefinitely
                    withTimeout(30_000) { // 30 second timeout
                        apiService.submitFeedback(
                            apiKey = apiKeyBody,
                            clientId = clientIdBody,
                            rating = ratingBody,
                            feedback = feedbackBody,
                            sentAt = sentAtBody
                        )
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
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
                        FeedbackSubmissionResult.Error("Server returned error: ${body.error ?: body.status ?: "Unknown error"}")
                    }
                } else {
                    val errorMessage = "HTTP ${response.code()}: ${response.message()}"
                    if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                        FeedbackSubmissionResult.Error(errorMessage)
                    } else {
                        // Retry for server errors (5xx) and some client errors
                        if (response.code() >= 500 || response.code() == 429) {
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
                    is kotlinx.coroutines.TimeoutCancellationException -> "Request timed out after 30 seconds"
                    is java.net.SocketTimeoutException -> "Connection timed out: ${e.message}"
                    is java.net.ConnectException -> "Could not connect to server: ${e.message}"
                    is java.io.IOException -> "Network IO error: ${e.message}"
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
    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }
}

/**
 * Result of feedback submission
 */
internal sealed class FeedbackSubmissionResult {
    data class Success(val message: String) : FeedbackSubmissionResult()
    data class Error(val message: String) : FeedbackSubmissionResult()
} 