package com.appero.sdk.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.appero_sdk_android.R

/**
 * Experience levels using 5-point Likert scale for user experience tracking
 * Each level represents user satisfaction and is sent directly to the API
 */
@Suppress("detekt:MagicNumber")
enum class Experience(val rating: Int, @StringRes val description: Int, @DrawableRes val icon: Int) {
    /**
     * Very Negative (1) - e.g., "500 error", critical bugs, system failures
     */
    VERY_NEGATIVE(1, R.string.appero_accessibility_rating_1, R.drawable.ic_rating_very_negative),

    /**
     * Negative (2) - e.g., slow performance, confusing UI
     */
    NEGATIVE(2, R.string.appero_accessibility_rating_2, R.drawable.ic_rating_negative),

    /**
     * Neutral (3) - e.g., average experience, neither good nor bad
     */
    NEUTRAL(3, R.string.appero_accessibility_rating_3, R.drawable.ic_rating_neutral),

    /**
     * Positive (4) - e.g., smooth flow, good performance
     */
    POSITIVE(4, R.string.appero_accessibility_rating_4, R.drawable.ic_rating_positive),

    /**
     * Very Positive (5) - e.g., "user started subscription", excellent experience
     */
    VERY_POSITIVE(5, R.string.appero_accessibility_rating_5, R.drawable.ic_rating_very_positive)
}
