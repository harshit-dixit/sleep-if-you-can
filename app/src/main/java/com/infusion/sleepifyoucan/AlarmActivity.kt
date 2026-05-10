package com.infusion.sleepifyoucan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.infusion.sleepifyoucan.data.AlarmRepository
import com.infusion.sleepifyoucan.data.AlarmScheduler
import com.infusion.sleepifyoucan.data.Converters
import com.infusion.sleepifyoucan.data.MissionConfig
import com.infusion.sleepifyoucan.data.StreakRepository
import com.infusion.sleepifyoucan.service.RingtoneService
import com.infusion.sleepifyoucan.ui.AlarmEvent
import com.infusion.sleepifyoucan.ui.AlarmRingingViewModel
import com.infusion.sleepifyoucan.ui.MissionState
import com.infusion.sleepifyoucan.ui.ShakeMissionScreen
import com.infusion.sleepifyoucan.ui.MathMissionScreen
import com.infusion.sleepifyoucan.ui.MemoryMissionScreen
import com.infusion.sleepifyoucan.utils.ShakeDetector
import com.infusion.sleepifyoucan.utils.turnScreenOnAndKeyguardOff
import com.infusion.sleepifyoucan.ui.theme.*
import com.infusion.sleepifyoucan.ui.*

class AlarmActivity : ComponentActivity() {

    private lateinit var shakeDetector: ShakeDetector
    
    // Lazy ViewModel initialization with Factory
    private val viewModel: AlarmRingingViewModel by viewModels {
        val app = application as SleepApplication
        val alarmId = intent.getIntExtra("ALARM_ID", 0)
        val missionJson = intent.getStringExtra("MISSION_CONFIG_JSON")
        val missionConfig = if (missionJson != null) {
            Converters().toMissionConfig(missionJson)
        } else {
            MissionConfig.Shake()
        }
        val alarmScheduler = AlarmScheduler(this)
        
        AlarmRingingViewModel.Factory(
            AlarmRepository(app.database.alarmDao(), alarmScheduler),
            StreakRepository(app.database.streakDao(), this),
            alarmScheduler,
            alarmId,
            missionConfig,
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        turnScreenOnAndKeyguardOff()
        
        // Disable Back Button
        onBackPressedDispatcher.addCallback(this) {
             // Block back button
        }

        shakeDetector = ShakeDetector(this)
        
        // Mission State starts as Initial, handled by UI interaction

        setContent {
            SleepIfYouCanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Charcoal
                ) {
                    val missionState by viewModel.missionState.collectAsState()
                    val snoozeCount by viewModel.snoozeCount.collectAsState()
                    
                    // One-shot events from ViewModel (snooze, finish)
                    LaunchedEffect(Unit) {
                        viewModel.events.collect { event ->
                            when (event) {
                                is AlarmEvent.StopAndFinish -> {
                                    stopService()
                                    finish()
                                }
                                is AlarmEvent.SnoozeAndFinish -> {
                                    stopService()
                                    finish()
                                }
                            }
                        }
                    }

                    AlarmRingingScreenHost(
                        label = intent.getStringExtra("LABEL"),
                        isSnoozeEnabled = intent.getBooleanExtra("IS_SNOOZE_ENABLED", true) &&
                                snoozeCount < AlarmRingingViewModel.MAX_SNOOZE_ATTEMPTS,
                        snoozeDuration = intent.getIntExtra("SNOOZE_DURATION", 5),
                        viewModel = viewModel,
                        shakeDetector = shakeDetector
                    )
                }
            }
        }
    }
    
    private fun stopService() {
        val stopIntent = Intent(this, RingtoneService::class.java).apply {
            action = RingtoneService.ACTION_STOP
        }
        startService(stopIntent)
        shakeDetector.stop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // Prevent silencing!
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onStop() {
        super.onStop()
        val missionState = viewModel.missionState.value
        // If user leaves during an active mission (not Initial or Completed), count as escape
        if (missionState !is MissionState.Completed && missionState !is MissionState.Initial) {
            val alarmId = intent.getIntExtra("ALARM_ID", 0)
            RingtoneService.recordEscape(alarmId)
            
            // Re-launch the activity to bring user back
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    val relaunchIntent = Intent(this, AlarmActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        intent.extras?.let { putExtras(it) }
                    }
                    startActivity(relaunchIntent)
                }
            }, 500)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        shakeDetector.stop()
    }
}

