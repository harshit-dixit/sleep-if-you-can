package com.infusion.sleepifyoucan

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.infusion.sleepifyoucan.ui.AlarmRingingViewModel
import com.infusion.sleepifyoucan.ui.MissionState
import com.infusion.sleepifyoucan.ui.ShakeMissionScreen
import com.infusion.sleepifyoucan.ui.MathMissionScreen
import com.infusion.sleepifyoucan.ui.MemoryMissionScreen
import com.infusion.sleepifyoucan.utils.ShakeDetector
import com.infusion.sleepifyoucan.utils.turnScreenOnAndKeyguardOff
import com.infusion.sleepifyoucan.ui.theme.*

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
        
        AlarmRingingViewModel.Factory(
            AlarmRepository(app.database.alarmDao(), AlarmScheduler(this)),
            StreakRepository(app.database.streakDao(), this),
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
                    color = BlackMute
                ) {
                    val missionState by viewModel.missionState.collectAsState()
                    
                    // Side-effect: If Completed, finish activity
                    LaunchedEffect(missionState) {
                        if (missionState is MissionState.Completed) {
                            stopService()
                            finish()
                        }
                    }

                    AlarmRingingScreenHost(
                        label = intent.getStringExtra("LABEL"),
                        isSnoozeEnabled = intent.getBooleanExtra("IS_SNOOZE_ENABLED", true),
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
        MissionState.Completed -> {
            // Handled by LaunchedEffect in Activity
            Box(Modifier.fillMaxSize().background(BlackMute))
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
                .background(BlackMute)
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
                        containerColor = OrangeJuice
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
