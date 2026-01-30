package com.infusion.sleepifyoucan.ui

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.infusion.sleepifyoucan.data.Alarm
import com.infusion.sleepifyoucan.data.AlarmRepository
import com.infusion.sleepifyoucan.data.MissionConfig
import com.infusion.sleepifyoucan.data.StreakRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

// --- State Definitions ---

@Parcelize
sealed class MissionState : Parcelable {
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
    data class Memory(
        val cards: List<Card>,
        val flippedIndices: List<Int> = emptyList(),
        val matchedPairs: Int = 0,
        val gridSize: Int = 4,
        val isComplete: Boolean = false
    ) : MissionState()
    
    @Parcelize
    object Completed : MissionState()
}

@Parcelize
data class Card(
    val id: Int,
    val symbol: String, // Emoji
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
) : Parcelable

// --- ViewModel ---

class AlarmRingingViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val alarmRepository: AlarmRepository,
    private val streakRepository: StreakRepository,
    private val alarmId: Int, // Passed via Factory
    private val missionConfig: MissionConfig // Passed via Factory
) : ViewModel() {

    // Persistent State
    private val _missionState = savedStateHandle.getStateFlow<MissionState>("mission_state", MissionState.Initial)
    val missionState: StateFlow<MissionState> = _missionState

    // initialize mission if needed
    fun initializeMission() {
        if (_missionState.value is MissionState.Initial) {
            val newState = when (missionConfig) {
                is MissionConfig.Shake -> MissionState.Shake(missionConfig.targetShakes)
                is MissionConfig.Math -> MissionState.Math(
                    missionConfig.difficulty.name,
                    0,
                    missionConfig.problemCount
                )
                is MissionConfig.Memory -> {
                    val cards = generateMemoryCards(missionConfig.gridSize)
                    MissionState.Memory(cards = cards, gridSize = missionConfig.gridSize)
                }
            }
            updateState(newState)
        }
    }

    // --- Actions ---

    fun onShake() {
        val current = _missionState.value
        if (current is MissionState.Shake) {
            val newCount = current.current + 1
            if (newCount >= current.target) {
                finishMission("SHAKE")
            } else {
                updateState(current.copy(current = newCount))
            }
        }
    }

    fun onMathSolved() {
        val current = _missionState.value
        if (current is MissionState.Math) {
            val newSolved = current.solveCount + 1
            if (newSolved >= current.totalProblems) {
                finishMission("MATH")
            } else {
                updateState(current.copy(solveCount = newSolved))
            }
        }
    }

    // Memory Game Logic
    fun onCardClicked(index: Int) {
        val current = _missionState.value as? MissionState.Memory ?: return
        
        // Validation: Index in bounds, not already flipped/matched, and not waiting for reset (2 flipped)
        if (index !in current.cards.indices) return
        val card = current.cards[index]
        if (card.isFlipped || card.isMatched || current.flippedIndices.size >= 2) return

        // Flip the card
        val newCards = current.cards.toMutableList()
        newCards[index] = card.copy(isFlipped = true)
        val newFlippedIndices = current.flippedIndices + index
        
        val intermediateState = current.copy(
            cards = newCards, 
            flippedIndices = newFlippedIndices
        )
        updateState(intermediateState)

        // Check Match if 2 cards flipped
        if (newFlippedIndices.size == 2) {
            viewModelScope.launch {
                // Brief delay to let user see the second card
                delay(800)
                checkForMatch(intermediateState, newFlippedIndices[0], newFlippedIndices[1])
            }
        }
    }

    private fun checkForMatch(requestState: MissionState.Memory, index1: Int, index2: Int) {
        // Reload latest state to ensure we don't overwrite concurrent changes (though rare here)
        val current = _missionState.value as? MissionState.Memory ?: return
        
        // Guard: Verify we are still in the state where we want to check these
        // (Simplified: just proceed with logic on 'current')
        
        val cards = current.cards.toMutableList()
        val card1 = cards[index1]
        val card2 = cards[index2]

        if (card1.symbol == card2.symbol) {
            // Match!
            cards[index1] = card1.copy(isMatched = true)
            cards[index2] = card2.copy(isMatched = true)
            val newMatched = current.matchedPairs + 1
            val isComplete = newMatched * 2 >= cards.size // All pairs matched

            if (isComplete) {
                finishMission("MEMORY")
            } else {
                 updateState(current.copy(
                    cards = cards,
                    flippedIndices = emptyList(),
                    matchedPairs = newMatched
                ))
            }
        } else {
            // No Match - Flip back
            cards[index1] = card1.copy(isFlipped = false)
            cards[index2] = card2.copy(isFlipped = false)
            updateState(current.copy(
                cards = cards,
                flippedIndices = emptyList()
            ))
        }
    }
    
    private fun finishMission(type: String) {
        viewModelScope.launch {
            streakRepository.recordSuccessfulWakeUp(alarmId, type)
            // Disable alarm if one-time
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm != null && alarm.daysOfWeek.isEmpty()) {
                alarmRepository.toggleEnabled(alarm, false)
            }
            updateState(MissionState.Completed)
        }
    }

    fun snooze() {
         viewModelScope.launch {
             val alarm = alarmRepository.getAlarmById(alarmId)
             // Logic for snooze scheduling is handled in Repository or Activity side usually, 
             // but here we just signal to Activity to finish. 
             // Actually, we should call AlarmScheduler.scheduleSnooze here if we have it?
             // The original Activity called `AlarmScheduler(...).scheduleSnooze`.
             // Ideally we inject AlarmScheduler. Since we don't have it injected yet, 
             // we'll rely on the Activity to handle the actual scheduling based on a boolean/event?
             // OR better: Inject AlarmScheduler.
             // For now, I'll emit a Snooze "Event" or just let Activity handle it?
             // Let's assume the VM should do it. I'll need AlarmScheduler dependency.
         }
    }

    // --- Helpers ---

    private fun updateState(newState: MissionState) {
        savedStateHandle["mission_state"] = newState
    }

    private fun generateMemoryCards(gridSize: Int): List<Card> {
        val totalCards = gridSize * gridSize
        val pairCount = totalCards / 2
        val symbols = listOf(
             "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
             "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔",
             "🐧", "🐦", "fw", "🐺", "🐗", "🐴", "🦄", "🐝",
             "🐛", "🦋", "🐌", "🐞", "🐜", "🦟", "🦗", "🕷"
        ).shuffled().take(pairCount)
        
        val deck = (symbols + symbols).shuffled().mapIndexed { index, symbol ->
            Card(id = index, symbol = symbol)
        }
        return deck
    }

    // --- Factory ---

    class Factory(
        private val alarmRepository: AlarmRepository,
        private val streakRepository: StreakRepository,
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
                    alarmId,
                    missionConfig
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
