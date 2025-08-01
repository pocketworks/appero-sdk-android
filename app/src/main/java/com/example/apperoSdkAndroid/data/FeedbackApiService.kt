package com.example.apperoSdkAndroid.data

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit API service for submitting feedback to the Appero backend
 */
internal interface FeedbackApiService {
    
    /**
     * Submit feedback to the Appero backend
     * 
     * @param apiKey The API key for authentication
     * @param clientId The client ID for identification
     * @param rating The rating (1-5)
     * @param feedback The feedback text
     * @param sentAt The timestamp in ISO 8601 format
     * @return Response indicating success or failure
     */
    @Multipart
    @POST("api/feedback")
    suspend fun submitFeedback(
        @Part("api_key") apiKey: RequestBody,
        @Part("client_id") clientId: RequestBody,
        @Part("rating") rating: RequestBody,
        @Part("feedback") feedback: RequestBody,
        @Part("sent_at") sentAt: RequestBody
    ): Response<FeedbackResponse>
}

/**
 * Response model for feedback submission
 * Matches the actual API response format: {"message":"Thank you for your feedback!","status":"success"}
 */
internal data class FeedbackResponse(
    val status: String,
    val message: String? = null,
    val error: String? = null
) 