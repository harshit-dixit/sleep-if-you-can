package com.infusion.sleepifyoucan.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromDaysList(value: List<Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDaysList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromMissionConfig(value: MissionConfig): String {
        // We need to preserve type information for Sealed classes. 
        // Simple GSON might lose the specific subclass if not configured with RuntimeTypeAdapterFactory.
        // For simplicity in this specific case, we can use a wrapper or manual serialization if needed.
        // But let's try standard GSON with a wrapper logic or just trust GSON if the structure is distinct.
        // A safer way for sealed classes without external libs is a custom wrapper.
        // However, let's use a simpler approach: encode the type in the JSON.
        
        // Actually, for Room, naive GSON might struggle with Sealed Classes deserialization 
        // unless we use a custom serializer.
        // Let's implement a custom container for storage to be safe.
        return gson.toJson(MissionConfigWrapper(value))
    }

    @TypeConverter
    fun toMissionConfig(value: String): MissionConfig {
        return try {
            val wrapper = gson.fromJson(value, MissionConfigWrapper::class.java)
            wrapper.toMissionConfig()
        } catch (e: Exception) {
            MissionConfig.Shake(20) // Fallback
        }
    }

    @TypeConverter
    fun fromAlarmSound(value: AlarmSound): String {
        return value.name
    }

    @TypeConverter
    fun toAlarmSound(value: String): AlarmSound {
        return try {
            AlarmSound.valueOf(value)
        } catch (e: Exception) {
            AlarmSound.DEFAULT
        }
    }

}

// Helper for GSON serialization of sealed class
data class MissionConfigWrapper(
    val type: String,
    val shakeTarget: Int? = null,
    val mathDifficulty: String? = null,
    val mathCount: Int? = null,
    val memoryGridSize: Int? = null,
    val typingTargetWord: String? = null,
    val typingCaseSensitive: Boolean? = null,
    val squatTarget: Int? = null,
    val stepTarget: Int? = null,
    val photoRequiredObject: String? = null,
    val barcodeExpected: String? = null
) {
    constructor(config: MissionConfig) : this(
        type = when (config) {
            is MissionConfig.Shake -> "SHAKE"
            is MissionConfig.Math -> "MATH"
            is MissionConfig.Typing -> "TYPING"
            is MissionConfig.Barcode -> "BARCODE"
        },
        shakeTarget = if (config is MissionConfig.Shake) config.targetShakes else null,
        mathDifficulty = if (config is MissionConfig.Math) config.difficulty.name else null,
        mathCount = if (config is MissionConfig.Math) config.problemCount else null,
        typingTargetWord = if (config is MissionConfig.Typing) config.targetWord else null,
        typingCaseSensitive = if (config is MissionConfig.Typing) config.caseSensitive else null,
        barcodeExpected = if (config is MissionConfig.Barcode) config.expectedBarcode else null
    )

    fun toMissionConfig(): MissionConfig {
        return when (type) {
            "SHAKE" -> MissionConfig.Shake(shakeTarget ?: 20)
            "MATH" -> MissionConfig.Math(
                Difficulty.valueOf(mathDifficulty ?: "EASY"),
                mathCount ?: 3
            )
            "TYPING" -> MissionConfig.Typing(
                typingTargetWord ?: "HELLO",
                typingCaseSensitive ?: false
            )
            "BARCODE" -> if (barcodeExpected.isNullOrBlank()) {
                MissionConfig.Shake(20)
            } else {
                MissionConfig.Barcode(barcodeExpected)
            }
            else -> MissionConfig.Shake(20) // Fallback
        }
    }
}
