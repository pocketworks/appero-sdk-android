package com.example.appero_sdk_android

import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.util.*

/**
 * Unit tests for ExperienceTracker
 */
class ExperienceTrackerTest {

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var experienceTracker: ExperienceTracker
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Mock editor behavior
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.putInt(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.putBoolean(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.remove(any())).thenReturn(mockEditor)
        
        // Mock user ID generation and retrieval
        val testUserId = "test-user-123"
        whenever(mockSharedPreferences.getString("appero_user_id", null)).thenReturn(testUserId)
        
        // Mock user-specific keys with the test user ID
        val userPrefix = "appero_user_${testUserId}_"
        whenever(mockSharedPreferences.getInt("${userPrefix}experience_points", 0)).thenReturn(0)
        whenever(mockSharedPreferences.getInt("${userPrefix}rating_threshold", 5)).thenReturn(5)
        whenever(mockSharedPreferences.getBoolean("${userPrefix}has_submitted_feedback", false)).thenReturn(false)
        
        experienceTracker = ExperienceTracker(mockSharedPreferences)
    }
    
    @Test
    fun testLogExperienceEnum() {
        // Test logging with Experience enum
        experienceTracker.log(Experience.VERY_POSITIVE)
        
        // Verify that experience points were updated (0 + 2 = 2)
        verify(mockEditor).putInt(contains("experience_points"), eq(2))
        verify(mockEditor).apply()
    }
    
    @Test
    fun testLogExperiencePoints() {
        // Test logging with custom points
        experienceTracker.log(3)
        
        // Verify that experience points were updated (0 + 3 = 3)
        verify(mockEditor).putInt(contains("experience_points"), eq(3))
        verify(mockEditor).apply()
    }
    
    @Test
    fun testLogNegativeExperience() {
        // Set initial points to 5
        val testUserId = "test-user-123"
        val userPrefix = "appero_user_${testUserId}_"
        whenever(mockSharedPreferences.getInt("${userPrefix}experience_points", 0)).thenReturn(5)
        
        // Log negative experience
        experienceTracker.log(Experience.VERY_NEGATIVE)
        
        // Verify that experience points were updated (5 - 2 = 3)
        verify(mockEditor).putInt(contains("experience_points"), eq(3))
        verify(mockEditor).apply()
    }
    
    @Test
    fun testExperiencePointsCannotGoBelowZero() {
        // Set initial points to 1
        val testUserId = "test-user-123"
        val userPrefix = "appero_user_${testUserId}_"
        whenever(mockSharedPreferences.getInt("${userPrefix}experience_points", 0)).thenReturn(1)
        
        // Log very negative experience (-2 points)
        experienceTracker.log(Experience.VERY_NEGATIVE)
        
        // Verify that experience points cannot go below 0 (1 - 2 = 0, not -1)
        verify(mockEditor).putInt(contains("experience_points"), eq(0))
        verify(mockEditor).apply()
    }
    
    @Test
    fun testShouldShowApperoWhenThresholdMet() {
        // Set experience points to meet threshold (5)
        val testUserId = "test-user-123"
        val userPrefix = "appero_user_${testUserId}_"
        whenever(mockSharedPreferences.getInt("${userPrefix}experience_points", 0)).thenReturn(5)
        whenever(mockSharedPreferences.getInt("${userPrefix}rating_threshold", 5)).thenReturn(5)
        whenever(mockSharedPreferences.getBoolean("${userPrefix}has_submitted_feedback", false)).thenReturn(false)
        
        assertTrue("Should show Appero when threshold is met and no feedback submitted", 
                  experienceTracker.shouldShowAppero())
    }
    
    @Test
    fun testShouldNotShowApperoWhenThresholdNotMet() {
        // Set experience points below threshold
        val testUserId = "test-user-123"
        val userPrefix = "appero_user_${testUserId}_"
        whenever(mockSharedPreferences.getInt("${userPrefix}experience_points", 0)).thenReturn(3)
        whenever(mockSharedPreferences.getInt("${userPrefix}rating_threshold", 5)).thenReturn(5)
        whenever(mockSharedPreferences.getBoolean("${userPrefix}has_submitted_feedback", false)).thenReturn(false)
        
        assertFalse("Should not show Appero when threshold is not met", 
                   experienceTracker.shouldShowAppero())
    }
    
