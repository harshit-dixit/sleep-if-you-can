package com.infusion.sleepifyoucan.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import com.infusion.sleepifyoucan.data.Alarm
import com.infusion.sleepifyoucan.data.Difficulty
import com.infusion.sleepifyoucan.data.MissionConfig
import com.infusion.sleepifyoucan.ui.theme.Coral
import com.infusion.sleepifyoucan.ui.theme.DarkBlue
import com.infusion.sleepifyoucan.ui.theme.DeepNavy
import com.infusion.sleepifyoucan.ui.theme.GlassBorder
import com.infusion.sleepifyoucan.ui.theme.GlassCard
import com.infusion.sleepifyoucan.ui.theme.GlassCardAccent
import com.infusion.sleepifyoucan.ui.theme.GlassWhite
import com.infusion.sleepifyoucan.ui.theme.Mint
import com.infusion.sleepifyoucan.ui.theme.Terracotta
import com.infusion.sleepifyoucan.ui.theme.TextDisabled
import com.infusion.sleepifyoucan.ui.theme.TextOnAccent
import com.infusion.sleepifyoucan.ui.theme.TextPrimary
import com.infusion.sleepifyoucan.ui.theme.TextSecondary
import com.infusion.sleepifyoucan.ui.theme.TextTertiary
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    alarm: Alarm? = null,
    defaultMissionType: String = "SHAKE",
    onSave: (Alarm) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

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

    var daysOfWeek by remember { mutableStateOf(alarm?.daysOfWeek ?: emptyList()) }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var isVibrate by remember { mutableStateOf(alarm?.isVibrate ?: true) }
    var ringtoneUri by remember { mutableStateOf(alarm?.ringtoneUri) }
    var isSnoozeEnabled by remember { mutableStateOf(alarm?.isSnoozeEnabled ?: true) }
    var snoozeDuration by remember { mutableIntStateOf(alarm?.snoozeDuration ?: 5) }

    var missionType by remember {
        mutableStateOf(
            alarm?.missionConfig?.let(::getMissionType) ?: missionTypeFromName(defaultMissionType)
        )
    }
    var shakeTarget by remember {
        mutableIntStateOf((alarm?.missionConfig as? MissionConfig.Shake)?.targetShakes ?: 20)
    }
    var mathDifficulty by remember {
        mutableStateOf((alarm?.missionConfig as? MissionConfig.Math)?.difficulty ?: Difficulty.EASY)
    }
    var mathCount by remember {
        mutableIntStateOf((alarm?.missionConfig as? MissionConfig.Math)?.problemCount ?: 3)
    }
    var typingWord by remember {
        mutableStateOf((alarm?.missionConfig as? MissionConfig.Typing)?.targetWord ?: "I WILL WAKE UP")
    }
    var typingCase by remember {
        mutableStateOf((alarm?.missionConfig as? MissionConfig.Typing)?.caseSensitive ?: false)
    }
    var barcodeValue by remember {
        mutableStateOf((alarm?.missionConfig as? MissionConfig.Barcode)?.expectedBarcode ?: "")
    }

    val barcodeLauncher = rememberLauncherForActivityResult(
        com.journeyapps.barcodescanner.ScanContract()
    ) { result ->
        result.contents?.let { barcodeValue = it }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.let { intent ->
                IntentCompat.getParcelableExtra(
                    intent,
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java
                )
            }
            ringtoneUri = uri?.toString()
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
            }
        }
    }

    fun buildMissionConfig(): MissionConfig = when (missionType) {
        MissionType.SHAKE -> MissionConfig.Shake(shakeTarget)
        MissionType.MATH -> MissionConfig.Math(mathDifficulty, mathCount)
        MissionType.TYPING -> MissionConfig.Typing(typingWord.trim().ifBlank { "I WILL WAKE UP" }, typingCase)
        MissionType.BARCODE -> MissionConfig.Barcode(barcodeValue.trim())
    }

    val canSave = missionType != MissionType.BARCODE || barcodeValue.isNotBlank()

    fun saveAlarm() {
        if (!canSave) return
        val newAlarm = alarm?.copy(
            hour = timePickerState.hour,
            minute = timePickerState.minute,
            daysOfWeek = daysOfWeek,
            label = label.ifBlank { null },
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
            label = label.ifBlank { null },
            isVibrate = isVibrate,
            ringtoneUri = ringtoneUri,
            missionConfig = buildMissionConfig(),
            isSnoozeEnabled = isSnoozeEnabled,
            snoozeDuration = snoozeDuration,
            isEnabled = true
        )
        onSave(newAlarm)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (alarm == null) "New Alarm" else "Edit Alarm", color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = ::saveAlarm, enabled = canSave) {
                        Icon(
                            Icons.Default.Check,
                            "Save",
                            tint = if (canSave) Coral else TextDisabled
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
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
            WeekDaySelector(selectedDays = daysOfWeek, onSelectionChanged = { daysOfWeek = it })
            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Default.MusicNote,
                    title = "Ringtone",
                    trailing = if (ringtoneUri == null) "Default" else "Custom",
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            )
                            val currentUri = ringtoneUri?.let(Uri::parse)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                        }
                        ringtoneLauncher.launch(intent)
                    }
                )

                HorizontalDivider(color = GlassBorder)

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

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = defaultTextFieldColors(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
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
            SectionLabel("Mission")
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
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(missionType.toIcon(), null, tint = Terracotta, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${missionType.displayName} Settings",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (missionType) {
                    MissionType.SHAKE -> CountInputWithSlider(
                        label = "Target Shakes",
                        value = shakeTarget,
                        onValueChange = { shakeTarget = it },
                        range = 5f..80f,
                        steps = 14
                    )

                    MissionType.MATH -> {
                        CountInputWithSlider(
                            label = "Problem Count",
                            value = mathCount,
                            onValueChange = { mathCount = it },
                            range = 1f..10f,
                            steps = 8
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Difficulty.entries.forEach { difficulty ->
                                FilterChip(
                                    selected = mathDifficulty == difficulty,
                                    onClick = { mathDifficulty = difficulty },
                                    label = { Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }) },
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
                                        selected = mathDifficulty == difficulty
                                    )
                                )
                            }
                        }
                    }

                    MissionType.TYPING -> {
                        OutlinedTextField(
                            value = typingWord,
                            onValueChange = { typingWord = it },
                            label = { Text("Phrase", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = defaultTextFieldColors(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Case sensitive", color = TextSecondary, modifier = Modifier.weight(1f))
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

                    MissionType.BARCODE -> {
                        if (barcodeValue.isNotBlank()) {
                            Text(
                                "Registered: $barcodeValue",
                                color = Mint,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Button(
                            onClick = {
                                val options = com.journeyapps.barcodescanner.ScanOptions().apply {
                                    setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.ALL_CODE_TYPES)
                                    setPrompt("Scan the code you must use to dismiss this alarm")
                                    setBeepEnabled(true)
                                    setOrientationLocked(false)
                                }
                                barcodeLauncher.launch(options)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Coral),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (barcodeValue.isBlank()) "Register Barcode" else "Rescan Barcode")
                        }
                        if (barcodeValue.isBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Register a specific barcode first. This mission will not accept random codes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = ::saveAlarm,
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracotta,
                    disabledContainerColor = GlassWhite,
                    contentColor = TextOnAccent,
                    disabledContentColor = TextDisabled
                )
            ) {
                Text(
                    text = "Save Alarm",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    trailing: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(trailing, color = Coral, style = MaterialTheme.typography.bodySmall)
    }
}

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
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
                        onValueChange(parsed.coerceIn(range.start.toInt(), range.endInclusive.toInt()))
                    }
                },
                modifier = Modifier
                    .width(80.dp)
                    .height(50.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    color = TextPrimary
                ),
                colors = defaultTextFieldColors()
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = {
                val newValue = it.toInt()
                onValueChange(newValue)
                textValue = newValue.toString()
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

enum class MissionType(val displayName: String, val description: String) {
    SHAKE("Shake", "Shake your phone"),
    MATH("Math", "Solve equations"),
    TYPING("Typing", "Type a phrase"),
    BARCODE("Barcode", "Scan a code")
}

fun MissionType.toIcon(): ImageVector = when (this) {
    MissionType.SHAKE -> Icons.Default.Vibration
    MissionType.MATH -> Icons.Default.Calculate
    MissionType.TYPING -> Icons.Default.Keyboard
    MissionType.BARCODE -> Icons.Default.QrCodeScanner
}

fun getMissionType(config: MissionConfig?): MissionType {
    return when (config) {
        is MissionConfig.Shake -> MissionType.SHAKE
        is MissionConfig.Math -> MissionType.MATH
        is MissionConfig.Typing -> MissionType.TYPING
        is MissionConfig.Barcode -> {
            if (config.expectedBarcode.isNullOrBlank()) MissionType.SHAKE else MissionType.BARCODE
        }
        else -> MissionType.SHAKE
    }
}

fun missionTypeFromName(name: String): MissionType {
    return runCatching { MissionType.valueOf(name) }.getOrDefault(MissionType.SHAKE)
}

@Composable
private fun defaultTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Coral,
    unfocusedBorderColor = GlassBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = Coral,
    focusedLabelColor = Coral,
    unfocusedLabelColor = TextSecondary
)
