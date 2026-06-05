package com.infusion.sleepifyoucan.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
val Context.dataStore by preferencesDataStore(name = "user_preferences")

/**
 * App preferences data class containing all user settings.
 */
data class AppPreferences(
    // Alarm Behavior
    val missionAudioBehavior: MissionAudioBehavior = MissionAudioBehavior.REDUCE_ON_ACTIVITY,
    val escapePreventionMode: EscapePreventionMode = EscapePreventionMode.BALANCED,
    val volumeEscalation: Boolean = true,
    val maxSnoozeCount: Int = 3,
    
    // Missions
    val defaultMissionType: String = "SHAKE",
    val missionTimeoutMinutes: Int = 5,
    
    // Appearance
    val isDarkMode: Boolean = true,
    val use24HourFormat: Boolean = false,
    
    // Notifications
    val bedtimeReminderEnabled: Boolean = false,
    val bedtimeReminderTime: String = "22:00"
)

/**
 * How the alarm audio behaves during mission completion.
 */
enum class MissionAudioBehavior {
    ALWAYS_PLAY,        // Current behavior - alarm plays at full volume
    REDUCE_ON_ACTIVITY, // 50% volume when user is actively interacting
    PAUSE_ON_ACTIVITY   // Pause alarm, resume after 10s of inactivity
}

/**
 * Escape prevention strictness levels.
 */
enum class EscapePreventionMode {
    OFF,        // No prevention - user can leave freely
    BALANCED,   // Persistent notification + penalty for leaving
    EVIL        // Lock task mode - cannot leave until complete
}

/**
 * Repository for managing user preferences with DataStore.
 */
class UserPreferencesRepository(private val context: Context) {
    
    private object Keys {
        val MISSION_AUDIO_BEHAVIOR = stringPreferencesKey("mission_audio_behavior")
        val ESCAPE_PREVENTION_MODE = stringPreferencesKey("escape_prevention_mode")
        val VOLUME_ESCALATION = booleanPreferencesKey("volume_escalation")
        val MAX_SNOOZE_COUNT = intPreferencesKey("max_snooze_count")
        val DEFAULT_MISSION_TYPE = stringPreferencesKey("default_mission_type")
        val MISSION_TIMEOUT_MINUTES = intPreferencesKey("mission_timeout_minutes")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
        val BEDTIME_REMINDER_ENABLED = booleanPreferencesKey("bedtime_reminder_enabled")
        val BEDTIME_REMINDER_TIME = stringPreferencesKey("bedtime_reminder_time")
    }
    
    /**
     * Flow of current preferences - automatically updates when preferences change.
     */
    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            missionAudioBehavior = try {
                MissionAudioBehavior.valueOf(
                    prefs[Keys.MISSION_AUDIO_BEHAVIOR] ?: MissionAudioBehavior.REDUCE_ON_ACTIVITY.name
                )
            } catch (e: Exception) {
                MissionAudioBehavior.REDUCE_ON_ACTIVITY
            },
            escapePreventionMode = try {
                EscapePreventionMode.valueOf(
                    prefs[Keys.ESCAPE_PREVENTION_MODE] ?: EscapePreventionMode.BALANCED.name
                )
            } catch (e: Exception) {
                EscapePreventionMode.BALANCED
            },
            volumeEscalation = prefs[Keys.VOLUME_ESCALATION] ?: true,
            maxSnoozeCount = prefs[Keys.MAX_SNOOZE_COUNT] ?: 3,
            defaultMissionType = supportedMissionName(prefs[Keys.DEFAULT_MISSION_TYPE]),
            missionTimeoutMinutes = prefs[Keys.MISSION_TIMEOUT_MINUTES] ?: 5,
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
            use24HourFormat = prefs[Keys.USE_24_HOUR_FORMAT] ?: false,
            bedtimeReminderEnabled = prefs[Keys.BEDTIME_REMINDER_ENABLED] ?: false,
            bedtimeReminderTime = prefs[Keys.BEDTIME_REMINDER_TIME] ?: "22:00"
        )
    }
    
    // ---- Update methods ----
    
    suspend fun updateMissionAudioBehavior(behavior: MissionAudioBehavior) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MISSION_AUDIO_BEHAVIOR] = behavior.name
        }
    }
    
    suspend fun updateEscapePreventionMode(mode: EscapePreventionMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ESCAPE_PREVENTION_MODE] = mode.name
        }
    }
    
    suspend fun updateVolumeEscalation(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VOLUME_ESCALATION] = enabled
        }
    }
    
    suspend fun updateMaxSnoozeCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_SNOOZE_COUNT] = count
        }
    }
    
    suspend fun updateDefaultMissionType(type: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_MISSION_TYPE] = supportedMissionName(type)
        }
    }
    
    suspend fun updateMissionTimeoutMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MISSION_TIMEOUT_MINUTES] = minutes
        }
    }
    
    suspend fun updateDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun updateUse24HourFormat(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_24_HOUR_FORMAT] = enabled
        }
    }
    
    suspend fun updateBedtimeReminder(enabled: Boolean, time: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BEDTIME_REMINDER_ENABLED] = enabled
            time?.let { prefs[Keys.BEDTIME_REMINDER_TIME] = it }
        }
    }
    private fun supportedMissionName(type: String?): String {
        return when (type) {
            "SHAKE", "MATH", "TYPING", "BARCODE" -> type
            else -> "SHAKE"
        }
    }
}

// ---- Extension functions for display names ----

fun MissionAudioBehavior.displayName(): String = when (this) {
    MissionAudioBehavior.ALWAYS_PLAY -> "Always playing"
    MissionAudioBehavior.REDUCE_ON_ACTIVITY -> "Reduce on activity"
    MissionAudioBehavior.PAUSE_ON_ACTIVITY -> "Pause during mission"
}

fun MissionAudioBehavior.description(): String = when (this) {
    MissionAudioBehavior.ALWAYS_PLAY -> "Alarm keeps playing at full volume"
    MissionAudioBehavior.REDUCE_ON_ACTIVITY -> "Volume lowers to 50% while solving"
    MissionAudioBehavior.PAUSE_ON_ACTIVITY -> "Pauses alarm, resumes if inactive for 10s"
}

fun EscapePreventionMode.displayName(): String = when (this) {
    EscapePreventionMode.OFF -> "Off"
    EscapePreventionMode.BALANCED -> "Balanced"
    EscapePreventionMode.EVIL -> "Evil Mode"
}

fun EscapePreventionMode.description(): String = when (this) {
    EscapePreventionMode.OFF -> "You can leave the alarm screen freely"
    EscapePreventionMode.BALANCED -> "Returns you to alarm with penalty if you leave"
    EscapePreventionMode.EVIL -> "Phone is locked until mission is complete"
}
