package com.example.apperoSdkAndroid.domain

import com.example.apperoSdkAndroid.data.ApperoApiService
import com.example.apperoSdkAndroid.data.ExperienceResponse
import com.example.apperoSdkAndroid.utils.HttpStatusCode
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Repository for handling experience submission to the Appero backend
 */
internal class ExperienceRepository(
	private val apiService: ApperoApiService
) {

	companion object {
		private const val MAX_RETRY_ATTEMPTS = 3
		private const val RETRY_DELAY_MS = 1000L
		private const val TIMEOUT = 30L
	}

	/**
	 * Submit experience points to the backend with retry logic
	 * API key is automatically added by the interceptor
	 * 
	 * @param clientId The client ID for identification
	 * @param value The experience points value
	 * @param context Additional context for the experience
	 * @param sentAt The timestamp when the experience was sent
	 * @return ExperienceSubmissionResult indicating success or failure
	 */
	suspend fun submitExperience(
		clientId: String,
		value: Int,
		context: String,
		sentAt: String
	): ExperienceSubmissionResult {
		
		repeat(MAX_RETRY_ATTEMPTS) { attempt ->
			try {
				// Create RequestBody objects for multipart form data
				val mediaType = "text/plain".toMediaTypeOrNull()
				val clientIdBody = clientId.toRequestBody(mediaType)
				val valueBody = value.toString().toRequestBody(mediaType)
				val contextBody = context.toRequestBody(mediaType)
				val sentAtBody = sentAt.toRequestBody(mediaType)
				
				val response = try {
					// Use withTimeout to prevent hanging indefinitely
					withTimeout(TIMEOUT * 1000) {
						apiService.experienceApi.submitExperience(
							clientId = clientIdBody,
							value = valueBody,
							context = contextBody,
							sentAt = sentAtBody
						)
					}
				} catch (e: TimeoutCancellationException) {
					throw e
				}
				
				return if (response.isSuccessful) {
					val body = response.body()
					
					if (body == null) {
						return ExperienceSubmissionResult.Error("Server returned empty response")
					}
					
					if (body.status == "success") {
						ExperienceSubmissionResult.Success(
							message = body.message ?: "Experience submitted successfully",
							shouldShowFeedback = body.should_show_feedback,
							feedbackUI = body.feedback_ui,
							flowType = body.flow_type
						)
					} else {
						ExperienceSubmissionResult.Error("Server returned error: ${body.error ?: body.status ?: "Unknown error"}")
					}
				} else {
					val errorMessage = "HTTP ${response.code()}: ${response.message()}"
					if (attempt == MAX_RETRY_ATTEMPTS - 1) {
						ExperienceSubmissionResult.Error(errorMessage)
					} else {
						// Retry for server errors (5xx) and some client errors
						if (response.code() >= HttpStatusCode.INTERNAL_SERVER_ERROR.value
							|| response.code() == HttpStatusCode.TOO_MANY_REQUESTS.value
						) {
							delay(RETRY_DELAY_MS * (attempt + 1))
							return@repeat // Continue to next retry
						} else {
							// Don't retry for client errors like 400, 401, 403, 404
							return ExperienceSubmissionResult.Error(errorMessage)
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
					return ExperienceSubmissionResult.Error(errorMessage)
				} else {
					delay(RETRY_DELAY_MS * (attempt + 1))
				}
			}
		}
		
		return ExperienceSubmissionResult.Error("Failed to submit experience after $MAX_RETRY_ATTEMPTS attempts")
	}
}

/**
 * Result of experience submission
 */
internal sealed class ExperienceSubmissionResult {
	data class Success(
		val message: String,
		val shouldShowFeedback: Boolean = false,
		val feedbackUI: com.example.apperoSdkAndroid.data.FeedbackUI? = null,
		val flowType: String? = null
	) : ExperienceSubmissionResult()
	data class Error(val message: String) : ExperienceSubmissionResult()
} 