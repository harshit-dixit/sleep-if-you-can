package com.infusion.sleepifyoucan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.os.Parcelable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Clear
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import com.infusion.sleepifyoucan.data.Difficulty
import com.infusion.sleepifyoucan.data.MissionConfig
import com.infusion.sleepifyoucan.ui.theme.*
import kotlin.random.Random

@Composable
fun MathMissionScreen(
    difficulty: String,
    solveCount: Int,
    totalProblems: Int,
    onSolved: () -> Unit
) {
    // Generate a new problem only when solveCount changes (not on every recomposition/rotation).
    // Using remember keyed on solveCount: regenerates problem on each solve, stable for recompositions.
    // (MathProblem cannot be rememberSaveable'd without a Parcel/Saver — key-based remember is idiomatic here.)
    val currentProblem by remember(solveCount) { 
        mutableStateOf(generateOneMathProblem(difficulty))
    }
    
    var userInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    
    // Screen flash color state
    var flashColor by remember { mutableStateOf(Clear) }
    
    // Shake animation
    val shakeOffset = remember { Animatable(0f) }
    
    val view = LocalView.current
    
    // Trigger shake animation on error
    LaunchedEffect(isError) {
        if (isError) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
            flashColor = DustyRose.copy(alpha = 0.4f)
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
            flashColor = Clear
        }
    }
    
    // Trigger success flash
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            flashColor = Sage.copy(alpha = 0.4f)
            kotlinx.coroutines.delay(300)
            flashColor = Clear
        }
    }
    
    val inputBackgroundColor by animateColorAsState(
        targetValue = when {
            showSuccess -> Sage.copy(alpha = 0.3f)
            isError -> DustyRose.copy(alpha = 0.3f)
            else -> Espresso
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
            
            // Progress — clamped so it never shows "6/5" etc.
            val displaySolveCount = solveCount.coerceAtMost(totalProblems)
            Text(
                "Problem ${displaySolveCount + 1} / $totalProblems",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier.semantics { contentDescription = "Problem ${displaySolveCount + 1} of $totalProblems" }
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Problem Display
            Text(
                text = currentProblem.display,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Terracotta,
                modifier = Modifier.semantics { contentDescription = "Math problem: ${currentProblem.display}" }
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Input Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(inputBackgroundColor, RoundedCornerShape(16.dp))
                    .semantics { contentDescription = if (userInput.isEmpty()) "Enter your answer" else "Current answer: $userInput" },
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
                    val answer = userInput.toIntOrNull()
                    if (answer == currentProblem.answer) {
                        showSuccess = true
                        isError = false
                        userInput = ""
                        onSolved()
                    } else {
                        isError = true
                        showSuccess = false
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
            val description = when (key) {
                "DEL" -> "Delete last digit"
                "OK" -> "Submit answer"
                else -> "Digit $key"
            }
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
                    .fillMaxWidth()
                    .semantics { contentDescription = description },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (key) {
                        "OK" -> Sage
                        "DEL" -> DustyRose.copy(alpha = 0.7f)
                        else -> Espresso
                    },
                    contentColor = TextPrimary
                )
            ) {
                if (key == "DEL") {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete",
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class MathProblem(val display: String, val answer: Int) : Parcelable {
    companion object {
        @JvmField val CREATOR = object : android.os.Parcelable.Creator<MathProblem> {
            override fun createFromParcel(source: android.os.Parcel) =
                MathProblem(source.readString() ?: "", source.readInt())
            override fun newArray(size: Int) = arrayOfNulls<MathProblem>(size)
        }
    }
    override fun writeToParcel(dest: android.os.Parcel, flags: Int) {
        dest.writeString(display)
        dest.writeInt(answer)
    }
    override fun describeContents() = 0
}

fun generateOneMathProblem(difficultyName: String): MathProblem {
    val difficulty = try {
        Difficulty.valueOf(difficultyName)
    } catch (e: Exception) {
        Difficulty.EASY
    }
    
    val (a, b) = when (difficulty) {
        Difficulty.EASY -> Pair(Random.nextInt(1, 10), Random.nextInt(1, 10))
        Difficulty.MEDIUM -> Pair(Random.nextInt(10, 50), Random.nextInt(1, 10))
        Difficulty.HARD -> Pair(Random.nextInt(10, 99), Random.nextInt(10, 99))
    }
    val isAdd = Random.nextBoolean()
    return if (isAdd) {
        MathProblem("$a + $b", a + b)
    } else {
        val max = maxOf(a, b)
        val min = minOf(a, b)
        MathProblem("$max - $min", max - min)
    }
}
