package com.example.apperoSdkAndroid.data

/**
 * Response model for experience submission
 * Matches the actual API response format from /api/v1/experiences
 */
internal data class ExperienceResponse(
    val status: String,
    val message: String? = null,
    val error: String? = null,
    val client_id: String? = null,
    val should_show_feedback: Boolean = false,
    val flow_type: String? = null,
    val feedback_ui: FeedbackUI? = null
)

/**
 * Feedback UI configuration returned by the experience endpoint
 */
internal data class FeedbackUI(
    val title: String? = null,
    val subtitle: String? = null,
    val prompt: String? = null
) 