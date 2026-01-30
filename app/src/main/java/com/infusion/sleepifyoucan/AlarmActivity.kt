package com.infusion.sleepifyoucan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.infusion.sleepifyoucan.data.AlarmRepository
import com.infusion.sleepifyoucan.data.AlarmScheduler
import com.infusion.sleepifyoucan.data.Converters
import com.infusion.sleepifyoucan.data.MissionConfig
import com.infusion.sleepifyoucan.data.Difficulty
import com.infusion.sleepifyoucan.data.StreakRepository
import com.infusion.sleepifyoucan.service.RingtoneService
import com.infusion.sleepifyoucan.utils.ShakeDetector
import com.infusion.sleepifyoucan.utils.turnScreenOnAndKeyguardOff
import com.infusion.sleepifyoucan.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class AlarmActivity : ComponentActivity() {

    private lateinit var shakeDetector: ShakeDetector
    private var isMissionActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        turnScreenOnAndKeyguardOff()
        
        // Disable Back Button
        onBackPressedDispatcher.addCallback(this) {
             // Block back button
        }

        // Extract Data
        val alarmId = intent.getIntExtra("ALARM_ID", 0)
        val missionJson = intent.getStringExtra("MISSION_CONFIG_JSON")
        val missionConfig = if (missionJson != null) {
            Converters().toMissionConfig(missionJson)
        } else {
             MissionConfig.Shake() // Default
        }
        val label = intent.getStringExtra("LABEL")
        val isSnoozeEnabled = intent.getBooleanExtra("IS_SNOOZE_ENABLED", true)
        val snoozeDuration = intent.getIntExtra("SNOOZE_DURATION", 5)

        shakeDetector = ShakeDetector(this)

        setContent {
            SleepIfYouCanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BlackMute
                ) {
                    AlarmRingingScreen(
                        label = label,
                        isSnoozeEnabled = isSnoozeEnabled,
                        snoozeDuration = snoozeDuration,
                        missionConfig = missionConfig,
                        onSnooze = {
                            snoozeAlarm(alarmId, snoozeDuration)
                        },
                        onDismiss = { missionType ->
                            // Record streak on successful mission completion
                            recordStreakAndDismiss(alarmId, missionType)
                        },
                        shakeDetector = shakeDetector
                    )
                }
            }
        }
    }

    private fun snoozeAlarm(alarmId: Int, durationMinutes: Int) {
        stopService()
        
        // Schedule Snooze for X minutes
        val repository = getRepository()
        lifecycleScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                // Schedule snooze
                 AlarmScheduler(this@AlarmActivity).scheduleSnooze(alarm, durationMinutes * 60 * 1000L)
            }
            finish()
        }
    }

    private fun recordStreakAndDismiss(alarmId: Int, missionType: String) {
        val app = application as SleepApplication
        val streakRepository = StreakRepository(app.database.streakDao(), this)
        
        lifecycleScope.launch {
            // Record successful wake-up
            streakRepository.recordSuccessfulWakeUp(alarmId, missionType)
            dismissAlarm(alarmId)
        }
    }

    private fun dismissAlarm(alarmId: Int) {
        stopService()
        
        // Handle "Next Alarm" logic (repeating)
        val repository = getRepository()
        lifecycleScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                if (alarm.daysOfWeek.isNotEmpty()) {
                    // Reschedule for next occurrence
                     AlarmScheduler(this@AlarmActivity).schedule(alarm)
                } else {
                    // Disable one-time alarm
                    repository.toggleEnabled(alarm, false)
                }
            }
            finish()
        }
    }
    
    private fun stopService() {
        val stopIntent = Intent(this, RingtoneService::class.java).apply {
            action = RingtoneService.ACTION_STOP
        }
        startService(stopIntent)
        shakeDetector.stop()
    }

    private fun getRepository(): AlarmRepository {
        val app = application as SleepApplication
        return AlarmRepository(app.database.alarmDao(), AlarmScheduler(this))
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
fun AlarmRingingScreen(
    label: String?,
    isSnoozeEnabled: Boolean,
    snoozeDuration: Int,
    missionConfig: MissionConfig,
    onSnooze: () -> Unit,
    onDismiss: (String) -> Unit,  // Now takes mission type
    shakeDetector: ShakeDetector
) {
    var missionState by remember { mutableStateOf<MissionState>(MissionState.Initial) }

    if (missionState is MissionState.Initial) {
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
                    onClick = { 
                        missionState = when (missionConfig) {
                            is MissionConfig.Shake -> MissionState.ShakeMission
                            is MissionConfig.Math -> MissionState.MathMission
                        }
                    },
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
                        onClick = onSnooze,
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
    } else if (missionState is MissionState.ShakeMission) {
        ShakeMissionScreen(
            config = missionConfig as MissionConfig.Shake,
            shakeDetector = shakeDetector,
            onComplete = { onDismiss("SHAKE") }
        )
    } else if (missionState is MissionState.MathMission) {
        MathMissionScreen(
            config = missionConfig as MissionConfig.Math,
            onComplete = { onDismiss("MATH") }
        )
    }
}

// --- SHAKE MISSION with Glass Filling Animation ---

@Composable
fun ShakeMissionScreen(
    config: MissionConfig.Shake,
    shakeDetector: ShakeDetector,
    onComplete: () -> Unit
) {
    var shakes by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        shakeDetector.start {
            shakes++
            if (shakes >= config.targetShakes) {
                onComplete()
            }
        }
    }

    val progress by animateFloatAsState(
        targetValue = shakes.toFloat() / config.targetShakes.toFloat(),
        label = "Progress"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackMute)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SHAKE IT!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Glass Filling Animation
        GlassFillingAnimation(
            progress = progress,
            modifier = Modifier.size(200.dp, 280.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "${shakes}/${config.targetShakes}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = PurpleNight
        )
        
        if (progress < 1f) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Keep shaking!",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

/**
 * Glass filling with water animation.
 */
@Composable
fun GlassFillingAnimation(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val waterColor by animateColorAsState(
        targetValue = when {
            progress >= 1f -> GreenLand
            progress >= 0.7f -> PurpleNight
            else -> PurpleNight.copy(alpha = 0.7f)
        },
        label = "WaterColor"
    )
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val glassThickness = 8.dp.toPx()
        val glassRadius = 16.dp.toPx()
        
        // Glass outline
        drawRoundRect(
            color = TextSecondary.copy(alpha = 0.3f),
            topLeft = Offset(0f, 0f),
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(glassRadius)
        )
        
        // Water fill (from bottom up)
        val waterHeight = (height - glassThickness * 2) * progress
        val waterTop = height - glassThickness - waterHeight
        
        drawRoundRect(
            color = waterColor,
            topLeft = Offset(glassThickness, waterTop),
            size = Size(width - glassThickness * 2, waterHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(glassRadius - glassThickness)
        )
    }
}

// --- MATH MISSION with Color Feedback ---

@Composable
fun MathMissionScreen(
    config: MissionConfig.Math,
    onComplete: () -> Unit
) {
    val problems = remember { generateMathProblems(config.difficulty, config.problemCount) }
    var currentProblemIndex by remember { mutableIntStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    if (currentProblemIndex >= problems.size) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    val currentProblem = problems[currentProblemIndex]
    
    // Animate background color on correct/wrong answer
    val inputBackgroundColor by animateColorAsState(
        targetValue = when {
            showSuccess -> GreenLand.copy(alpha = 0.3f)
            isError -> OrangeJuice.copy(alpha = 0.3f)
            else -> BlackMuteSurface
        },
        label = "InputBg"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackMute)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Progress
        Text(
            "Problem ${currentProblemIndex + 1} / ${problems.size}",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Problem Display
        Text(
            text = currentProblem.display,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = PurpleNight
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Input Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(inputBackgroundColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userInput.ifEmpty { "?" },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (userInput.isEmpty()) TextDisabled else TextPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Numeric Keypad
        NumericKeypad(
            onNumberClick = { num -> 
                if (userInput.length < 5) userInput += num 
                isError = false
                showSuccess = false
            },
            onDeleteClick = { 
                if (userInput.isNotEmpty()) userInput = userInput.dropLast(1)
                isError = false
                showSuccess = false
            },
            onEnterClick = {
                if (userInput.toIntOrNull() == currentProblem.answer) {
                    showSuccess = true
                    userInput = ""
                    currentProblemIndex++
                } else {
                    isError = true
                    userInput = ""
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onEnterClick: () -> Unit
) {
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "DEL", "0", "OK"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(keys.size) { index ->
            val key = keys[index]
            Button(
                onClick = {
                    when (key) {
                        "DEL" -> onDeleteClick()
                        "OK" -> onEnterClick()
                        else -> onNumberClick(key)
                    }
                },
                modifier = Modifier
                    .aspectRatio(1.5f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (key) {
                        "OK" -> GreenLand
                        "DEL" -> OrangeJuice.copy(alpha = 0.7f)
                        else -> BlackMuteSurface
                    },
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = if (key == "DEL") "⌫" else key,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

sealed class MissionState {
    object Initial : MissionState()
    object ShakeMission : MissionState()
    object MathMission : MissionState()
}

data class MathProblem(val display: String, val answer: Int)

fun generateMathProblems(difficulty: Difficulty, count: Int): List<MathProblem> {
    return List(count) {
        val (a, b) = when (difficulty) {
            Difficulty.EASY -> Pair(Random.nextInt(1, 10), Random.nextInt(1, 10)) // 5 + 3
            Difficulty.MEDIUM -> Pair(Random.nextInt(10, 50), Random.nextInt(1, 10)) // 23 + 6
            Difficulty.HARD -> Pair(Random.nextInt(10, 99), Random.nextInt(10, 99)) // 45 + 88
        }
        val isAdd = Random.nextBoolean()
        if (isAdd) {
            MathProblem("$a + $b", a + b)
        } else {
            // Ensure positive result for simplicity
            val max = maxOf(a, b)
            val min = minOf(a, b)
            MathProblem("$max - $min", max - min)
        }
    }
}
