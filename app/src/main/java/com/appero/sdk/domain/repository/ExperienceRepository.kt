package com.appero.sdk.domain.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.appero.sdk.data.remote.ApperoApiService
import com.appero.sdk.util.AppVersionUtils
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
 * Repository for handling experience submission to the Appero backend
 *
 * Note: This class does not queue on failure; queuing is performed by the caller (ExperienceTracker)
 * to avoid duplicate enqueues.
 */
internal class ExperienceRepository(
	private val sharedPreferences: SharedPreferences,
	private val apiService: ApperoApiService,
	private val context: Context
) {

	companion object {
		private const val MAX_RETRY_ATTEMPTS = 3
		private const val RETRY_DELAY_MS = 1000L
		private const val TIMEOUT = 30L
		private const val KEY_QUEUED_EXPERIENCES = "queued_experiences_list"
	}

	private val gson = Gson()

	/**
	 * Submit experience points to the backend with retry logic (configurable)
	 */
	suspend fun submitExperience(
		clientId: String,
		value: Int,
		context: String,
		sentAt: String,
		allowRetry: Boolean = true
	): ExperienceSubmissionResult {
		val attempts = if (allowRetry) MAX_RETRY_ATTEMPTS else 1
		repeat(attempts) { attempt ->
			try {
				val mediaType = "text/plain".toMediaTypeOrNull()
				val clientIdBody = clientId.toRequestBody(mediaType)
				val valueBody = value.toString().toRequestBody(mediaType)
				val contextBody = context.toRequestBody(mediaType)
				val sentAtBody = sentAt.toRequestBody(mediaType)
				val sourceBody = AppVersionUtils.getSource().toRequestBody(mediaType)
				val buildVersionBody = AppVersionUtils.getBuildVersion(this.context).toRequestBody(mediaType)
				
				val response = try {
					withTimeout(TIMEOUT * 1000) {
						apiService.experienceApi.submitExperience(
							clientId = clientIdBody,
							value = valueBody,
							context = contextBody,
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
					if (attempt == attempts - 1) {
						ExperienceSubmissionResult.Error(errorMessage)
					} else {
						if (response.code() >= HttpStatusCode.INTERNAL_SERVER_ERROR.value
							|| response.code() == HttpStatusCode.TOO_MANY_REQUESTS.value
						) {
							delay(RETRY_DELAY_MS * (attempt + 1))
							return@repeat
						} else {
							return ExperienceSubmissionResult.Error(errorMessage)
						}
					}
				}
				
			} catch (e: Exception) {
				val errorMessage = when (e) {
					is TimeoutCancellationException -> "Request timed out after 30 seconds"
					is SocketTimeoutException -> "Connection timed out: ${e.message}"
					is ConnectException -> "Could not connect to server: ${e.message}"
					is IOException -> "Network IO error: ${e.message}"
					else -> "Network error: ${e.message}"
				}
				
				if (attempt == attempts - 1) {
					return ExperienceSubmissionResult.Error(errorMessage)
				} else {
					delay(RETRY_DELAY_MS * (attempt + 1))
				}
			}
		}
		
		return ExperienceSubmissionResult.Error("Failed to submit experience after $attempts attempts")
	}

	fun getQueuedExperiences(): List<QueuedExperience> {
		val json = sharedPreferences.getString(KEY_QUEUED_EXPERIENCES, null) ?: return emptyList()
		return try {
			val type = object : TypeToken<List<QueuedExperience>>() {}.type
			gson.fromJson<List<QueuedExperience>>(json, type) ?: emptyList()
		} catch (e: Exception) {
			emptyList()
		}
	}

	fun saveQueuedExperiences(queuedExperiences: List<QueuedExperience>) {
		val json = gson.toJson(queuedExperiences)
		sharedPreferences.edit { putString(KEY_QUEUED_EXPERIENCES, json) }
	}
}

internal data class QueuedExperience(
	val id: String,
	val value: Int,
	val context: String,
	val sentAt: String,
	val retryCount: Int
)

internal sealed class ExperienceSubmissionResult {
	data class Success(
		val message: String,
		val shouldShowFeedback: Boolean = false,
		val feedbackUI: com.appero.sdk.data.remote.dto.FeedbackUI? = null,
		val flowType: String? = null
	) : ExperienceSubmissionResult()
	data class Error(val message: String) : ExperienceSubmissionResult()
} 