package com.appero.sdk.util

import android.content.Context
import android.content.pm.PackageManager

/**
 * Utility class for getting app version and build information
 */
internal object AppVersionUtils {
    
    /**
     * Get the source platform identifier
     * @return "android" for Android platform
     */
    fun getSource(): String = "android"
    
    /**
     * Get the build version in format "versionName.versionCode"
     * @param context Application context
     * @return Build version string (e.g., "1.0.1")
     */
    fun getBuildVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "unknown"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "$versionName.$versionCode"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown.0"
        }
    }
} 