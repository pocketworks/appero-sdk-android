package com.example.appero_sdk_android

import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ExperienceTrackerTest {
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var experienceTracker: ExperienceTracker
    
    @Before
    fun setup() {
        // Mock SharedPreferences behavior
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        
        // Default values
        `when`(mockSharedPreferences.getInt("experience_points", 0)).thenReturn(0)
        `when`(mockSharedPreferences.getInt("rating_threshold", 5)).thenReturn(5)
        `when`(mockSharedPreferences.getBoolean("has_submitted_feedback", false)).thenReturn(false)
        
        experienceTracker = ExperienceTracker(mockSharedPreferences)
    }
    
    @Test
    fun testLogExperienceEnum() {
        // Test logging with Experience enum
        experienceTracker.log(Experience.VERY_POSITIVE)
        
        verify(mockEditor).putInt("experience_points", 2)
        verify(mockEditor).apply()
    }
    
    @Test
    fun testLogExperiencePoints() {
        // Test logging with custom points
        experienceTracker.log(3)
        
        verify(mockEditor).putInt("experience_points", 3)
        verify(mockEditor).apply()
    }
    
    @Test
    fun testLogNegativeExperience() {
        // Set current points to 3 (so 3 + (-2) = 1, which is valid)
        `when`(mockSharedPreferences.getInt("experience_points", 0)).thenReturn(3)
        experienceTracker = ExperienceTracker(mockSharedPreferences)
        
        // Log negative experience
        experienceTracker.log(Experience.VERY_NEGATIVE)
        
        verify(mockEditor).putInt("experience_points", 1)
        verify(mockEditor).apply()
    }
    
    @Test
    fun testExperiencePointsCannotGoBelowZero() {
        // Set current points to 0 initially
        `when`(mockSharedPreferences.getInt("experience_points", 0)).thenReturn(0)
        experienceTracker = ExperienceTracker(mockSharedPreferences)
        
        // Log very negative experience (-2 points) when starting from 0
        experienceTracker.log(Experience.VERY_NEGATIVE)
        
        // Should calculate 0 + (-2) = -2, but then set to 0 to prevent negative
        verify(mockEditor).putInt("experience_points", 0)
        verify(mockEditor).apply()
    }
    
    @Test
    fun testShouldShowApperoWhenThresholdMet() {
        // Set points above threshold and no feedback submitted
        `when`(mockSharedPreferences.getInt("experience_points", 0)).thenReturn(6)
        `when`(mockSharedPreferences.getInt("rating_threshold", 5)).thenReturn(5)
        `when`(mockSharedPreferences.getBoolean("has_submitted_feedback", false)).thenReturn(false)
        
        experienceTracker = ExperienceTracker(mockSharedPreferences)
        
        assert(experienceTracker.shouldShowAppero())
    }
    
    @Test
    fun testShouldNotShowApperoWhenThresholdNotMet() {
        // Set points below threshold
        `when`(mockSharedPreferences.getInt("experience_points", 0)).thenReturn(3)
        `when`(mockSharedPreferences.getInt("rating_threshold", 5)).thenReturn(5)
        
        experienceTracker = ExperienceTracker(mockSharedPreferences)
        
        assert(!experienceTracker.shouldShowAppero())
    }
    
    @Test
    fun testShouldNotShowApperoWhenFeedbackAlreadySubmitted() {
        // Set points above threshold but feedback already submitted
        `when`(mockSharedPreferences.getInt("experience_points", 0)).thenReturn(6)
        `when`(mockSharedPreferences.getInt("rating_threshold", 5)).thenReturn(5)
        `when`(mockSharedPreferences.getBoolean("has_submitted_feedback", false)).thenReturn(true)
        
        experienceTracker = ExperienceTracker(mockSharedPreferences)
        
        assert(!experienceTracker.shouldShowAppero())
    }
    
    @Test
    fun testMarkFeedbackSubmitted() {
        experienceTracker.markFeedbackSubmitted()
        
        verify(mockEditor).putBoolean("has_submitted_feedback", true)
        verify(mockEditor).apply()
    }
    
    @Test
    fun testResetExperienceAndPrompt() {
        experienceTracker.resetExperienceAndPrompt()
        
        verify(mockEditor).putInt("experience_points", 0)
        verify(mockEditor).putBoolean("has_submitted_feedback", false)
        verify(mockEditor, times(2)).apply()
    }
    
    @Test
    fun testSetRatingThreshold() {
        experienceTracker.ratingThreshold = 10
        
        verify(mockEditor).putInt("rating_threshold", 10)
        verify(mockEditor).apply()
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testSetRatingThresholdWithNegativeValue() {
        experienceTracker.ratingThreshold = -1
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testSetRatingThresholdWithZeroValue() {
        experienceTracker.ratingThreshold = 0
    }
    
    @Test
    fun testGetExperienceState() {
        // Set up mock values
        `when`(mockSharedPreferences.getInt("experience_points", 0)).thenReturn(7)
        `when`(mockSharedPreferences.getInt("rating_threshold", 5)).thenReturn(5)
        `when`(mockSharedPreferences.getBoolean("has_submitted_feedback", false)).thenReturn(false)
        
        experienceTracker = ExperienceTracker(mockSharedPreferences)
        
        val state = experienceTracker.getExperienceState()
        
        assert(state.experiencePoints == 7)
        assert(state.ratingThreshold == 5)
        assert(!state.hasSubmittedFeedback)
        assert(state.shouldShowPrompt)
    }
    
    @Test
    fun testResetUser() {
        experienceTracker.resetUser()
        
        verify(mockEditor).putInt("experience_points", 0)
        verify(mockEditor).putBoolean("has_submitted_feedback", false)
        verify(mockEditor, times(2)).apply()
    }
} 