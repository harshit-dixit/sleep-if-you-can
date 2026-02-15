package com.infusion.sleepifyoucan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.infusion.sleepifyoucan.data.*
import com.infusion.sleepifyoucan.ui.theme.*
import com.infusion.sleepifyoucan.utils.EvilModeHelper

/**
 * Settings screen — displayed as a tab, no separate Scaffold/TopBar.
 * Bottom nav remains visible and functional.
 */
@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onMissionAudioChange: (MissionAudioBehavior) -> Unit,
    onEscapeModeChange: (EscapePreventionMode) -> Unit,
    onVolumeEscalationChange: (Boolean) -> Unit
) {
    var showAudioDialog by remember { mutableStateOf(false) }
    var showEscapeDialog by remember { mutableStateOf(false) }
    var showEvilModeWarning by remember { mutableStateOf(false) }
    var pendingEvilMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        // ALARM BEHAVIOR Section
        SettingsSection(title = "Alarm Behavior") {
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Sound during mission",
                subtitle = preferences.missionAudioBehavior.displayName(),
                onClick = { showAudioDialog = true }
            )

            HorizontalDivider(color = GlassBorder)

            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Escape prevention",
                subtitle = preferences.escapePreventionMode.displayName(),
                onClick = { showEscapeDialog = true }
            )

            HorizontalDivider(color = GlassBorder)

            SettingsToggleItem(
                icon = Icons.Default.VolumeUp,
                title = "Gradual volume increase",
                subtitle = "Start quiet, get louder over time",
                checked = preferences.volumeEscalation,
                onCheckedChange = onVolumeEscalationChange
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // MISSIONS Section
        SettingsSection(title = "Missions") {
            SettingsItem(
                icon = Icons.Default.Extension,
                title = "Default mission",
                subtitle = preferences.defaultMissionType,
                onClick = { /* TODO: Mission picker */ }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ABOUT Section
        SettingsSection(title = "About") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "1.0.0",
                onClick = { }
            )

            HorizontalDivider(color = GlassBorder)

            SettingsItem(
                icon = Icons.Default.Star,
                title = "Rate on Play Store",
                subtitle = "Help us improve!",
                onClick = { /* TODO: Launch Play Store */ }
            )

            HorizontalDivider(color = GlassBorder)

            SettingsItem(
                icon = Icons.Default.Email,
                title = "Send feedback",
                subtitle = "Report bugs or suggest features",
                onClick = { /* TODO: Feedback form */ }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Audio Behavior Dialog
    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            title = { Text("Sound during mission", color = TextPrimary) },
            containerColor = DarkBlue,
            text = {
                Column {
                    MissionAudioBehavior.entries.forEach { behavior ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMissionAudioChange(behavior)
                                    showAudioDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.missionAudioBehavior == behavior,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Coral,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(behavior.displayName(), color = TextPrimary)
                                Text(
                                    behavior.description(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { }
        )
    }

    // Escape Mode Dialog
    if (showEscapeDialog) {
        AlertDialog(
            onDismissRequest = { showEscapeDialog = false },
            title = { Text("Escape prevention", color = TextPrimary) },
            containerColor = DarkBlue,
            text = {
                Column {
                    EscapePreventionMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (mode == EscapePreventionMode.EVIL) {
                                        pendingEvilMode = true
                                        showEscapeDialog = false
                                        showEvilModeWarning = true
                                    } else {
                                        onEscapeModeChange(mode)
                                        showEscapeDialog = false
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.escapePreventionMode == mode,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (mode == EscapePreventionMode.EVIL) OrangeAccent else Coral,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    mode.displayName(),
                                    color = if (mode == EscapePreventionMode.EVIL) OrangeAccent else TextPrimary
                                )
                                Text(
                                    mode.description(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { }
        )
    }

    // Evil Mode Warning Dialog
    if (showEvilModeWarning) {
        AlertDialog(
            onDismissRequest = {
                showEvilModeWarning = false
                pendingEvilMode = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("😈 ", style = MaterialTheme.typography.headlineSmall)
                    Text("Evil Mode", color = OrangeAccent)
                }
            },
            containerColor = DarkBlue,
            text = {
                Text(
                    EvilModeHelper.getPermissionExplanation(),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEscapeModeChange(EscapePreventionMode.EVIL)
                        showEvilModeWarning = false
                        pendingEvilMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) {
                    Text("Enable Evil Mode")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEvilModeWarning = false
                        pendingEvilMode = false
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Coral,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Icon(Icons.Default.KeyboardArrowRight, null, tint = TextDisabled)
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Coral,
                checkedTrackColor = Coral.copy(alpha = 0.3f),
                uncheckedThumbColor = TextDisabled,
                uncheckedTrackColor = GlassWhite
            )
        )
    }
}