@Composable
fun AlarmRingingScreenHost(
    label: String?,
    isSnoozeEnabled: Boolean,
    snoozeDuration: Int,
    viewModel: AlarmRingingViewModel,
    shakeDetector: ShakeDetector
) {
    val missionState by viewModel.missionState.collectAsState()
    
    // Extract mission config from state type if possible to know what "Start" button launches
    // Or just rely on viewModel.missionState to determine screen.
    // Ideally, "Initial" state just shows the "DISMISS" button which transitions to specific mission state.

    when (val state = missionState) {
        is MissionState.Initial -> {
            InitialAlarmScreen(
                label = label,
                isSnoozeEnabled = isSnoozeEnabled,
                snoozeDuration = snoozeDuration,
                onDismissClick = { 
                    viewModel.initializeMission()
                },
                onSnoozeClick = { viewModel.snooze() }
             )
        }
        is MissionState.Shake -> {
            ShakeMissionScreen(
                currentShakes = state.current,
                targetShakes = state.target,
                shakeDetector = shakeDetector,
                onShake = { viewModel.onShake() }
            )
        }
        is MissionState.Math -> {
            MathMissionScreen(
                difficulty = state.difficulty,
                solveCount = state.solveCount,
                totalProblems = state.totalProblems,
                onSolved = { viewModel.onMathSolved() }
            )
        }
        is MissionState.Memory -> {
            MemoryMissionScreen(
                state = state,
                onCardClick = { viewModel.onCardClicked(it) }
            )
        }
        is MissionState.Typing -> {
            TypingMissionScreen(
                targetWord = state.targetWord,
                currentInput = state.currentInput,
                caseSensitive = state.caseSensitive,
                onInputChange = { viewModel.onTypingInput(it) }
            )
        }
        is MissionState.Squat -> {
            SquatMissionScreen(
                targetSquats = state.target,
                currentSquats = state.current,
                onSquatDetected = { viewModel.onSquatDetected() }
            )
        }
        is MissionState.Step -> {
            StepMissionScreen(
                targetSteps = state.target,
                currentSteps = state.current,
                onStepDetected = { viewModel.onStepDetected() }
            )
        }
        is MissionState.Photo -> {
            PhotoMissionScreen(
                requiredObject = state.requiredObject,
                onPhotoTaken = { viewModel.onPhotoTaken() }
            )
        }
        is MissionState.Barcode -> {
            BarcodeMissionScreen(
                expectedBarcode = state.expectedBarcode,
                onBarcodeScanned = { viewModel.onBarcodeScanned(it) }
            )
        }
        is MissionState.WakeUpCheck -> {
            WakeUpCheckScreen(
                onWakeUpConfirmed = { viewModel.onWakeUpConfirmed() }
            )
        }
        MissionState.Completed -> {
            // Handled by LaunchedEffect in Activity
            Box(Modifier.fillMaxSize().background(Charcoal))
        }
    }
}

@Composable
fun InitialAlarmScreen(
    label: String?,
    isSnoozeEnabled: Boolean,
    snoozeDuration: Int,
    onDismissClick: () -> Unit,
    onSnoozeClick: () -> Unit
) {
     Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GradientPrimary)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label ?: "WAKE UP",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Time to rise and shine!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismissClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Terracotta,
                        contentColor = TextOnAccent
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "DISMISS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                
                if (isSnoozeEnabled) {
                    OutlinedButton(
                        onClick = onSnoozeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        Text(
                            "SNOOZE ($snoozeDuration min)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
}
