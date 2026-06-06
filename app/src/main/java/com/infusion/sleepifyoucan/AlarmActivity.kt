package com.infusion.sleepifyoucan

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
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
import com.infusion.sleepifyoucan.utils.ShakeDetector
import com.infusion.sleepifyoucan.utils.turnScreenOnAndKeyguardOff
import com.infusion.sleepifyoucan.ui.theme.*
import com.infusion.sleepifyoucan.ui.*
import androidx.lifecycle.lifecycleScope
import com.infusion.sleepifyoucan.data.EscapePreventionMode
import com.infusion.sleepifyoucan.data.UserPreferencesRepository
import com.infusion.sleepifyoucan.data.AppPreferences
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
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.coroutines.delay

class AlarmActivity : ComponentActivity() {

    private lateinit var shakeDetector: ShakeDetector
    private var escapePreventionMode: EscapePreventionMode = EscapePreventionMode.BALANCED
    private var alarmFlowSettled = false
    
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
        val userPreferencesRepository = UserPreferencesRepository(applicationContext)
        
        AlarmRingingViewModel.Factory(
            AlarmRepository(app.database.alarmDao(), alarmScheduler),
            StreakRepository(app.database.streakDao(), this),
            alarmScheduler,
            userPreferencesRepository,
            alarmId,
            missionConfig,
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enterRingingWindowMode()
        
        // Disable Back Button
        onBackPressedDispatcher.addCallback(this) {
             // Block back button
        }

        shakeDetector = ShakeDetector(this)
        
        // Start Evil Mode if preference is EVIL
        val userPrefsRepository = UserPreferencesRepository(applicationContext)
        lifecycleScope.launch {
            userPrefsRepository.preferences.collect { prefs ->
                escapePreventionMode = prefs.escapePreventionMode
                if (prefs.escapePreventionMode == EscapePreventionMode.EVIL) {
                    EvilModeHelper.startEvilMode(this@AlarmActivity)
                } else {
                    EvilModeHelper.stopEvilMode(this@AlarmActivity)
                }
            }
        }
        
        // Mission State starts as Initial, handled by UI interaction

        setContent {
            val prefs by userPrefsRepository.preferences.collectAsState(initial = AppPreferences())
            
            SleepIfYouCanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Charcoal
                ) {
                    val snoozeCount by viewModel.snoozeCount.collectAsState()
                    
                    // One-shot events from ViewModel (snooze, finish)
                    LaunchedEffect(Unit) {
                        viewModel.events.collect { event ->
                            when (event) {
                                is AlarmEvent.StopAndFinish -> {
                                    alarmFlowSettled = true
                                    EvilModeHelper.stopEvilMode(this@AlarmActivity)
                                    stopService()
                                    finish()
                                }
                                is AlarmEvent.SnoozeAndFinish -> {
                                    alarmFlowSettled = true
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
                                snoozeCount < prefs.maxSnoozeCount,
                        snoozeDuration = intent.getIntExtra("SNOOZE_DURATION", 5),
                        viewModel = viewModel,
                        shakeDetector = shakeDetector
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enterRingingWindowMode()
    }
    
    override fun onUserInteraction() {
        super.onUserInteraction()
        RingtoneService.recordUserInteraction()
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
        // If user leaves before the alarm is settled, count it as an escape and bring the alarm back.
        if (
            !alarmFlowSettled &&
            escapePreventionMode != EscapePreventionMode.OFF &&
            missionState !is MissionState.Completed &&
            missionState !is MissionState.Initial
        ) {
            val alarmId = intent.getIntExtra("ALARM_ID", 0)
            RingtoneService.recordEscape(alarmId)
            
            // Re-launch the activity to bring user back
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    val relaunchIntent = Intent(this, AlarmActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        )
                        intent.extras?.let { putExtras(it) }
                    }
                    try {
                        startActivity(relaunchIntent)
                    } catch (e: Exception) {
                        Log.w("AlarmActivity", "Unable to relaunch alarm activity", e)
                    }
                }
            }, 500)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        EvilModeHelper.stopEvilMode(this)
        shakeDetector.stop()
    }

    private fun enterRingingWindowMode() {
        turnScreenOnAndKeyguardOff()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
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
        is MissionState.Typing -> {
            TypingMissionScreen(
                targetWord = state.targetWord,
                currentInput = state.currentInput,
                caseSensitive = state.caseSensitive,
                onInputChange = { viewModel.onTypingInput(it) }
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
                .background(Canvas)
                .safeDrawingPadding()
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
                // A short hold prevents accidental taps while the alarm is ringing.
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
                        .height(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPressed) SurfaceElevated else Surface)
                        .border(
                            width = 1.dp,
                            color = if (isPressed) HairlineStrong else Hairline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .pointerInput(Unit) {
                            this.awaitPointerEventScope {
                                while (true) {
                                    awaitFirstDown(requireUnconsumed = false)
                                    isPressed = true
                                    
                                    waitForUpOrCancellation()
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
                                .background(AccentYellowSoft)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isPressed) {
                            CircularProgressIndicator(
                                progress = { progress },
                                color = AccentYellow,
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
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = AccentYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "HOLD TO START MISSION",
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
                            contentColor = AccentBlue
                        ),
                        border = BorderStroke(1.dp, AccentBlueSoft)
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
