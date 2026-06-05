package com.infusion.sleepifyoucan.ui

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infusion.sleepifyoucan.data.Alarm
import com.infusion.sleepifyoucan.data.AlarmRepository
import com.infusion.sleepifyoucan.data.AlarmScheduler
import com.infusion.sleepifyoucan.data.MissionConfig
import com.infusion.sleepifyoucan.data.StreakRepository
import com.infusion.sleepifyoucan.data.UserPreferencesRepository
import com.infusion.sleepifyoucan.service.RingtoneService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class MissionState : Parcelable {
    @Parcelize
    object Initial : MissionState()

    @Parcelize
    data class Shake(val target: Int, val current: Int = 0) : MissionState()

    @Parcelize
    data class Math(
        val difficulty: String,
        val solveCount: Int,
        val totalProblems: Int
    ) : MissionState()

    @Parcelize
    data class Typing(
        val targetWord: String,
        val currentInput: String = "",
        val caseSensitive: Boolean = false
    ) : MissionState()

    @Parcelize
    data class Barcode(
        val expectedBarcode: String,
        val scannedBarcode: String? = null
    ) : MissionState()

    @Parcelize
    object WakeUpCheck : MissionState()

    @Parcelize
    object Completed : MissionState()
}

sealed class AlarmEvent {
    object StopAndFinish : AlarmEvent()
    data class SnoozeAndFinish(val durationMillis: Long) : AlarmEvent()
}

class AlarmRingingViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val alarmRepository: AlarmRepository,
    private val streakRepository: StreakRepository,
    private val alarmScheduler: AlarmScheduler,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val alarmId: Int,
    private val missionConfig: MissionConfig
) : ViewModel() {

    private val _missionState = savedStateHandle.getStateFlow<MissionState>(
        "mission_state",
        MissionState.Initial
    )
    val missionState: StateFlow<MissionState> = _missionState

    private val _snoozeCount = savedStateHandle.getStateFlow("snooze_count", 0)
    val snoozeCount: StateFlow<Int> = _snoozeCount

    private val _events = MutableSharedFlow<AlarmEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AlarmEvent> = _events.asSharedFlow()

    fun initializeMission() {
        if (_missionState.value !is MissionState.Initial) return

        val penalty = RingtoneService.getEscapePenalty()
        val newState = when (missionConfig) {
            is MissionConfig.Shake -> MissionState.Shake(
                target = missionConfig.targetShakes + (penalty * 5)
            )
            is MissionConfig.Math -> MissionState.Math(
                difficulty = missionConfig.difficulty.name,
                solveCount = 0,
                totalProblems = (missionConfig.problemCount + penalty).coerceAtLeast(1)
            )
            is MissionConfig.Typing -> MissionState.Typing(
                targetWord = missionConfig.targetWord,
                caseSensitive = missionConfig.caseSensitive
            )
            is MissionConfig.Barcode -> MissionState.Barcode(
                expectedBarcode = missionConfig.expectedBarcode
            )
        }
        updateState(newState)
    }

    fun onShake() {
        RingtoneService.recordUserInteraction()
        val current = _missionState.value as? MissionState.Shake ?: return
        val nextCount = current.current + 1
        if (nextCount >= current.target) {
            finishMission("SHAKE")
        } else {
            updateState(current.copy(current = nextCount))
        }
    }

    fun onMathSolved() {
        RingtoneService.recordUserInteraction()
        val current = _missionState.value as? MissionState.Math ?: return
        val nextSolved = current.solveCount + 1
        if (nextSolved >= current.totalProblems) {
            finishMission("MATH")
        } else {
            updateState(current.copy(solveCount = nextSolved))
        }
    }

    fun onTypingInput(input: String) {
        RingtoneService.recordUserInteraction()
        val current = _missionState.value as? MissionState.Typing ?: return
        val target = if (current.caseSensitive) current.targetWord else current.targetWord.uppercase()
        val userInput = if (current.caseSensitive) input else input.uppercase()

        if (userInput == target) {
            finishMission("TYPING")
        } else {
            updateState(current.copy(currentInput = input))
        }
    }

    fun onBarcodeScanned(scannedCode: String) {
        RingtoneService.recordUserInteraction()
        val current = _missionState.value as? MissionState.Barcode ?: return
        if (current.expectedBarcode == scannedCode) {
            updateState(current.copy(scannedBarcode = scannedCode))
            finishMission("BARCODE")
        }
    }

    fun onWakeUpConfirmed() {
        if (_missionState.value !is MissionState.WakeUpCheck) return

        viewModelScope.launch {
            val alarm = alarmRepository.getAlarmById(alarmId)
            completeAlarm(alarm)
        }
    }

    fun snooze() {
        val currentSnoozeCount = _snoozeCount.value
        viewModelScope.launch {
            try {
                val prefs = userPreferencesRepository.preferences.first()
                if (currentSnoozeCount >= prefs.maxSnoozeCount) return@launch

                val alarm = alarmRepository.getAlarmById(alarmId) ?: return@launch
                val durationMillis = (alarm.snoozeDuration.toLong() + currentSnoozeCount) * 60_000L

                alarmScheduler.scheduleSnooze(alarm, durationMillis)
                savedStateHandle["snooze_count"] = currentSnoozeCount + 1
                _events.emit(AlarmEvent.SnoozeAndFinish(durationMillis))
            } catch (e: Exception) {
                android.util.Log.e("AlarmRingingViewModel", "Failed to schedule snooze", e)
            }
        }
    }

    private fun finishMission(type: String) {
        viewModelScope.launch {
            streakRepository.recordSuccessfulWakeUp(alarmId, type)
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm?.isWakeUpCheckEnabled == true) {
                updateState(MissionState.WakeUpCheck)
            } else {
                completeAlarm(alarm)
            }
        }
    }

    private suspend fun completeAlarm(alarm: Alarm?) {
        if (alarm != null) {
            if (alarm.daysOfWeek.isEmpty()) {
                alarmRepository.toggleEnabled(alarm, false)
            } else {
                alarmScheduler.schedule(alarm)
            }
        }
        updateState(MissionState.Completed)
        _events.emit(AlarmEvent.StopAndFinish)
    }

    private fun updateState(newState: MissionState) {
        savedStateHandle["mission_state"] = newState
    }

    class Factory(
        private val alarmRepository: AlarmRepository,
        private val streakRepository: StreakRepository,
        private val alarmScheduler: AlarmScheduler,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val alarmId: Int,
        private val missionConfig: MissionConfig,
        owner: androidx.savedstate.SavedStateRegistryOwner,
        defaultArgs: android.os.Bundle? = null
    ) : androidx.lifecycle.AbstractSavedStateViewModelFactory(owner, defaultArgs) {
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            if (modelClass.isAssignableFrom(AlarmRingingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AlarmRingingViewModel(
                    handle,
                    alarmRepository,
                    streakRepository,
                    alarmScheduler,
                    userPreferencesRepository,
                    alarmId,
                    missionConfig
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
