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
import com.infusion.sleepifyoucan.service.RingtoneService
import com.infusion.sleepifyoucan.utils.ShakeDetector
import com.infusion.sleepifyoucan.utils.turnScreenOnAndKeyguardOff
import com.infusion.sleepifyoucan.ui.theme.SleepIfYouCanTheme
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
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmRingingScreen(
                        label = label,
                        isSnoozeEnabled = isSnoozeEnabled,
                        snoozeDuration = snoozeDuration,
                        missionConfig = missionConfig,
                        onSnooze = {
                            snoozeAlarm(alarmId, snoozeDuration)
                        },
                        onDismiss = {
                            dismissAlarm(alarmId)
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
    onDismiss: () -> Unit,
    shakeDetector: ShakeDetector
) {
    var missionState by remember { mutableStateOf<MissionState>(MissionState.Initial) }

    if (missionState is MissionState.Initial) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label ?: "ALARM",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Clock could go here
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { 
                        // Start Mission!
                        missionState = when (missionConfig) {
                            is MissionConfig.Shake -> MissionState.ShakeMission
                            is MissionConfig.Math -> MissionState.MathMission
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("DISMISS", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                
                if (isSnoozeEnabled) {
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("SNOOZE ($snoozeDuration min)", fontSize = 18.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    } else if (missionState is MissionState.ShakeMission) {
        ShakeMissionScreen(
            config = missionConfig as MissionConfig.Shake,
            shakeDetector = shakeDetector,
            onComplete = onDismiss
        )
    } else if (missionState is MissionState.MathMission) {
        MathMissionScreen(
            config = missionConfig as MissionConfig.Math,
            onComplete = onDismiss
        )
    }
}

// --- SHAKE MISSION ---

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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
         Text("SHAKE IT!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
         Spacer(modifier = Modifier.height(32.dp))
         CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(150.dp), strokeWidth = 12.dp)
         Text("${shakes}/${config.targetShakes}", fontSize = 48.sp)
    }
}

// --- MATH MISSION ---

@Composable
fun MathMissionScreen(
    config: MissionConfig.Math,
    onComplete: () -> Unit
) {
    // Generate Problems
    val problems = remember { generateMathProblems(config.difficulty, config.problemCount) }
    var currentProblemIndex by remember { mutableIntStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    if (currentProblemIndex >= problems.size) {
        // All Done
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    val currentProblem = problems[currentProblemIndex]

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Progress
        Text("Problem ${currentProblemIndex + 1} / ${problems.size}", fontSize = 18.sp)
        
        Spacer(modifier = Modifier.height(32.dp))

        // Problem Display
        Text(
            text = currentProblem.display,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Input Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userInput,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Numeric Keypad
        NumericKeypad(
            onNumberClick = { num -> 
                if (userInput.length < 5) userInput += num 
                isError = false
            },
            onDeleteClick = { 
                if (userInput.isNotEmpty()) userInput = userInput.dropLast(1)
                isError = false
            },
            onEnterClick = {
                if (userInput.toIntOrNull() == currentProblem.answer) {
                    // Correct!
                    userInput = ""
                    currentProblemIndex++
                } else {
                    // Wrong
                    isError = true
                    userInput = ""
                }
            }
        )
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
                    containerColor = if (key == "OK") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (key == "OK") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                if (key == "DEL") {
                     // Icon
                     Text("<") 
                } else {
                    Text(text = key, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
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
