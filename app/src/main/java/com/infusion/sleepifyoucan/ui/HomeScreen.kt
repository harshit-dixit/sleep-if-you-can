package com.infusion.sleepifyoucan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infusion.sleepifyoucan.data.Alarm
import com.infusion.sleepifyoucan.ui.theme.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Home screen showing sleep visualization, next alarm, and quick stats.
 * Based on wireframe reference images with moon/stars illustration.
 */
@Composable
fun HomeScreen(
    nextAlarm: Alarm?,
    currentStreak: Int,
    sleepTimeHours: Int = 7,
    sleepTimeMinutes: Int = 30,
    onAlarmClick: () -> Unit,
    onEditSleepTime: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BlackMute)
    ) {
        // Top buttons row
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Statistics button
            IconButton(
                onClick = onStatisticsClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavyLight.copy(alpha = 0.8f))
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Statistics",
                    tint = Coral
                )
            }
            
            // Settings button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavyLight.copy(alpha = 0.8f))
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Moon and Stars Illustration
        MoonStarsIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Sleep Time Display
        SleepTimeDisplay(
            hours = sleepTimeHours,
            minutes = sleepTimeMinutes,
            onEdit = onEditSleepTime
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Alarms Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alarms",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            
            // Mini streak badge
            if (currentStreak > 0) {
                StreakBadge(streakCount = currentStreak)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Next Alarm Card
        if (nextAlarm != null) {
            NextAlarmCard(
                alarm = nextAlarm,
                onClick = onAlarmClick
            )
        } else {
            NoAlarmsCard(onClick = onAlarmClick)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        } // Close Column
    } // Close Box
}

/**
 * Moon and stars illustration for the home screen header.
 */
@Composable
fun MoonStarsIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Stars (small circles)
        val starColor = YellowSand
        val starPositions = listOf(
            Offset(width * 0.15f, height * 0.2f) to 4.dp.toPx(),
            Offset(width * 0.25f, height * 0.35f) to 6.dp.toPx(),
            Offset(width * 0.1f, height * 0.5f) to 3.dp.toPx(),
            Offset(width * 0.85f, height * 0.15f) to 5.dp.toPx(),
            Offset(width * 0.9f, height * 0.35f) to 4.dp.toPx(),
            Offset(width * 0.75f, height * 0.25f) to 7.dp.toPx()
        )
        
        starPositions.forEach { (offset, radius) ->
            drawCircle(
                color = starColor,
                radius = radius,
                center = offset
            )
        }
        
        // Moon (large crescent)
        val moonCenterX = width * 0.5f
        val moonCenterY = height * 0.5f
        val moonRadius = height * 0.35f
        
        // Full moon circle
        val moonGradient = Brush.radialGradient(
            colors = listOf(YellowSandLight, YellowSand),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius
        )
        
        drawCircle(
            brush = moonGradient,
            radius = moonRadius,
            center = Offset(moonCenterX, moonCenterY)
        )
        
        // Crescent shadow (darker circle offset to create crescent effect)
        drawCircle(
            color = BlackMute,
            radius = moonRadius * 0.85f,
            center = Offset(moonCenterX + moonRadius * 0.3f, moonCenterY - moonRadius * 0.2f)
        )
    }
}

/**
 * Sleep time display with hours and minutes.
 */
@Composable
fun SleepTimeDisplay(
    hours: Int,
    minutes: Int,
    onEdit: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Sleep Time",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format("%02d", hours),
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )
                Text(
                    text = "hr",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = String.format("%02d", minutes),
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )
                Text(
                    text = "min",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Edit button
        IconButton(
            onClick = onEdit,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BlackMuteSurface)
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit sleep time",
                tint = TextSecondary
            )
        }
    }
}

/**
 * Mini streak badge for home screen.
 */
@Composable
fun StreakBadge(streakCount: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(YellowSand.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🔥",
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$streakCount",
            style = MaterialTheme.typography.labelLarge,
            color = YellowSand,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Card showing the next upcoming alarm.
 */
@Composable
fun NextAlarmCard(
    alarm: Alarm,
    onClick: () -> Unit
) {
    val timeUntil = calculateTimeUntil(alarm)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = BlackMuteSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Alarm icon with glow
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PurpleNight.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = PurpleNight,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = alarm.label ?: timeUntil,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            // Toggle switch
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = null,  // Controlled by parent
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PurpleNight,
                    checkedTrackColor = PurpleNight.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextDisabled,
                    uncheckedTrackColor = BlackMuteDark
                )
            )
        }
    }
}

/**
 * Placeholder card when no alarms are set.
 */
@Composable
fun NoAlarmsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = BlackMuteSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = TextDisabled,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No alarms set",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tap to create your first alarm",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDisabled
            )
        }
    }
}

/**
 * Calculate time until alarm rings.
 */
private fun calculateTimeUntil(alarm: Alarm): String {
    val now = LocalDateTime.now()
    var alarmTime = now.withHour(alarm.hour).withMinute(alarm.minute).withSecond(0)
    
    if (alarmTime.isBefore(now) || alarmTime.isEqual(now)) {
        alarmTime = alarmTime.plusDays(1)
    }
    
    val hoursUntil = ChronoUnit.HOURS.between(now, alarmTime)
    val minutesUntil = ChronoUnit.MINUTES.between(now, alarmTime) % 60
    
    return when {
        hoursUntil > 0 -> "In ${hoursUntil}h ${minutesUntil}m"
        else -> "In ${minutesUntil}m"
    }
}
