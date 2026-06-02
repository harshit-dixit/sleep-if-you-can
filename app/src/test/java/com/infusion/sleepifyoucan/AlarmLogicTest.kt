package com.infusion.sleepifyoucan

import com.infusion.sleepifyoucan.data.Alarm
import com.infusion.sleepifyoucan.data.AlarmScheduleCalculator
import com.infusion.sleepifyoucan.data.Converters
import com.infusion.sleepifyoucan.data.MissionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AlarmLogicTest {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun oneTimeAlarmInPastSchedulesTomorrow() {
        val now = millis(2026, Calendar.JUNE, 2, 8, 30)
        val alarm = Alarm(hour = 7, minute = 15)

        val trigger = AlarmScheduleCalculator.nextTriggerTimeMillis(alarm, now, utc)

        assertEquals(millis(2026, Calendar.JUNE, 3, 7, 15), trigger)
    }

    @Test
    fun repeatingAlarmTodayInFutureSchedulesToday() {
        val now = millis(2026, Calendar.JUNE, 2, 8, 30) // Tuesday
        val alarm = Alarm(
            hour = 9,
            minute = 0,
            daysOfWeek = listOf(Calendar.TUESDAY, Calendar.THURSDAY)
        )

        val trigger = AlarmScheduleCalculator.nextTriggerTimeMillis(alarm, now, utc)

        assertEquals(millis(2026, Calendar.JUNE, 2, 9, 0), trigger)
    }

    @Test
    fun repeatingAlarmTodayInPastSchedulesNextMatchingDay() {
        val now = millis(2026, Calendar.JUNE, 2, 8, 30) // Tuesday
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            daysOfWeek = listOf(Calendar.TUESDAY, Calendar.THURSDAY)
        )

        val trigger = AlarmScheduleCalculator.nextTriggerTimeMillis(alarm, now, utc)

        assertEquals(millis(2026, Calendar.JUNE, 4, 7, 0), trigger)
    }

    @Test
    fun removedMissionConfigsFallbackToShake() {
        val config = Converters().toMissionConfig("""{"type":"PHOTO","photoRequiredObject":"Laptop"}""")

        assertEquals(MissionConfig.Shake(20), config)
    }

    @Test
    fun blankBarcodeConfigFallbacksToShake() {
        val config = Converters().toMissionConfig("""{"type":"BARCODE","barcodeExpected":""}""")

        assertEquals(MissionConfig.Shake(20), config)
    }

    @Test
    fun registeredBarcodeConfigSurvivesRoundTrip() {
        val config = Converters().toMissionConfig(
            Converters().fromMissionConfig(MissionConfig.Barcode("9781234567890"))
        )

        assertTrue(config is MissionConfig.Barcode)
        assertEquals("9781234567890", (config as MissionConfig.Barcode).expectedBarcode)
    }

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long {
        return Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
    }
}
