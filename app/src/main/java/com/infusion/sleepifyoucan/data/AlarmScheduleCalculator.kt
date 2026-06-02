package com.infusion.sleepifyoucan.data

import java.util.Calendar
import java.util.TimeZone

object AlarmScheduleCalculator {
    fun nextTriggerTimeMillis(
        alarm: Alarm,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val now = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
        }

        if (alarm.daysOfWeek.isEmpty()) {
            return calendarForDayOffset(alarm, now, timeZone, 0).let { candidate ->
                if (candidate.timeInMillis <= now.timeInMillis) {
                    candidate.add(Calendar.DAY_OF_YEAR, 1)
                }
                candidate.timeInMillis
            }
        }

        for (dayOffset in 0..7) {
            val candidate = calendarForDayOffset(alarm, now, timeZone, dayOffset)
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (alarm.daysOfWeek.contains(dayOfWeek) && candidate.timeInMillis > now.timeInMillis) {
                return candidate.timeInMillis
            }
        }

        return calendarForDayOffset(alarm, now, timeZone, 1).timeInMillis
    }

    private fun calendarForDayOffset(
        alarm: Alarm,
        now: Calendar,
        timeZone: TimeZone,
        dayOffset: Int
    ): Calendar {
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
