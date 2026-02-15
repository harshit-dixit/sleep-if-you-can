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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Display target word
        Text(
            text = if (caseSensitive) targetWord else targetWord.uppercase(),
            style = TextStyle(
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Input field
        OutlinedTextField(
            value = currentInput,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            ),
            placeholder = {
                Text(
                    "Type here...",
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontSize = 24.sp)
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (caseSensitive) KeyboardType.Text else KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    // Auto-submit when done is pressed
                }
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Case sensitivity indicator
        if (!caseSensitive) {
            Text(
                text = "Case insensitive",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress indicator
        val progress = if (currentInput.isNotEmpty()) {
            val target = if (caseSensitive) targetWord else targetWord.uppercase()
            val userInput = if (caseSensitive) currentInput else currentInput.uppercase()
            
            // Calculate how many characters match
            val matchLength = target.zip(userInput).takeWhile { it.first == it.second }.count()
            matchLength.toFloat() / target.length.toFloat()
        } else 0f
        
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = OrangeAccent,
            trackColor = androidx.compose.ui.graphics.Color.DarkGray
        )
    }
}
