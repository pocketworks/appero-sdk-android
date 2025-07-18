package com.example.appero_sdk_android

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ApperoTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    @Before
    fun setup() {
        // Mock SharedPreferences behavior
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
    }
    
    @Test
    fun testSdkInitialization() {
        // Test SDK initialization
        val apiKey = "test-api-key"
        val clientId = "test-client-id"
        
        Appero.start(mockContext, apiKey, clientId)
        
        // Verify SDK is initialized
        assert(Appero.isInitialized())
        
        // Verify SharedPreferences interactions
        verify(mockEditor).putString("api_key", apiKey)
        verify(mockEditor).putString("client_id", clientId)
        verify(mockEditor).putBoolean("is_initialized", true)
        verify(mockEditor).apply()
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testSdkInitializationWithBlankApiKey() {
        // Test that blank API key throws exception
        Appero.start(mockContext, "", "test-client-id")
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testSdkInitializationWithBlankClientId() {
        // Test that blank client ID throws exception
        Appero.start(mockContext, "test-api-key", "")
    }
} 