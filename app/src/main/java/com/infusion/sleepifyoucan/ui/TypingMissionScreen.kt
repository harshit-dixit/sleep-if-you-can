package com.infusion.sleepifyoucan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infusion.sleepifyoucan.ui.theme.*

@Composable
fun TypingMissionScreen(
    targetWord: String,
    currentInput: String,
    caseSensitive: Boolean,
    onInputChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Compute target for comparison (respects caseSensitive setting)
    val normalizedTarget = if (caseSensitive) targetWord else targetWord.uppercase()
    val normalizedInput = if (caseSensitive) currentInput else currentInput.uppercase()

    // Submit action — shared between button and keyboard Done key
    val handleSubmit = {
        if (normalizedInput == normalizedTarget) {
            keyboardController?.hide()
            onInputChange(currentInput) // triggers VM which finishes mission
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackMute)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Type the word:",
            style = MaterialTheme.typography.headlineMedium,
            color = OrangeAccent,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Type the word mission" }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Display target word (always shown uppercase when not case-sensitive for clarity)
        Text(
            text = if (caseSensitive) targetWord else targetWord.uppercase(),
            style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Target word: ${if (caseSensitive) targetWord else targetWord.uppercase()}" }
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Input field
        OutlinedTextField(
            value = currentInput,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics { contentDescription = "Type the target word here" },
            textStyle = TextStyle(fontSize = 32.sp, textAlign = TextAlign.Center),
            placeholder = {
                Text("Type here…", textAlign = TextAlign.Center, style = TextStyle(fontSize = 24.sp), color = TextDisabled)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                capitalization = if (caseSensitive) KeyboardCapitalization.None else KeyboardCapitalization.Characters
            ),
            keyboardActions = KeyboardActions(
                onDone = { handleSubmit() }
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeAccent,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Gray,
                focusedTextColor = androidx.compose.ui.graphics.Color.White,
                unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                cursorColor = OrangeAccent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!caseSensitive) {
            Text(
                text = "Case insensitive — typing in UPPERCASE works",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Character-by-character progress bar
        val progress = if (currentInput.isNotEmpty() && normalizedTarget.isNotEmpty()) {
            val matchLength = normalizedTarget.zip(normalizedInput).takeWhile { it.first == it.second }.count()
            matchLength.toFloat() / normalizedTarget.length.toFloat()
        } else 0f

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .semantics { contentDescription = "Typing progress: ${(progress * 100).toInt()} percent" },
            color = OrangeAccent,
            trackColor = androidx.compose.ui.graphics.Color.DarkGray
        )
    }
}
