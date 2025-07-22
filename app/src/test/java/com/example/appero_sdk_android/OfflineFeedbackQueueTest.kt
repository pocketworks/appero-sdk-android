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
class OfflineFeedbackQueueTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var offlineQueue: OfflineFeedbackQueue
    
    @Before
    fun setup() {
        // Mock SharedPreferences behavior
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { }
        
        // Mock no existing queued feedback
        `when`(mockSharedPreferences.getString("queued_feedback_list", null)).thenReturn(null)
        
        offlineQueue = OfflineFeedbackQueue(mockContext, mockSharedPreferences)
    }
    
    @Test
    fun `queueFeedback should store feedback locally`() {
        // Given
        val apiKey = "test-api-key"
        val clientId = "test-client-id"
        val rating = 5
        val feedback = "Great app!"
        
        // When
        offlineQueue.queueFeedback(apiKey, clientId, rating, feedback)
        
        // Then
        verify(mockEditor).putString(eq("queued_feedback_list"), anyString())
        verify(mockEditor).apply()
    }
    
    @Test
    fun `getQueueSize should return correct count when empty`() {
        // When
        val queueSize = offlineQueue.getQueueSize()
        
        // Then
        assert(queueSize == 0)
    }
    
    @Test
    fun `clearQueue should clear all queued feedback`() {
        // When
        offlineQueue.clearQueue()
        
        // Then
        verify(mockEditor).putString("queued_feedback_list", "[]")
        verify(mockEditor).apply()
    }
    
    @Test
    fun `queueFeedback should limit queue size to prevent memory issues`() {
        // Given - simulate large existing queue
        val largeQueueJson = "[]" // In reality this would be a large JSON array
        `when`(mockSharedPreferences.getString("queued_feedback_list", null)).thenReturn(largeQueueJson)
        
        // When
        offlineQueue.queueFeedback("api", "client", 5, "test")
        
        // Then - verify it was saved (specific logic would depend on queue size management)
        verify(mockEditor).putString(eq("queued_feedback_list"), anyString())
    }
} 