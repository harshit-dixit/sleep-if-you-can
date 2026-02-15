package com.infusion.sleepifyoucan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.infusion.sleepifyoucan.data.SleepReport
import com.infusion.sleepifyoucan.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun StatisticsDashboard(
    sleepReports: List<SleepReport>,
    currentStreak: Int,
    weeklyProgress: Map<Long, Boolean>,
    totalAlarmsCompleted: Int,
    averageSleepScore: Float
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GradientPrimary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            ScaleFadeAnimation(visible = true) {
                Text(
                    text = "Your Statistics",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Key Metrics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Current Streak",
                    value = currentStreak.toString(),
                    unit = "days",
                    gradient = GradientSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Avg Sleep Score",
                    value = averageSleepScore.roundToInt().toString(),
                    unit = "/100",
                    gradient = GradientAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Alarms Completed",
                    value = totalAlarmsCompleted.toString(),
                    unit = "total",
                    gradient = GradientWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Sleep Sessions",
                    value = sleepReports.size.toString(),
                    unit = "tracked",
                    gradient = GradientSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Weekly Progress
        item {
            WeeklyProgressCard(weeklyProgress)
        }

        // Sleep Quality Distribution
        item {
            SleepQualityChart(sleepReports)
        }

        // Recent Sleep Reports
        if (sleepReports.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Sleep Sessions",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(sleepReports.take(5)) { report ->
                StaggeredFadeIn(index = sleepReports.indexOf(report)) {
                    SleepReportCard(report)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient, RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WeeklyProgressCard(weeklyProgress: Map<Long, Boolean>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyLight),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Progress",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simple weekly chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                val calendar = Calendar.getInstance()

                days.forEachIndexed { index, day ->
                    val dayOfWeek = (calendar.firstDayOfWeek + index) % 7
                    val hasCompleted = weeklyProgress[dayOfWeek.toLong()] ?: false

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasCompleted) GradientSuccess
                                    else OceanBlue.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasCompleted) "✓" else "○",
                                color = if (hasCompleted) TextPrimary else TextTertiary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepQualityChart(sleepReports: List<SleepReport>) {
    val qualityCounts = sleepReports.groupBy { report ->
        when {
            report.score >= 80 -> "Excellent"
            report.score >= 60 -> "Good"
            report.score >= 40 -> "Fair"
            else -> "Poor"
        }
    }.mapValues { it.value.size }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyLight),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sleep Quality Distribution",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            val qualities = listOf("Excellent", "Good", "Fair", "Poor")
            val colors = listOf(Mint, SkyBlue, Gold, Coral)

            qualities.forEachIndexed { index, quality ->
                val count = qualityCounts[quality] ?: 0
                val percentage = if (sleepReports.isNotEmpty()) {
                    (count.toFloat() / sleepReports.size * 100).roundToInt()
                } else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(colors[index])
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = quality,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "$count ($percentage%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // Progress bar
                LinearProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colors[index],
                    trackColor = OceanBlue.copy(alpha = 0.3f)
                )

                if (index < qualities.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SleepReportCard(report: SleepReport) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(report.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkBlue),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            report.score >= 80 -> GradientSuccess
                            report.score >= 60 -> GradientWarning
                            else -> GradientAccent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = report.score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = report.summary.lines().firstOrNull() ?: "Sleep session completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 2
                )
            }
        }
    }
}
