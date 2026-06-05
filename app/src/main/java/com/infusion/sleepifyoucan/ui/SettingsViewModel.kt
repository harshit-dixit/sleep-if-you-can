package com.infusion.sleepifyoucan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.infusion.sleepifyoucan.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = UserPreferencesRepository(application)
    
    /**
     * Current app preferences as StateFlow.
     */
    val preferences: StateFlow<AppPreferences> = repository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferences()
        )
    
    fun updateMissionAudioBehavior(behavior: MissionAudioBehavior) {
        viewModelScope.launch {
            repository.updateMissionAudioBehavior(behavior)
        }
    }
    
    fun updateEscapePreventionMode(mode: EscapePreventionMode) {
        viewModelScope.launch {
            repository.updateEscapePreventionMode(mode)
        }
    }
    
    fun updateVolumeEscalation(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateVolumeEscalation(enabled)
        }
    }
    
    fun updateMaxSnoozeCount(count: Int) {
        viewModelScope.launch {
            repository.updateMaxSnoozeCount(count)
        }
    }
    
    fun updateDefaultMissionType(type: String) {
        viewModelScope.launch {
            repository.updateDefaultMissionType(type)
        }
    }

    fun updateUse24HourFormat(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateUse24HourFormat(enabled)
        }
    }
}
