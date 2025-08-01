package com.example.appero_sdk_android.domain

import android.content.SharedPreferences
import androidx.core.content.edit

internal class ClientRepository(
    private val sharedPreferences: SharedPreferences,
) {
    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_IS_INITIALIZED = "is_initialized"
    }

    fun putApiKey(apiKey: String) {
        sharedPreferences.edit { putString(KEY_API_KEY, apiKey) }
    }

    fun putClientId(clientId: String) {
        sharedPreferences.edit { putString(KEY_CLIENT_ID, clientId) }
    }

    fun putIsApperoInitialized(isApperoInitialized: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_IS_INITIALIZED, isApperoInitialized) }
    }

    fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_API_KEY, null)
    }

    fun getClientId(): String? {
        return sharedPreferences.getString(KEY_CLIENT_ID, null)

    }

    fun getIsApperoInitialized(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_INITIALIZED, false)
    }

    // Method to clear all Appero related preferences if needed
    fun clearAll() {
        sharedPreferences.edit {
            remove(KEY_API_KEY)
                .remove(KEY_CLIENT_ID)
                .remove(KEY_IS_INITIALIZED)
        }
    }
}