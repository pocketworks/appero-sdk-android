package com.example.apperoSdkAndroid

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ApperoAnalyticsListenerTest {
    @Mock
    private lateinit var mockAnalyticsListener: ApperoAnalyticsListener
    
    @Test
    fun testAnalyticsListenerCanBeSet() {
        // Test that the method can be called without throwing exceptions
        // Note: This will work even if SDK is not initialized
        Appero.setAnalyticsListener(mockAnalyticsListener)
    }
    
    @Test
    fun testAnalyticsListenerCanBeNull() {
        // Test that null can be passed without throwing exceptions
        Appero.setAnalyticsListener(null)
    }
    
    @Test
    fun testAnalyticsListenerInterface() {
        val testListener = TestAnalyticsListener()
        testListener.onRatingSelected(4)
        testListener.onApperoFeedbackSubmitted(4, "Great app!")
        assert(testListener.ratingCalls.contains(4))
        assert(testListener.feedbackCalls.contains(Pair(4, "Great app!")))
    }
    
    private class TestAnalyticsListener : ApperoAnalyticsListener {
        var feedbackCalls = mutableListOf<Pair<Int, String>>()
        var ratingCalls = mutableListOf<Int>()
        
        override fun onApperoFeedbackSubmitted(rating: Int, feedback: String) {
            feedbackCalls.add(Pair(rating, feedback))
        }
        
        override fun onRatingSelected(rating: Int) {
            ratingCalls.add(rating)
        }
        
        fun reset() {
            feedbackCalls.clear()
            ratingCalls.clear()
        }
    }
} 