    @Test
    fun testShouldNotShowApperoWhenFeedbackAlreadySubmitted() {
        // Set experience points above threshold but feedback already submitted
        val testUserId = "test-user-123"
        val userPrefix = "appero_user_${testUserId}_"
        whenever(mockSharedPreferences.getInt("${userPrefix}experience_points", 0)).thenReturn(10)
        whenever(mockSharedPreferences.getInt("${userPrefix}rating_threshold", 5)).thenReturn(5)
        whenever(mockSharedPreferences.getBoolean("${userPrefix}has_submitted_feedback", false)).thenReturn(true)
        
        assertFalse("Should not show Appero when feedback already submitted", 
                   experienceTracker.shouldShowAppero())
    }
    
    @Test
    fun testSetRatingThreshold() {
        // Test setting a valid rating threshold
        experienceTracker.ratingThreshold = 10
        
        verify(mockEditor).putInt(contains("rating_threshold"), eq(10))
        verify(mockEditor).apply()
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testSetRatingThresholdWithZeroValue() {
        // Test setting rating threshold to zero should throw exception
        experienceTracker.ratingThreshold = 0
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testSetRatingThresholdWithNegativeValue() {
        // Test setting rating threshold to negative value should throw exception
        experienceTracker.ratingThreshold = -1
    }
    
    @Test
    fun testMarkFeedbackSubmitted() {
        // Test marking feedback as submitted
        experienceTracker.markFeedbackSubmitted()
        
        verify(mockEditor).putBoolean(contains("has_submitted_feedback"), eq(true))
        verify(mockEditor).apply()
    }
    
    @Test
    fun testResetExperienceAndPrompt() {
        // Test resetting experience and prompt
        experienceTracker.resetExperienceAndPrompt()
        
        verify(mockEditor).putInt(contains("experience_points"), eq(0))
        verify(mockEditor).putBoolean(contains("has_submitted_feedback"), eq(false))
        verify(mockEditor).apply()
    }
    
    @Test
    fun testGetExperienceState() {
        // Set up mock values
        val testUserId = "test-user-123"
        val userPrefix = "appero_user_${testUserId}_"
        whenever(mockSharedPreferences.getInt("${userPrefix}experience_points", 0)).thenReturn(7)
        whenever(mockSharedPreferences.getInt("${userPrefix}rating_threshold", 5)).thenReturn(5)
        whenever(mockSharedPreferences.getBoolean("${userPrefix}has_submitted_feedback", false)).thenReturn(false)
        
        val state = experienceTracker.getExperienceState()
        
        assertNotNull("Experience state should not be null", state)
        assertEquals("Experience points should match", 7, state.experiencePoints)
        assertEquals("Rating threshold should match", 5, state.ratingThreshold)
        assertEquals("User ID should match", testUserId, state.userId)
        assertTrue("Should show prompt when threshold met and no feedback", state.shouldShowPrompt)
        assertFalse("Has submitted feedback should be false", state.hasSubmittedFeedback)
    }
    
    @Test
    fun testSetUser() {
        // Test setting a specific user ID
        val newUserId = "new-user-456"
        experienceTracker.setUser(newUserId)
        
        verify(mockEditor).putString("appero_user_id", newUserId)
        verify(mockEditor).apply()
    }
    
    @Test
    fun testResetUser() {
        // Test resetting user (generates new UUID)
        experienceTracker.resetUser()
        
        // Verify that a new user ID was stored (we can't predict the UUID, but we can verify the call)
        verify(mockEditor).putString(eq("appero_user_id"), any())
        verify(mockEditor).apply()
    }
    
    @Test
    fun testGetCurrentUserId() {
        // Test getting current user ID
        val userId = experienceTracker.getCurrentUserId()
        
        assertEquals("Should return the test user ID", "test-user-123", userId)
    }
} 