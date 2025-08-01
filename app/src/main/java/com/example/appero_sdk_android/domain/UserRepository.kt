package com.example.appero_sdk_android.domain

import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.UUID

/**
 * Manages user sessions and user ID persistence for the Appero SDK
 * Each user has their own experience tracking and feedback submission status
 */
internal class UserRepository(private val sharedPreferences: SharedPreferences) {

    companion object {
        private const val KEY_USER_ID = "appero_user_id"
        private const val KEY_USER_PREFIX = "appero_user_"

        // Per-user keys (appended with user ID)
        private const val USER_KEY_EXPERIENCE_POINTS = "experience_points"
        private const val USER_KEY_RATING_THRESHOLD = "rating_threshold"
        private const val USER_KEY_HAS_SUBMITTED_FEEDBACK = "has_submitted_feedback"
    }

    private var currentUserId: String? = null

    init {
        // Restore current user ID on initialization
        currentUserId = sharedPreferences.getString(KEY_USER_ID, null)

        // Generate a new user ID if none exists
        if (currentUserId == null) {
            currentUserId = generateUserId()
            saveCurrentUserId()
        }
    }

    /**
     * Get the current user ID
     * @return Current user ID (auto-generated UUID if none was set)
     */
    fun getCurrentUserId(): String {
        return currentUserId ?: run {
            val newUserId = generateUserId()
            setUser(newUserId)
            newUserId
        }
    }

    /**
     * Set a specific user ID (for account-based systems)
     * This will switch the active user and load their experience data
     *
     * @param userId The user ID to set as active
     */
    fun setUser(userId: String) {
        require(userId.isNotBlank()) { "User ID cannot be blank" }

        currentUserId = userId
        saveCurrentUserId()
    }

    /**
     * Reset the current user (clear user data and generate new ID)
     * Use this when a user logs out
     */
    fun resetUser() {
        currentUserId = generateUserId()
        saveCurrentUserId()
    }

    /**
     * Get experience points for the current user
     */
    fun getExperiencePoints(): Int {
        val userId = getCurrentUserId()
        return sharedPreferences.getInt(getUserKey(userId, USER_KEY_EXPERIENCE_POINTS), 0)
    }

    /**
     * Set experience points for the current user
     */
    fun setExperiencePoints(points: Int) {
        val userId = getCurrentUserId()
        sharedPreferences.edit {
            putInt(getUserKey(userId, USER_KEY_EXPERIENCE_POINTS), maxOf(0, points))
        }
    }

    /**
     * Get rating threshold for the current user
     */
    fun getRatingThreshold(): Int {
        val userId = getCurrentUserId()
        return sharedPreferences.getInt(getUserKey(userId, USER_KEY_RATING_THRESHOLD), 5)
    }

    /**
     * Set rating threshold for the current user
     */
    fun setRatingThreshold(threshold: Int) {
        require(threshold > 0) { "Rating threshold must be positive" }
        val userId = getCurrentUserId()
        sharedPreferences.edit {
            putInt(getUserKey(userId, USER_KEY_RATING_THRESHOLD), threshold)
        }
    }

    /**
     * Check if the current user has submitted feedback
     */
    fun hasSubmittedFeedback(): Boolean {
        val userId = getCurrentUserId()
        return sharedPreferences.getBoolean(getUserKey(userId, USER_KEY_HAS_SUBMITTED_FEEDBACK), false)
    }

    /**
     * Mark that the current user has submitted feedback
     */
    fun markFeedbackSubmitted() {
        val userId = getCurrentUserId()
        sharedPreferences.edit {
            putBoolean(getUserKey(userId, USER_KEY_HAS_SUBMITTED_FEEDBACK), true)
        }
    }

    /**
     * Reset experience and feedback status for the current user
     */
    fun resetExperienceAndPrompt() {
        val userId = getCurrentUserId()
        sharedPreferences.edit {
            putInt(getUserKey(userId, USER_KEY_EXPERIENCE_POINTS), 0)
            putBoolean(getUserKey(userId, USER_KEY_HAS_SUBMITTED_FEEDBACK), false)
        }
    }

    /**
     * Clear all data for a specific user
     * @param userId The user ID to clear data for
     */
    fun clearUserData(userId: String) {
        sharedPreferences.edit {
            // Remove all user-specific keys
            remove(getUserKey(userId, USER_KEY_EXPERIENCE_POINTS))
            remove(getUserKey(userId, USER_KEY_RATING_THRESHOLD))
            remove(getUserKey(userId, USER_KEY_HAS_SUBMITTED_FEEDBACK))
        }
    }

    /**
     * Generate a unique user ID (UUID)
     */
    private fun generateUserId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Save the current user ID to SharedPreferences
     */
    private fun saveCurrentUserId() {
        sharedPreferences.edit {
            putString(KEY_USER_ID, currentUserId)
        }
    }

    /**
     * Generate a user-specific key for SharedPreferences
     * @param userId The user ID
     * @param key The base key
     * @return User-specific key in format "appero_user_{userId}_{key}"
     */
    private fun getUserKey(userId: String, key: String): String {
        return "${KEY_USER_PREFIX}${userId}_${key}"
    }
}