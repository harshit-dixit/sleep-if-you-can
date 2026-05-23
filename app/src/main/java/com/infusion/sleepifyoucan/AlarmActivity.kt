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
import androidx.lifecycle.lifecycleScope
import com.infusion.sleepifyoucan.data.EscapePreventionMode
import com.infusion.sleepifyoucan.data.UserPreferencesRepository
import com.infusion.sleepifyoucan.utils.EvilModeHelper
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import kotlinx.coroutines.delay

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
        
        // Start Evil Mode if preference is EVIL
        val userPrefsRepository = UserPreferencesRepository(applicationContext)
        lifecycleScope.launch {
            userPrefsRepository.preferences.collect { prefs ->
                if (prefs.escapePreventionMode == EscapePreventionMode.EVIL) {
                    EvilModeHelper.startEvilMode(this@AlarmActivity)
                }
            }
        }
        
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
                                    EvilModeHelper.stopEvilMode(this@AlarmActivity)
                                    stopService()
                                    finish()
                                }
                                is AlarmEvent.SnoozeAndFinish -> {
                                    EvilModeHelper.stopEvilMode(this@AlarmActivity)
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
        EvilModeHelper.stopEvilMode(this)
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
                // Custom Press-and-Hold Dismiss Button
                var isPressed by remember { mutableStateOf(false) }
                var progress by remember { mutableFloatStateOf(0f) }
                val view = LocalView.current
                
                LaunchedEffect(isPressed) {
                    if (isPressed) {
                        val startTime = System.currentTimeMillis()
                        val duration = 3000L // 3 seconds
                        var lastTick = 0L
                        while (progress < 1f) {
                            val elapsed = System.currentTimeMillis() - startTime
                            progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                            
                            // Haptic feedback tick every 500ms
                            val tickCount = elapsed / 500
                            if (tickCount > lastTick) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                lastTick = tickCount
                            }
                            
                            if (progress >= 1f) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                onDismissClick()
                                break
                            }
                            delay(16)
                        }
                    } else {
                        progress = 0f
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isPressed) Terracotta.copy(alpha = 0.2f) else GlassWhite)
                        .border(
                            width = 2.dp,
                            color = if (isPressed) Terracotta else GlassBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .pointerInput(Unit) {
                            this.awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    isPressed = true
                                    
                                    val change = waitForUpOrCancellation()
                                    isPressed = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Progress Bar background filling
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .align(Alignment.CenterStart)
                                .background(Terracotta.copy(alpha = 0.5f))
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isPressed) {
                            CircularProgressIndicator(
                                progress = { progress },
                                color = Terracotta,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp),
                                trackColor = Color.Transparent
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "HOLDING...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = Terracotta,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "HOLD TO DISMISS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
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
