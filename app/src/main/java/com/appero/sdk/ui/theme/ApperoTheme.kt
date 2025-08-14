@file:Suppress("detekt:MagicNumber","detekt:LongParameterList")

package com.appero.sdk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

interface ApperoTheme {
    val primaryColor: Color
    val backgroundColor: Color
    val surfaceColor: Color
    val textColor: Color
    val secondaryTextColor: Color
    val accentColor: Color
    val errorColor: Color
    val successColor: Color
    val veryNegativeColor: Color
    val negativeColor: Color
    val neutralColor: Color
    val positiveColor: Color
    val veryPositiveColor: Color
    val buttonBackgroundColor: Color
    val buttonTextColor: Color
    val textFieldBackgroundColor: Color
    val dividerColor: Color
}

class DefaultTheme : ApperoTheme {
    override val primaryColor = Color(0xFF007AFF)
    override val backgroundColor get() = Color.Transparent
    override val surfaceColor get() = Color.Transparent
    override val textColor get() = Color.Unspecified
    override val secondaryTextColor = Color(0xFF8E8E93)
    override val accentColor = Color(0xFF007AFF)
    override val errorColor = Color(0xFFFF3B30)
    override val successColor = Color(0xFF34C759)
    override val veryNegativeColor = Color(0xFFFF3B30)
    override val negativeColor = Color(0xFFFF9500)
    override val neutralColor = Color(0xFFFFCC00)
    override val positiveColor = Color(0xFF007AFF)
    override val veryPositiveColor = Color(0xFF34C759)
    override val buttonBackgroundColor = Color(0xFF007AFF)
    override val buttonTextColor = Color(0xFFFFFFFF)
    override val textFieldBackgroundColor get() = Color.Transparent
    override val dividerColor = Color(0xFFC6C6C8)
}

class LightTheme : ApperoTheme {
    override val primaryColor = Color(0xFF007AFF)
    override val backgroundColor = Color(0xFFFFFFFF)
    override val surfaceColor = Color(0xFFF2F2F7)
    override val textColor = Color(0xFF000000)
    override val secondaryTextColor = Color(0xFF3C3C43).copy(alpha = 0.6f)
    override val accentColor = Color(0xFF007AFF)
    override val errorColor = Color(0xFFFF3B30)
    override val successColor = Color(0xFF34C759)
    override val veryNegativeColor = Color(0xFFFF3B30)
    override val negativeColor = Color(0xFFFF9500)
    override val neutralColor = Color(0xFFFFCC00)
    override val positiveColor = Color(0xFF007AFF)
    override val veryPositiveColor = Color(0xFF34C759)
    override val buttonBackgroundColor = Color(0xFF007AFF)
    override val buttonTextColor = Color(0xFFFFFFFF)
    override val textFieldBackgroundColor = Color(0xFFF2F2F7)
    override val dividerColor = Color(0xFFC6C6C8)
}

class DarkTheme : ApperoTheme {
    override val primaryColor = Color(0xFF007AFF)
    override val backgroundColor = Color(0xFF000000)
    override val surfaceColor = Color(0xFF1C1C1E)
    override val textColor = Color(0xFFFFFFFF)
    override val secondaryTextColor = Color(0xFF8E8E93)
    override val accentColor = Color(0xFF007AFF)
    override val errorColor = Color(0xFFFF453A)
    override val successColor = Color(0xFF32D74B)
    override val veryNegativeColor = Color(0xFFFF453A)
    override val negativeColor = Color(0xFFFF9F0A)
    override val neutralColor = Color(0xFFFFD60A)
    override val positiveColor = Color(0xFF007AFF)
    override val veryPositiveColor = Color(0xFF32D74B)
    override val buttonBackgroundColor = Color(0xFF007AFF)
    override val buttonTextColor = Color(0xFFFFFFFF)
    override val textFieldBackgroundColor = Color(0xFF2C2C2E)
    override val dividerColor = Color(0xFF38383A)
}

class CustomTheme(
    override val primaryColor: Color = Color(0xFF007AFF),
    override val backgroundColor: Color = Color(0xFFFFFFFF),
    override val surfaceColor: Color = Color(0xFFF2F2F7),
    override val textColor: Color = Color(0xFF000000),
    override val secondaryTextColor: Color = Color(0xFF3C3C43).copy(alpha = 0.6f),
    override val accentColor: Color = Color(0xFF007AFF),
    override val errorColor: Color = Color(0xFFFF3B30),
    override val successColor: Color = Color(0xFF34C759),
    override val veryNegativeColor: Color = Color(0xFFFF3B30),
    override val negativeColor: Color = Color(0xFFFF9500),
    override val neutralColor: Color = Color(0xFFFFCC00),
    override val positiveColor: Color = Color(0xFF007AFF),
    override val veryPositiveColor: Color = Color(0xFF34C759),
    override val buttonBackgroundColor: Color = Color(0xFF007AFF),
    override val buttonTextColor: Color = Color(0xFFFFFFFF),
    override val textFieldBackgroundColor: Color = Color(0xFFF2F2F7),
    override val dividerColor: Color = Color(0xFFC6C6C8)
) : ApperoTheme

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