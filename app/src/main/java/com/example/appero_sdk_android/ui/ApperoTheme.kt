package com.example.appero_sdk_android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ApperoTheme interface - matches iOS ApperoTheme protocol
 * Allows customization of Appero feedback UI colors
 */
interface ApperoTheme {
    val primaryColor: Color
    val backgroundColor: Color
    val surfaceColor: Color
    val textColor: Color
    val secondaryTextColor: Color
    val accentColor: Color
    val errorColor: Color
    val successColor: Color
    
    // Emoji rating colors (5 emotions: 😢😕😐😊😍)
    val veryNegativeColor: Color    // Red for 😢
    val negativeColor: Color        // Orange for 😕  
    val neutralColor: Color         // Yellow/Gray for 😐
    val positiveColor: Color        // Light Blue for 😊
    val veryPositiveColor: Color    // Green for 😍
    
    // UI element colors
    val buttonBackgroundColor: Color
    val buttonTextColor: Color
    val textFieldBackgroundColor: Color
    val dividerColor: Color
}

/**
 * Default theme - uses iOS system colors and responds to light/dark mode
 * Matches iOS DefaultTheme() behavior
 */
class DefaultTheme : ApperoTheme {
    override val primaryColor = Color(0xFF007AFF)
    override val backgroundColor get() = Color.Transparent // Will use system background
    override val surfaceColor get() = Color.Transparent // Will use system surface  
    override val textColor get() = Color.Unspecified // Will use system text colors
    override val secondaryTextColor = Color(0xFF8E8E93)
    override val accentColor = Color(0xFF007AFF)
    override val errorColor = Color(0xFFFF3B30)
    override val successColor = Color(0xFF34C759)
    
    // Emoji rating colors (iOS-style) - these work well in both light/dark
    override val veryNegativeColor = Color(0xFFFF3B30)    // Red
    override val negativeColor = Color(0xFFFF9500)        // Orange  
    override val neutralColor = Color(0xFFFFCC00)         // Yellow
    override val positiveColor = Color(0xFF007AFF)        // Blue
    override val veryPositiveColor = Color(0xFF34C759)    // Green
    
    // UI elements
    override val buttonBackgroundColor = Color(0xFF007AFF)
    override val buttonTextColor = Color(0xFFFFFFFF)
    override val textFieldBackgroundColor get() = Color.Transparent // Will use system
    override val dividerColor = Color(0xFFC6C6C8)
}

/**
 * Light theme - fixed light colors that don't change with system appearance
 * Matches iOS LightTheme() behavior
 */
class LightTheme : ApperoTheme {
    override val primaryColor = Color(0xFF007AFF)
    override val backgroundColor = Color(0xFFFFFFFF)
    override val surfaceColor = Color(0xFFF2F2F7)
    override val textColor = Color(0xFF000000)
    override val secondaryTextColor = Color(0xFF3C3C43).copy(alpha = 0.6f)
    override val accentColor = Color(0xFF007AFF)
    override val errorColor = Color(0xFFFF3B30)
    override val successColor = Color(0xFF34C759)
    
    // Fixed light emoji colors
    override val veryNegativeColor = Color(0xFFFF3B30)    // Red
    override val negativeColor = Color(0xFFFF9500)        // Orange
    override val neutralColor = Color(0xFFFFCC00)         // Yellow
    override val positiveColor = Color(0xFF007AFF)        // Blue
    override val veryPositiveColor = Color(0xFF34C759)    // Green
    
    // UI elements
    override val buttonBackgroundColor = Color(0xFF007AFF)
    override val buttonTextColor = Color(0xFFFFFFFF)
    override val textFieldBackgroundColor = Color(0xFFF2F2F7)
    override val dividerColor = Color(0xFFC6C6C8)
}

/**
 * Dark theme - fixed dark colors that don't change with system appearance
 * Matches iOS DarkTheme() behavior  
 */
class DarkTheme : ApperoTheme {
    override val primaryColor = Color(0xFF007AFF)
    override val backgroundColor = Color(0xFF000000)
    override val surfaceColor = Color(0xFF1C1C1E)
    override val textColor = Color(0xFFFFFFFF)
    override val secondaryTextColor = Color(0xFF8E8E93)
    override val accentColor = Color(0xFF007AFF)
    override val errorColor = Color(0xFFFF453A)
    override val successColor = Color(0xFF32D74B)
    
    // Fixed dark emoji colors
    override val veryNegativeColor = Color(0xFFFF453A)    // Red
    override val negativeColor = Color(0xFFFF9F0A)        // Orange
    override val neutralColor = Color(0xFFFFD60A)         // Yellow
    override val positiveColor = Color(0xFF007AFF)        // Blue
    override val veryPositiveColor = Color(0xFF32D74B)    // Green
    
    // UI elements
    override val buttonBackgroundColor = Color(0xFF007AFF)
    override val buttonTextColor = Color(0xFFFFFFFF)
    override val textFieldBackgroundColor = Color(0xFF2C2C2E)
    override val dividerColor = Color(0xFF38383A)
}

/**
 * Custom theme builder for developers who want to provide their own colors
 * Matches iOS custom theme capability
 */
class CustomTheme(
    override val primaryColor: Color = Color(0xFF007AFF),
    override val backgroundColor: Color = Color(0xFFFFFFFF),
    override val surfaceColor: Color = Color(0xFFF2F2F7),
    override val textColor: Color = Color(0xFF000000),
    override val secondaryTextColor: Color = Color(0xFF3C3C43).copy(alpha = 0.6f),
    override val accentColor: Color = Color(0xFF007AFF),
    override val errorColor: Color = Color(0xFFFF3B30),
    override val successColor: Color = Color(0xFF34C759),
    
    // Custom emoji rating colors
    override val veryNegativeColor: Color = Color(0xFFFF3B30),
    override val negativeColor: Color = Color(0xFFFF9500),
    override val neutralColor: Color = Color(0xFFFFCC00),
    override val positiveColor: Color = Color(0xFF007AFF),
    override val veryPositiveColor: Color = Color(0xFF34C759),
    
    // Custom UI element colors
    override val buttonBackgroundColor: Color = Color(0xFF007AFF),
    override val buttonTextColor: Color = Color(0xFFFFFFFF),
    override val textFieldBackgroundColor: Color = Color(0xFFF2F2F7),
    override val dividerColor: Color = Color(0xFFC6C6C8)
) : ApperoTheme

/**
 * Helper function to create Material 3 ColorScheme from ApperoTheme
 * For integration with existing Material Design 3 components
 */
@Composable
fun ApperoTheme.toMaterial3ColorScheme(): ColorScheme {
    return if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = primaryColor,
            background = backgroundColor,
            surface = surfaceColor,
            onPrimary = buttonTextColor,
            onBackground = textColor,
            onSurface = textColor,
            secondary = accentColor,
            error = errorColor
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            background = backgroundColor,
            surface = surfaceColor,
            onPrimary = buttonTextColor,
            onBackground = textColor,
            onSurface = textColor,
            secondary = accentColor,
            error = errorColor
        )
    }
}
