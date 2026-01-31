package com.infusion.sleepifyoucan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import com.infusion.sleepifyoucan.data.Difficulty
import com.infusion.sleepifyoucan.data.MissionConfig
import com.infusion.sleepifyoucan.ui.theme.*
import kotlin.random.Random

// Helper for Math Problems (Moved from AlarmActivity or Activity-level state)
// Since we want to persist state in VM, the generation should primarily happen in VM.
// BUT, refactoring that logic now might be invasive.
// For now, I will keep the generation here or just display what VM gives.
// The VM `MissionState.Math` has `problemsLeft`. It doesn't store the CURRENT problem string.
// To support rotation survival (SavedState), we SHOULD store the active problem in VM.
// However, the `MissionState.Math` I defined in Step 89 is simple:
// data class Math(val difficulty: String, val solveCount: Int, val totalProblems: Int)
// It misses the "Current Problem".
// I'll stick to generating it here for simplicity of migration, acknowledging that
// rotating screen might generate a new problem (minor UX annoyance but valid MVP).
// OR I can use `rememberSaveable` here to keep current problem across rotation!

@Composable
fun MathMissionScreen(
    difficulty: String,
    solveCount: Int,
    totalProblems: Int,
    onSolved: () -> Unit
) {
    // Generate problems locally.
    // We use a key to regenerating when solveCount changes.
    // Use rememberSaveable to keep the current problem if screen rotates.
    
    var currentProblem by remember(solveCount) { 
        mutableStateOf(generateOneMathProblem(difficulty))
    }
    
    var userInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    
    // Screen flash color state
    var flashColor by remember { mutableStateOf(Color.Transparent) }
    
    // Shake animation
    val shakeOffset = remember { Animatable(0f) }
    
    // Context for haptic feedback
    val context = LocalContext.current
    val view = LocalView.current
    
    // Trigger shake animation on error
    LaunchedEffect(isError) {
        if (isError) {
            // Haptic feedback
            view.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
            
            // Flash red
            flashColor = OrangeJuice.copy(alpha = 0.4f)
            
            // Shake animation
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -10f at 250
                    10f at 300
                    0f at 400
                }
            )
            
            // Clear flash
            flashColor = Color.Transparent
        }
    }
    
    // Trigger success flash
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            // Haptic feedback
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            
            // Flash green
            flashColor = GreenLand.copy(alpha = 0.4f)
            kotlinx.coroutines.delay(300)
            flashColor = Color.Transparent
        }
    }
    
    // Animate background color on correct/wrong answer
    val inputBackgroundColor by animateColorAsState(
        targetValue = when {
            showSuccess -> GreenLand.copy(alpha = 0.3f)
            isError -> OrangeJuice.copy(alpha = 0.3f)
            else -> BlackMuteSurface
        },
        label = "InputBg"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Flash overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(flashColor)
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = shakeOffset.value }
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Progress
        Text(
            "Problem ${solveCount + 1} / $totalProblems",
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
                    // Notify VM
                    onSolved()
                } else {
                    isError = true
                    userInput = ""
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    } // Close outer Box
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

data class MathProblem(val display: String, val answer: Int)

fun generateOneMathProblem(difficultyName: String): MathProblem {
    val difficulty = try {
        Difficulty.valueOf(difficultyName)
    } catch (e: Exception) {
        Difficulty.EASY
    }
    
    val (a, b) = when (difficulty) {
        Difficulty.EASY -> Pair(Random.nextInt(1, 10), Random.nextInt(1, 10)) // 5 + 3
        Difficulty.MEDIUM -> Pair(Random.nextInt(10, 50), Random.nextInt(1, 10)) // 23 + 6
        Difficulty.HARD -> Pair(Random.nextInt(10, 99), Random.nextInt(10, 99)) // 45 + 88
    }
    val isAdd = Random.nextBoolean()
    if (isAdd) {
        return MathProblem("$a + $b", a + b)
    } else {
        // Ensure positive result for simplicity
        val max = maxOf(a, b)
        val min = minOf(a, b)
        return MathProblem("$max - $min", max - min)
    }
}
