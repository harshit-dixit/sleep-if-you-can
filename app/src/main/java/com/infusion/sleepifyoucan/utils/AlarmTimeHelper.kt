package com.infusion.sleepifyoucan.utils

import java.util.Calendar

/**
 * Computes a human-readable "time until alarm" string.
 *
 * Handles:
 * - One-time alarms (empty daysOfWeek): next occurrence today or tomorrow
 * - Repeating alarms: find next matching day of week
 * - Past-today rollover
 *
 * @param hour Alarm hour (0-23)
 * @param minute Alarm minute (0-59)
 * @param daysOfWeek List of Calendar.SUNDAY..Calendar.SATURDAY, or empty for one-time
 * @return e.g. "in 7h 23m", "in 23m", "in 1d 3h", "tomorrow"
 */
fun getTimeUntilAlarm(hour: Int, minute: Int, daysOfWeek: List<Int>): String {
    val now = Calendar.getInstance()
    val alarmTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (daysOfWeek.isEmpty()) {
        // One-time alarm: if time has passed today, it's tomorrow
        if (alarmTime.before(now) || alarmTime == now) {
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }
    } else {
        // Repeating alarm: find the next matching day
        var found = false
        for (i in 0..7) {
            val candidate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, i)
            }
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (daysOfWeek.contains(dayOfWeek)) {
                // If it's today but time has passed, skip to next occurrence
                if (i == 0 && candidate.before(now)) continue
                alarmTime.timeInMillis = candidate.timeInMillis
                found = true
                break
            }
        }
        if (!found) {
            // Fallback: shouldn't happen with valid daysOfWeek
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    val diffMillis = alarmTime.timeInMillis - now.timeInMillis
    if (diffMillis <= 0) return "now"

    val totalMinutes = diffMillis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours >= 24 -> {
            val days = hours / 24
            val remainingHours = hours % 24
            if (remainingHours > 0) "in ${days}d ${remainingHours}h"
            else "in ${days}d"
        }
        hours > 0 -> "in ${hours}h ${minutes}m"
        minutes > 0 -> "in ${minutes}m"
        else -> "now"
    }
}
