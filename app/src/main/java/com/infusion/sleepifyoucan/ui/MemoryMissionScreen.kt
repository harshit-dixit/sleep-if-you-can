package com.infusion.sleepifyoucan.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infusion.sleepifyoucan.ui.theme.*
import kotlin.math.sqrt

@Composable
fun MemoryMissionScreen(
    state: MissionState.Memory,
    onCardClick: (Int) -> Unit
) {
    val view = LocalView.current
    var flashColor by remember { mutableStateOf(Color.Transparent) }
    var previousMatchedPairs by remember { mutableIntStateOf(state.matchedPairs) }
    
    // Flash green on match
    LaunchedEffect(state.matchedPairs) {
        if (state.matchedPairs > previousMatchedPairs) {
            // Match found!
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            flashColor = GreenLand.copy(alpha = 0.3f)
            kotlinx.coroutines.delay(300)
            flashColor = Color.Transparent
        }
        previousMatchedPairs = state.matchedPairs
    }
    
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
                .background(BlackMute)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "MEMORY MATCH",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = PurpleNight
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Match all pairs to dismiss!",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Dynamic grid columns: sqrt(totalCards) gives the side length of a square grid.
        // Minimum 2 columns to avoid degenerate layouts.
        val columns = sqrt(state.cards.size.toFloat()).toInt().coerceAtLeast(2)
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics { contentDescription = "Memory card grid, ${state.matchedPairs} pairs matched" }
        ) {
            items(state.cards.size) { index ->
                val card = state.cards[index]
                MemoryCardItem(
                    card = card,
                    onClick = { onCardClick(index) },
                    index = index
                )
            }
        }
        } // Close Column
    } // Close outer Box
}

@Composable
fun MemoryCardItem(
    card: Card,
    onClick: () -> Unit,
    index: Int = 0
) {
    val accessibilityDesc = when {
        card.isMatched -> "Card ${index + 1}: matched, ${card.symbol}"
        card.isFlipped -> "Card ${index + 1}: flipped, ${card.symbol}"
        else -> "Card ${index + 1}: face down, tap to flip"
    }
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(400),
        label = "CardFlip"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .semantics { contentDescription = accessibilityDesc }
            .clickable(enabled = !card.isFlipped && !card.isMatched) { onClick() }
    ) {
        if (rotation <= 90f) {
            // Back of card
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = PurpleNight)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("?", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Front of card
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }, // Correct content orientation
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (card.isMatched) GreenLand else Color.White
                )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = card.symbol,
                        fontSize = 32.sp
                    )
                }
            }
        }
    }
}
