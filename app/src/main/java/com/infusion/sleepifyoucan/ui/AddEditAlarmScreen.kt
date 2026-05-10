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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.core.content.IntentCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.infusion.sleepifyoucan.data.Alarm
import com.infusion.sleepifyoucan.data.MissionConfig
import com.infusion.sleepifyoucan.data.Difficulty
import com.infusion.sleepifyoucan.ui.theme.*
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
    
    // Mission Fields
    var missionType by remember { mutableStateOf<MissionType>(getMissionType(alarm?.missionConfig)) }
    
    // Snooze Fields
    var isSnoozeEnabled by remember { mutableStateOf(alarm?.isSnoozeEnabled ?: true) }
    var snoozeDuration by remember { mutableIntStateOf(alarm?.snoozeDuration ?: 5) }
    
    // Mission Config State
    var shakeTarget by remember { mutableIntStateOf((alarm?.missionConfig as? MissionConfig.Shake)?.targetShakes ?: 20) }
    var mathDifficulty by remember { mutableStateOf((alarm?.missionConfig as? MissionConfig.Math)?.difficulty ?: Difficulty.EASY) }
    var mathCount by remember { mutableIntStateOf((alarm?.missionConfig as? MissionConfig.Math)?.problemCount ?: 3) }
    var typingWord by remember { mutableStateOf((alarm?.missionConfig as? MissionConfig.Typing)?.targetWord ?: "I WILL WAKE UP") }
    var typingCase by remember { mutableStateOf((alarm?.missionConfig as? MissionConfig.Typing)?.caseSensitive ?: false) }
    var squatCount by remember { mutableIntStateOf((alarm?.missionConfig as? MissionConfig.Squat)?.targetSquats ?: 10) }
    var stepCount by remember { mutableIntStateOf((alarm?.missionConfig as? MissionConfig.Step)?.targetSteps ?: 50) }
    var photoLabel by remember { mutableStateOf((alarm?.missionConfig as? MissionConfig.Photo)?.requiredObject ?: "Laptop") }
    var barcodeValue by remember { mutableStateOf((alarm?.missionConfig as? MissionConfig.Barcode)?.expectedBarcode ?: "") }
    
    // Try Mission Dialog
    var showTryMission by remember { mutableStateOf(false) }
    
    // Barcode Scanner
    val barcodeLauncher = rememberLauncherForActivityResult(com.journeyapps.barcodescanner.ScanContract()) { result ->
        if (result.contents != null) {
            barcodeValue = result.contents
        }
    }

    // Ringtone Picker
    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.let { intent ->
                IntentCompat.getParcelableExtra(intent, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            if (uri != null) {
                ringtoneUri = uri.toString()
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) { e.printStackTrace() }
            } else {
                ringtoneUri = null
            }
        }
    }

    // Build config helper
    fun buildMissionConfig(): MissionConfig = when (missionType) {
        MissionType.SHAKE -> MissionConfig.Shake(shakeTarget)
        MissionType.MATH -> MissionConfig.Math(mathDifficulty, mathCount)
        MissionType.MEMORY -> MissionConfig.Memory(4)
        MissionType.TYPING -> MissionConfig.Typing(typingWord, typingCase)
        MissionType.SQUAT -> MissionConfig.Squat(squatCount)
        MissionType.STEP -> MissionConfig.Step(stepCount)
        MissionType.PHOTO -> MissionConfig.Photo(photoLabel)
        MissionType.BARCODE -> MissionConfig.Barcode(if (barcodeValue.isBlank()) null else barcodeValue)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (alarm == null) "New Alarm" else "Edit Alarm",
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
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
                            missionConfig = buildMissionConfig(),
                            isEnabled = true
                        )
                        onSave(newAlarm)
                    }) {
                        Icon(Icons.Default.Check, "Save", tint = Coral)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepNavy
                )
            )
        },
        containerColor = DeepNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Time Picker ──
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = DarkBlue,
                        selectorColor = Coral,
                        containerColor = Color.Transparent,
                        clockDialSelectedContentColor = TextOnAccent,
                        clockDialUnselectedContentColor = TextSecondary,
                        periodSelectorSelectedContainerColor = Coral.copy(alpha = 0.3f),
                        periodSelectorSelectedContentColor = Coral,
                        periodSelectorUnselectedContentColor = TextSecondary,
                        timeSelectorSelectedContainerColor = Coral.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = TextPrimary,
                        timeSelectorUnselectedContainerColor = GlassWhite,
                        timeSelectorUnselectedContentColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Days ──
            WeekDaySelector(selectedDays = daysOfWeek, onSelectionChanged = { daysOfWeek = it })

            Spacer(modifier = Modifier.height(16.dp))

            // ── Settings Card ──
            GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Ringtone", color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(if (ringtoneUri == null) "Default" else "Custom", color = Coral, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(color = GlassBorder)

                // Vibrate
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Vibration, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Vibrate", color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isVibrate,
                        onCheckedChange = { isVibrate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Coral,
                            checkedTrackColor = Coral.copy(0.3f),
                            uncheckedThumbColor = TextDisabled,
                            uncheckedTrackColor = GlassWhite
                        )
                    )
                }

                HorizontalDivider(color = GlassBorder)

                // Label
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Coral,
                        focusedLabelColor = Coral,
                        unfocusedLabelColor = TextSecondary
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Snooze ──
            SectionLabel("Snooze")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Snooze", color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isSnoozeEnabled,
                        onCheckedChange = { isSnoozeEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Coral,
                            checkedTrackColor = Coral.copy(0.3f),
                            uncheckedThumbColor = TextDisabled,
                            uncheckedTrackColor = GlassWhite
                        )
                    )
                }
                
                if (isSnoozeEnabled) {
                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 8.dp))
                    Text("Duration", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 5, 10, 20).forEach { mins ->
                            FilterChip(
                                selected = snoozeDuration == mins,
                                onClick = { snoozeDuration = mins },
                                label = { Text("${mins}m") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Coral.copy(0.2f),
                                    selectedLabelColor = Coral,
                                    containerColor = GlassWhite,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = GlassBorder,
                                    selectedBorderColor = Coral.copy(0.5f),
                                    enabled = true,
                                    selected = snoozeDuration == mins
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Mission Selector (Grid) ──
            SectionLabel("Mission")
            
            // Replaced LazyVerticalGrid with simple Column/Row to avoid nesting scroll issues and cut-off
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MissionType.entries.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { type ->
                            Box(modifier = Modifier.weight(1f)) {
                                MissionCard(
                                    type = type,
                                    isSelected = missionType == type,
                                    onClick = { missionType = type }
                                )
                            }
                        }
                        // Fill empty space if odd number of items
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Mission Configuration ──
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${missionType.emoji} ${missionType.displayName} Settings",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Try Mission Button
                    TextButton(
                        onClick = { showTryMission = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Mint)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Try it", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (missionType) {
                    MissionType.SHAKE -> {
                        CountInputWithSlider(
                            label = "Target Shakes",
                            value = shakeTarget,
                            onValueChange = { shakeTarget = it },
                            range = 5f..100f,
                            steps = 19
                        )
                    }
                    MissionType.MATH -> {
                        CountInputWithSlider(
                            label = "Problem Count",
                            value = mathCount,
                            onValueChange = { mathCount = it },
                            range = 1f..10f,
                            steps = 9
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Difficulty", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Difficulty.entries.forEach { diff ->
                                FilterChip(
                                    selected = mathDifficulty == diff,
                                    onClick = { mathDifficulty = diff },
                                    label = { Text(diff.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Coral.copy(0.2f),
                                        selectedLabelColor = Coral,
                                        containerColor = GlassWhite,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = GlassBorder,
                                        selectedBorderColor = Coral.copy(0.5f),
                                        enabled = true,
                                        selected = mathDifficulty == diff
                                    )
                                )
                            }
                        }
                    }
                    MissionType.MEMORY -> {
                        Text("Memory Match — 4×4 Grid", color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("Find all matching pairs to dismiss the alarm", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    MissionType.TYPING -> {
                        OutlinedTextField(
                            value = typingWord,
                            onValueChange = { typingWord = it },
                            label = { Text("Phrase to Type", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Coral,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = Coral,
                                focusedLabelColor = Coral,
                                unfocusedLabelColor = TextSecondary
                            ),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Case Sensitive", color = TextPrimary, modifier = Modifier.weight(1f))
                            Switch(
                                checked = typingCase,
                                onCheckedChange = { typingCase = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Coral,
                                    checkedTrackColor = Coral.copy(0.3f),
                                    uncheckedThumbColor = TextDisabled,
                                    uncheckedTrackColor = GlassWhite
                                )
                            )
                        }
                    }
                    MissionType.SQUAT -> {
                        CountInputWithSlider(
                            label = "Target Squats",
                            value = squatCount,
                            onValueChange = { squatCount = it },
                            range = 5f..50f,
                            steps = 9
                        )
                    }
                    MissionType.STEP -> {
                        CountInputWithSlider(
                            label = "Target Steps",
                            value = stepCount,
                            onValueChange = { stepCount = it },
                            range = 10f..200f,
                            steps = 19
                        )
                    }
                    MissionType.PHOTO -> {
                        OutlinedTextField(
                            value = photoLabel,
                            onValueChange = { photoLabel = it },
                            label = { Text("Object to Photograph (e.g. Cup)", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Coral,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = Coral,
                                focusedLabelColor = Coral,
                                unfocusedLabelColor = TextSecondary
                            ),
                            singleLine = true
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "ML Kit will try to identify this object",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                    MissionType.BARCODE -> {
                        if (barcodeValue.isNotEmpty()) {
                            Text("Registered: $barcodeValue", color = Mint, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(
                            onClick = {
                                val options = com.journeyapps.barcodescanner.ScanOptions().apply {
                                    setPrompt("Scan barcode to register")
                                    setBeepEnabled(true)
                                    setOrientationLocked(false)
                                }
                                barcodeLauncher.launch(options)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Coral),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (barcodeValue.isEmpty()) "Scan Barcode" else "Rescan Barcode")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Save Alarm Button
            Button(
                onClick = {
                    val newAlarm = alarm?.copy(
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        daysOfWeek = daysOfWeek,
                        label = label,
                        isVibrate = isVibrate,
                        ringtoneUri = ringtoneUri,
                        missionConfig = buildMissionConfig(),
                        isSnoozeEnabled = isSnoozeEnabled,
                        snoozeDuration = snoozeDuration,
                        isEnabled = true
                    ) ?: Alarm(
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        daysOfWeek = daysOfWeek,
                        label = label,
                        isVibrate = isVibrate,
                        ringtoneUri = ringtoneUri,
                        missionConfig = buildMissionConfig(),
                        isSnoozeEnabled = isSnoozeEnabled,
                        snoozeDuration = snoozeDuration,
                        isEnabled = true
                    )
                    onSave(newAlarm)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
            ) {
                Text(
                    text = "Save Alarm",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextOnAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // ── Try Mission Dialog ──
    if (showTryMission) {
        TryMissionDialog(
            missionType = missionType,
            config = buildMissionConfig(),
            onDismiss = { showTryMission = false }
        )
    }
}

// ── Mission Card for Grid ──
@Composable
fun MissionCard(
    type: MissionType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    GlassCardAccent(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        isSelected = isSelected,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Icon(
                imageVector = type.toIcon(),
                contentDescription = null,
                tint = if (isSelected) Coral else TextSecondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                type.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Coral else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                type.description,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

// ── Count Input with Slider + Manual TextField ──
@Composable
fun CountInputWithSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = textValue,
                onValueChange = { text ->
                    textValue = text
                    text.toIntOrNull()?.let { parsed ->
                        val clamped = parsed.coerceIn(range.start.toInt(), range.endInclusive.toInt())
                        onValueChange(clamped)
                    }
                },
                modifier = Modifier.width(80.dp).height(50.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    color = TextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral,
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = Coral
                )
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = {
                val newVal = it.toInt()
                onValueChange(newVal)
                textValue = newVal.toString()
            },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Coral,
                activeTrackColor = Coral,
                inactiveTrackColor = GlassWhite
            )
        )
    }
}

// ── Try Mission Dialog (full-screen preview) ──
@Composable
fun TryMissionDialog(
    missionType: MissionType,
    config: MissionConfig,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavy)
        ) {
            // Mission content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 70.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (missionType) {
                    MissionType.SHAKE -> {
                        val target = (config as MissionConfig.Shake).targetShakes
                        var count by remember { mutableIntStateOf(0) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MissionPreviewInfo(
                                icon = missionType.toIcon(),
                                title = "Shake Preview",
                                description = "Target: $target shakes\nCurrent: $count"
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { 
                                count++
                                if (count >= target) {
                                    // Succeeded
                                }
                            }) {
                                Text("Simulate Shake")
                            }
                        }
                    }
                    MissionType.MATH -> {
                        val cfg = config as MissionConfig.Math
                        var solveCount by remember { mutableIntStateOf(0) }
                        MathMissionScreen(
                            difficulty = cfg.difficulty.name,
                            solveCount = solveCount,
                            totalProblems = cfg.problemCount,
                            onSolved = { solveCount++ }
                        )
                    }
                    MissionType.MEMORY -> {
                        MissionPreviewInfo(
                            icon = missionType.toIcon(),
                            title = "Memory Preview",
                            description = "Match all card pairs on a 4×4 grid.\nThis mission will show a grid of face-down cards — flip two at a time to find matches!"
                        )
                    }
                    MissionType.TYPING -> {
                        val cfg = config as MissionConfig.Typing
                        var input by remember { mutableStateOf("") }
                        TypingMissionScreen(
                            targetWord = cfg.targetWord,
                            currentInput = input,
                            caseSensitive = cfg.caseSensitive,
                            onInputChange = { input = it }
                        )
                    }
                    MissionType.SQUAT -> {
                        val target = (config as MissionConfig.Squat).targetSquats
                        var count by remember { mutableIntStateOf(0) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SquatMissionScreen(
                                targetSquats = target,
                                currentSquats = count,
                                onSquatDetected = { /* No-op in preview */ }
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { count++ }) {
                                Text("Simulate Squat")
                            }
                        }
                    }
                    MissionType.STEP -> {
                        val target = (config as MissionConfig.Step).targetSteps
                        var count by remember { mutableIntStateOf(0) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            StepMissionScreen(
                                targetSteps = target,
                                currentSteps = count,
                                onStepDetected = { /* No-op in preview */ }
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { count++ }) {
                                Text("Simulate Step")
                            }
                        }
                    }
                    MissionType.PHOTO -> {
                        val obj = (config as MissionConfig.Photo).requiredObject
                        PhotoMissionScreen(
                            requiredObject = obj,
                            onPhotoTaken = { /* preview only */ }
                        )
                    }
                    MissionType.BARCODE -> {
                        val expected = (config as MissionConfig.Barcode).expectedBarcode
                        BarcodeMissionScreen(
                            expectedBarcode = expected,
                            onBarcodeScanned = { _ -> /* preview only */ }
                        )
                    }
                }
            }

            // Close Preview Button — always on top
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .statusBarsPadding(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Error.copy(alpha = 0.9f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Close Preview", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Mission Preview Info (for missions that can't run in preview) ──
@Composable
fun MissionPreviewInfo(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Coral,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(description, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 24.sp)
    }
}

// ── Section Label ──
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = Coral,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 4.dp)
    )
}

// ── WeekDay Selector ──
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
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Coral.copy(0.2f) else GlassWhite)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Coral else GlassBorder,
                        shape = CircleShape
                    )
                    .clickable {
                        val newSelection = if (isSelected) selectedDays - dayInt else selectedDays + dayInt
                        onSelectionChanged(newSelection)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayStr,
                    color = if (isSelected) Coral else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ── Mission Type Enum with Display Info ──
enum class MissionType(val displayName: String, val description: String) {
    SHAKE("Shake", "Shake your phone"),
    MATH("Math", "Solve equations"),
    MEMORY("Memory", "Match card pairs"),
    TYPING("Typing", "Type a phrase"),
    SQUAT("Squat", "Do squats"),
    STEP("Steps", "Walk around"),
    PHOTO("Photo", "Take a photo"),
    BARCODE("Barcode", "Scan a code")
}

fun MissionType.toIcon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    MissionType.SHAKE -> Icons.Default.Vibration
    MissionType.MATH -> Icons.Default.Calculate
    MissionType.MEMORY -> Icons.Default.Extension
    MissionType.TYPING -> Icons.Default.Keyboard
    MissionType.SQUAT -> Icons.Default.DirectionsWalk
    MissionType.STEP -> Icons.Default.DirectionsWalk
    MissionType.PHOTO -> Icons.Default.CameraAlt
    MissionType.BARCODE -> Icons.Default.QrCodeScanner
}

fun getMissionType(config: MissionConfig?): MissionType {
    return when (config) {
        is MissionConfig.Shake -> MissionType.SHAKE
        is MissionConfig.Math -> MissionType.MATH
        is MissionConfig.Memory -> MissionType.MEMORY
        is MissionConfig.Typing -> MissionType.TYPING
        is MissionConfig.Squat -> MissionType.SQUAT
        is MissionConfig.Step -> MissionType.STEP
        is MissionConfig.Photo -> MissionType.PHOTO
        is MissionConfig.Barcode -> MissionType.BARCODE
        else -> MissionType.SHAKE
    }
}
