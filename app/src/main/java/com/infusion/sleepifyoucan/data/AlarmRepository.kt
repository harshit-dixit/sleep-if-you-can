package com.infusion.sleepifyoucan.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler
) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun insert(alarm: Alarm) {
        val id = alarmDao.insertAlarm(alarm).toInt()
        val savedAlarm = alarm.copy(id = id) // ID was 0, now it's real
        if (savedAlarm.isEnabled) {
            alarmScheduler.schedule(savedAlarm)
        }
    }

    suspend fun update(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
        // Always cancel previous instance to avoid ghosts
        alarmScheduler.cancel(alarm)
        
        if (alarm.isEnabled) {
            alarmScheduler.schedule(alarm)
        }
    }

    suspend fun delete(alarm: Alarm) {
        alarmScheduler.cancel(alarm)
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun toggleEnabled(alarm: Alarm, isEnabled: Boolean) {
        val newAlarm = alarm.copy(isEnabled = isEnabled)
        update(newAlarm)
    }

    suspend fun getAlarmById(id: Int): Alarm? {
        return alarmDao.getAlarmById(id)
    }
}
