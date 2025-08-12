package com.example.apperoSdkAndroid.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Centralized API service for all Appero backend communication
 * Manages a single Retrofit client instance shared across all repositories
 */
internal object ApperoApiService {
    
    private const val BASE_URL = "https://app.appero.co.uk/"
    private const val TIMEOUT = 30L
    
    private val retrofit: Retrofit by lazy {
        // Create HTTP client with logging for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        // Create Retrofit instance
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Get the feedback API service
     */
    val feedbackApi: FeedbackApiService by lazy {
        retrofit.create(FeedbackApiService::class.java)
    }
    
    // Future API services can be added here:
    // val userApi: UserApiService by lazy { retrofit.create(UserApiService::class.java) }
    // val analyticsApi: AnalyticsApiService by lazy { retrofit.create(AnalyticsApiService::class.java) }
} 