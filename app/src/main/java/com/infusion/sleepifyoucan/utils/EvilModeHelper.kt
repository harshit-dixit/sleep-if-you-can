package com.infusion.sleepifyoucan.utils

import android.app.Activity
import android.os.Build

/**
 * Helper for "Evil Mode" - Lock Task mode that prevents users from leaving
 * the alarm activity until they complete the mission.
 */
object EvilModeHelper {
    
    /**
     * Check if Evil Mode (Lock Task) is available on this device.
     */
    fun isEvilModeAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }
    
    /**
     * Start Evil Mode - locks the task to prevent user from leaving.
     * Requires the app to be set as Device Owner or user to confirm screen pinning.
     */
    fun startEvilMode(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                activity.startLockTask()
            } catch (e: Exception) {
                // Lock task not available - user hasn't enabled screen pinning
                // or app is not device owner
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Stop Evil Mode - releases the task lock.
     */
    fun stopEvilMode(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                activity.stopLockTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get the explanation text to show users when they enable Evil Mode.
     */
    fun getPermissionExplanation(): String {
        return """
EVIL MODE

This mode makes it IMPOSSIBLE to escape the alarm until you complete the mission.

What happens:
• Your phone will be locked to this app
• You cannot switch apps, go home, or access notifications
• The only way out is to complete the mission

Perfect for heavy sleepers who snooze through everything!

Note: You can always disable this in settings when your phone is unlocked.
        """.trimIndent()
    }
}
