package com.infusion.sleepifyoucan.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infusion.sleepifyoucan.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Streak celebration screen showing current streak count, weekly calendar,
 * and motivational message. Based on wireframe reference image.
 */
@Composable
fun StreakScreen(
    currentStreak: Int,
    weeklyProgress: Map<Long, Boolean>,
    motivationalMessage: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BlackMute)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Animated Flame Icon
        AnimatedFlameIcon(
            modifier = Modifier.size(160.dp),
            isOnFire = currentStreak >= 3
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Streak Count - Large, bold number
        Text(
            text = "$currentStreak",
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        
        Text(
            text = "days streak",
            style = MaterialTheme.typography.headlineSmall,
            color = YellowSand
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Weekly Calendar Card
        WeeklyCalendarCard(
            weeklyProgress = weeklyProgress,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Motivational Message
        Text(
            text = motivationalMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Animated flame icon that pulses and glows when streak is active.
 */
@Composable
fun AnimatedFlameIcon(
    modifier: Modifier = Modifier,
    isOnFire: Boolean = true
) {
    // Pulse animation for the flame
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOnFire) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isOnFire) 0.6f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        modifier = modifier.scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect behind the flame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val gradient = Brush.radialGradient(
                colors = listOf(
                    YellowSand.copy(alpha = glowAlpha),
                    OrangeJuice.copy(alpha = glowAlpha * 0.5f),
                    Color.Transparent
                ),
                center = center,
                radius = size.minDimension / 1.5f
            )
            drawCircle(brush = gradient)
        }
        
        // Flame icon using Canvas
        Canvas(modifier = Modifier.fillMaxSize(0.6f)) {
            val width = size.width
            val height = size.height
            
            // Main flame path
            val flamePath = Path().apply {
                moveTo(width * 0.5f, 0f)
                cubicTo(
                    width * 0.8f, height * 0.3f,
                    width * 0.9f, height * 0.6f,
                    width * 0.5f, height
                )
                cubicTo(
                    width * 0.1f, height * 0.6f,
                    width * 0.2f, height * 0.3f,
                    width * 0.5f, 0f
                )
                close()
            }
            
            // Draw flame with gradient
            val flameGradient = Brush.verticalGradient(
                colors = listOf(YellowSand, OrangeJuice, OrangeJuice.copy(alpha = 0.8f))
            )
            drawPath(flamePath, flameGradient, style = Fill)
            
            // Inner flame (lighter color)
            val innerFlamePath = Path().apply {
                moveTo(width * 0.5f, height * 0.25f)
                cubicTo(
                    width * 0.65f, height * 0.45f,
                    width * 0.7f, height * 0.65f,
                    width * 0.5f, height * 0.85f
                )
                cubicTo(
                    width * 0.3f, height * 0.65f,
                    width * 0.35f, height * 0.45f,
                    width * 0.5f, height * 0.25f
                )
                close()
            }
            
            val innerGradient = Brush.verticalGradient(
                colors = listOf(YellowSandLight, YellowSand)
            )
            drawPath(innerFlamePath, innerGradient, style = Fill)
        }
    }
}

/**
 * Weekly calendar card showing M-S with checkmarks for completed days.
 */
@Composable
fun WeeklyCalendarCard(
    weeklyProgress: Map<Long, Boolean>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = BlackMuteSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Day letters row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                days.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Day indicators row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0..6) {
                    val date = startOfWeek.plusDays(i.toLong())
                    val epochDay = date.toEpochDay()
                    val isCompleted = weeklyProgress[epochDay] ?: false
                    val isToday = date == today
                    val isFuture = date.isAfter(today)
                    
                    DayIndicator(
                        dayNumber = date.dayOfMonth,
                        isCompleted = isCompleted,
                        isToday = isToday,
                        isFuture = isFuture
                    )
                }
            }
        }
    }
}

/**
 * Individual day indicator showing completion status.
 */
@Composable
fun DayIndicator(
    dayNumber: Int,
    isCompleted: Boolean,
    isToday: Boolean,
    isFuture: Boolean
) {
    val backgroundColor = when {
        isCompleted && isToday -> PurpleNight
        isCompleted -> GreenLand.copy(alpha = 0.8f)
        isToday -> PurpleNight.copy(alpha = 0.3f)
        isFuture -> BlackMuteDark
        else -> BlackMuteDark
    }
    
    val contentColor = when {
        isCompleted -> TextPrimary
        isFuture -> TextDisabled
        else -> TextSecondary
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Text(
                text = "✓",
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        } else {
            Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
