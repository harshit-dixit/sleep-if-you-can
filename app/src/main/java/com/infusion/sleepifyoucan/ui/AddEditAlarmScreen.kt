package com.infusion.sleepifyoucan.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infusion.sleepifyoucan.data.Alarm
import com.infusion.sleepifyoucan.data.MissionConfig
import com.infusion.sleepifyoucan.data.Difficulty
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    alarm: Alarm? = null,
    onSave: (Alarm) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    
    // Time State
    val calendar = Calendar.getInstance()
    if (alarm != null) {
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
        calendar.set(Calendar.MINUTE, alarm.minute)
    } else {
        // Default to now + 1 min or just now
    }
    
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    // Alarm Fields
    var daysOfWeek by remember { mutableStateOf(alarm?.daysOfWeek ?: emptyList()) }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var isVibrate by remember { mutableStateOf(alarm?.isVibrate ?: true) }
    var ringtoneUri by remember { mutableStateOf(alarm?.ringtoneUri) }
    var ringtoneName by remember { mutableStateOf("Default") } // Logic to resolve name could be added
    
    // Mission Fields
    var missionType by remember { mutableStateOf<MissionType>(getMissionType(alarm?.missionConfig)) }
    
    // Snooze Fields
    var isSnoozeEnabled by remember { mutableStateOf(alarm?.isSnoozeEnabled ?: true) }
    var snoozeDuration by remember { mutableIntStateOf(alarm?.snoozeDuration ?: 5) }
    
    // Mission Config State
    var shakeTarget by remember { mutableIntStateOf((alarm?.missionConfig as? MissionConfig.Shake)?.targetShakes ?: 20) }
    var mathDifficulty by remember { mutableStateOf((alarm?.missionConfig as? MissionConfig.Math)?.difficulty ?: Difficulty.EASY) }
    var mathCount by remember { mutableIntStateOf((alarm?.missionConfig as? MissionConfig.Math)?.problemCount ?: 3) }

    // Ringtone Picker
    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                ringtoneUri = uri.toString()
                // Take Persistable Permission
                try {
                     context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            } else {
                ringtoneUri = null // Silent
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarm == null) "Add Alarm" else "Edit Alarm") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Construct Logic
                        val config = when (missionType) {
                            MissionType.SHAKE -> MissionConfig.Shake(shakeTarget)
                            MissionType.MATH -> MissionConfig.Math(mathDifficulty, mathCount)
                        }
                        
                        val newAlarm = Alarm(
                            id = alarm?.id ?: 0,
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                            daysOfWeek = daysOfWeek,
                            label = label.ifBlank { null },
                            ringtoneUri = ringtoneUri,
                            isVibrate = isVibrate,
                            isSnoozeEnabled = isSnoozeEnabled,
                            snoozeDuration = snoozeDuration,
                            missionConfig = config,
                            isEnabled = true
                        )
                        onSave(newAlarm)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
             )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimePicker(state = timePickerState)

            Spacer(modifier = Modifier.height(24.dp))

            // Days of Week Selector
            WeekDaySelector(
                selectedDays = daysOfWeek,
                onSelectionChanged = { daysOfWeek = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Ringtone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                                    
                                    val currentUri = if (ringtoneUri != null) Uri.parse(ringtoneUri) else null
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                                }
                                ringtoneLauncher.launch(intent)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ringtone", modifier = Modifier.weight(1f))
                        Text(if (ringtoneUri == null) "Default" else "Selected", color = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Vibrate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibrate", modifier = Modifier.weight(1f))
                        Switch(checked = isVibrate, onCheckedChange = { isVibrate = it })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Label
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Snooze Settings
            Text("Snooze Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Snooze", modifier = Modifier.weight(1f))
                        Switch(checked = isSnoozeEnabled, onCheckedChange = { isSnoozeEnabled = it })
                    }
                    
                    if (isSnoozeEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text("Duration")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 5, 10, 20).forEach { mins ->
                                FilterChip(
                                    selected = snoozeDuration == mins,
                                    onClick = { snoozeDuration = mins },
                                    label = { Text("$mins m") }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mission Selector
            Text("Mission", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        MissionType.entries.forEach { type ->
                             FilterChip(
                                selected = missionType == type,
                                onClick = { missionType = type },
                                label = { Text(type.name) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mission Config UI
                    if (missionType == MissionType.SHAKE) {
                        Text("Target Shakes: $shakeTarget")
                        Slider(
                            value = shakeTarget.toFloat(),
                            onValueChange = { shakeTarget = it.toInt() },
                            valueRange = 5f..100f,
                            steps = 19
                        )
                    } else if (missionType == MissionType.MATH) {
                         Text("Problem Count: $mathCount")
                         Slider(
                            value = mathCount.toFloat(),
                            onValueChange = { mathCount = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 9
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Difficulty")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Difficulty.entries.forEach { diff ->
                                FilterChip(
                                    selected = mathDifficulty == diff,
                                    onClick = { mathDifficulty = diff },
                                    label = { Text(diff.name) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Spacer for bottom navigation/FAB
             Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun WeekDaySelector(
    selectedDays: List<Int>,
    onSelectionChanged: (List<Int>) -> Unit
) {
    val days = listOf(
        Calendar.SUNDAY to "S",
        Calendar.MONDAY to "M",
        Calendar.TUESDAY to "T",
        Calendar.WEDNESDAY to "W",
        Calendar.THURSDAY to "T",
        Calendar.FRIDAY to "F",
        Calendar.SATURDAY to "S"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (dayInt, dayStr) ->
            val isSelected = selectedDays.contains(dayInt)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        val newSelection = if (isSelected) {
                            selectedDays - dayInt
                        } else {
                            selectedDays + dayInt
                        }
                        onSelectionChanged(newSelection)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayStr,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

enum class MissionType {
    SHAKE, MATH
}

fun getMissionType(config: MissionConfig?): MissionType {
    return when (config) {
        is MissionConfig.Shake -> MissionType.SHAKE
        is MissionConfig.Math -> MissionType.MATH
        else -> MissionType.SHAKE
    }
}
