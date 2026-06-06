package com.infusion.sleepifyoucan.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.infusion.sleepifyoucan.ui.theme.AccentBlue
import com.infusion.sleepifyoucan.ui.theme.AccentBlueSoft
import com.infusion.sleepifyoucan.ui.theme.AccentGreen
import com.infusion.sleepifyoucan.ui.theme.AccentGreenSoft
import com.infusion.sleepifyoucan.ui.theme.Body
import com.infusion.sleepifyoucan.ui.theme.Canvas
import com.infusion.sleepifyoucan.ui.theme.Hairline
import com.infusion.sleepifyoucan.ui.theme.Ink
import com.infusion.sleepifyoucan.ui.theme.Mute
import com.infusion.sleepifyoucan.ui.theme.Surface
import com.infusion.sleepifyoucan.ui.theme.SurfaceCard

data class StartupPermissionUiState(
    val items: List<StartupPermissionUiItem>
) {
    val allGranted: Boolean
        get() = items.all { it.isGranted }

    val missingCount: Int
        get() = items.count { !it.isGranted }
}

data class StartupPermissionUiItem(
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val accentColor: Color = AccentBlue
)

@Composable
fun StartupPermissionScreen(
    state: StartupPermissionUiState,
    onGrantMissingClick: () -> Unit,
    onContinueAnywayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(AccentBlueSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Set up alarm permissions",
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "These let alarms ring on time and open over the lock screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = Body,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.items.forEach { item ->
                    PermissionRow(item)
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Hairline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Button(
                    onClick = onGrantMissingClick,
                    enabled = state.missingCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = Canvas
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(
                        text = if (state.missingCount == 0) "All permissions granted" else "Grant missing permissions",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onContinueAnywayClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Continue anyway",
                        color = Mute,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(item: StartupPermissionUiItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    color = if (item.isGranted) AccentGreenSoft else item.accentColor.copy(alpha = 0.14f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isGranted) Icons.Default.Check else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (item.isGranted) AccentGreen else item.accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = Ink
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = Body
            )
        }
    }
}
