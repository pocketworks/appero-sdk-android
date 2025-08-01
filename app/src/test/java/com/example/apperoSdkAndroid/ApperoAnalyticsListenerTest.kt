package com.example.apperoSdkAndroid

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
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
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    @Mock
    private lateinit var mockAnalyticsListener: ApperoAnalyticsListener
    @Before
    fun setup() {
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        Appero.start(mockContext, "test-api-key", "test-client-id")
    }
    @Test
    fun testAnalyticsListenerCanBeSet() {
        Appero.setAnalyticsListener(mockAnalyticsListener)
        assert(Appero.setAnalyticsListener != null)
    }
    @Test
    fun testAnalyticsListenerCanBeNull() {
        Appero.setAnalyticsListener(null)
        // No exception thrown
    }
    @Test
    fun testAnalyticsListenerNullSafety() {
        Appero.setAnalyticsListener(null)
        val threshold = Appero.ratingThreshold
        assert(threshold >= 0)
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
    @Test
    fun testAnalyticsListenerInterface() {
        val testListener = TestAnalyticsListener()
        testListener.onRatingSelected(4)
        testListener.onApperoFeedbackSubmitted(4, "Great app!")
        assert(testListener.ratingCalls.contains(4))
        assert(testListener.feedbackCalls.contains(Pair(4, "Great app!")))
    }
} 