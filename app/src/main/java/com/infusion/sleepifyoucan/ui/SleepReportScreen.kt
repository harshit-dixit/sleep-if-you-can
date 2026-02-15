package com.infusion.sleepifyoucan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infusion.sleepifyoucan.data.SleepReport
import com.infusion.sleepifyoucan.ui.theme.BlackMute
import com.infusion.sleepifyoucan.ui.theme.OrangeAccent
import androidx.compose.ui.text.TextStyle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SleepReportScreen(
    sleepReports: List<SleepReport>,
    onStartSleepTracking: () -> Unit,
    onStopSleepTracking: () -> Unit,
    isTrackingActive: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackMute)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Sleep Reports",
            style = MaterialTheme.typography.headlineLarge,
            color = OrangeAccent,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sleep tracking controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.DarkGray
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isTrackingActive) "Sleep Tracking Active" else "Sleep Tracking Inactive",
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Text(
                        text = if (isTrackingActive) "Tracking your sleep patterns..." else "Tap to start tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }

                Button(
                    onClick = if (isTrackingActive) onStopSleepTracking else onStartSleepTracking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTrackingActive) androidx.compose.ui.graphics.Color.Red else OrangeAccent,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    Text(if (isTrackingActive) "Stop" else "Start")
                }
            }
        }

        // Sleep reports list
        if (sleepReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No sleep reports yet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = androidx.compose.ui.graphics.Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Start tracking your sleep to see detailed reports and insights",
                        style = MaterialTheme.typography.bodyLarge,
                        color = androidx.compose.ui.graphics.Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sleepReports) { report ->
                    SleepReportCard(report)
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
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.DarkGray
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date and score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.titleMedium,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold
                )

                // Score indicator
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                report.score >= 80 -> androidx.compose.ui.graphics.Color.Green
                                report.score >= 60 -> androidx.compose.ui.graphics.Color.Yellow
                                else -> androidx.compose.ui.graphics.Color.Red
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = report.score.toString(),
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary
            Text(
                text = report.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.White,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Recommendations
            if (report.recommendations.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "💡 ${report.recommendations}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.LightGray,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
