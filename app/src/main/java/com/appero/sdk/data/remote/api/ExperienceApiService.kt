package com.appero.sdk.data.remote.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit API service for submitting experiences to the Appero backend
 */
internal interface ExperienceApiService {
    
    /**
     * Submit experience points to the Appero backend
     * API key is automatically added by the interceptor
     * 
     * @param clientId The client ID for identification
     * @param value The experience points value
     * @param context Additional context for the experience
     * @param sentAt The timestamp in ISO 8601 format
     * @param source The platform source (always "android")
     * @param buildVersion The app version in format "versionName.versionCode"
     * @return Response indicating success or failure
     */
    @Multipart
    @POST("api/v1/experiences")
    suspend fun submitExperience(
        @Part("client_id") clientId: RequestBody,
        @Part("value") value: RequestBody,
        @Part("context") context: RequestBody,
        @Part("sent_at") sentAt: RequestBody,
        @Part("source") source: RequestBody,
        @Part("build_version") buildVersion: RequestBody
    ): Response<com.appero.sdk.data.remote.dto.ExperienceResponse>
} 