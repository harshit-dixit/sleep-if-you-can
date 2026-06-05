package com.infusion.sleepifyoucan

import android.Manifest
import android.app.NotificationManager
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.infusion.sleepifyoucan.data.Alarm
import com.infusion.sleepifyoucan.data.AlarmRepository
import com.infusion.sleepifyoucan.data.AlarmScheduleCalculator
import com.infusion.sleepifyoucan.data.AlarmScheduler
import com.infusion.sleepifyoucan.data.StreakRepository
import com.infusion.sleepifyoucan.ui.AddEditAlarmScreen
import com.infusion.sleepifyoucan.ui.AlarmViewModel
import com.infusion.sleepifyoucan.ui.AppDestination
import com.infusion.sleepifyoucan.ui.BottomNavigationBar
import com.infusion.sleepifyoucan.ui.MissionType
import com.infusion.sleepifyoucan.ui.SettingsScreen
import com.infusion.sleepifyoucan.ui.SettingsViewModel
import com.infusion.sleepifyoucan.ui.StreakScreen
import com.infusion.sleepifyoucan.ui.StartupPermissionScreen
import com.infusion.sleepifyoucan.ui.StartupPermissionUiItem
import com.infusion.sleepifyoucan.ui.StartupPermissionUiState
import com.infusion.sleepifyoucan.ui.getMissionType
import com.infusion.sleepifyoucan.ui.toIcon
import com.infusion.sleepifyoucan.ui.theme.AccentBlue
import com.infusion.sleepifyoucan.ui.theme.AccentBlueSoft
import com.infusion.sleepifyoucan.ui.theme.AccentGreen
import com.infusion.sleepifyoucan.ui.theme.AccentGreenSoft
import com.infusion.sleepifyoucan.ui.theme.AccentRed
import com.infusion.sleepifyoucan.ui.theme.AccentRedSoft
import com.infusion.sleepifyoucan.ui.theme.AccentYellow
import com.infusion.sleepifyoucan.ui.theme.AccentYellowSoft
import com.infusion.sleepifyoucan.ui.theme.Ash
import com.infusion.sleepifyoucan.ui.theme.Body
import com.infusion.sleepifyoucan.ui.theme.Canvas
import com.infusion.sleepifyoucan.ui.theme.Clear
import com.infusion.sleepifyoucan.ui.theme.Error
import com.infusion.sleepifyoucan.ui.theme.GlassCard
import com.infusion.sleepifyoucan.ui.theme.Hairline
import com.infusion.sleepifyoucan.ui.theme.HairlineStrong
import com.infusion.sleepifyoucan.ui.theme.Ink
import com.infusion.sleepifyoucan.ui.theme.SleepIfYouCanTheme
import com.infusion.sleepifyoucan.ui.theme.Surface
import com.infusion.sleepifyoucan.ui.theme.SurfaceElevated
import com.infusion.sleepifyoucan.ui.theme.TextDisabled
import com.infusion.sleepifyoucan.ui.theme.TextPrimary
import com.infusion.sleepifyoucan.ui.theme.TextSecondary
import com.infusion.sleepifyoucan.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private var showOnboarding by mutableStateOf(false)
    private var permissionGateDismissed by mutableStateOf(false)
    private var permissionRefreshKey by mutableIntStateOf(0)
    private var startupPermissionFlowRunning = false
    private val attemptedSpecialPermissions = mutableSetOf<StartupSpecialPermission>()
    private lateinit var runtimePermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var specialPermissionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runtimePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            refreshStartupPermissions()
            if (startupPermissionFlowRunning) {
                openNextSpecialStartupPermission()
            }
        }

        specialPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            refreshStartupPermissions()
            if (startupPermissionFlowRunning) {
                openNextSpecialStartupPermission()
            }
        }

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        showOnboarding = !prefs.getBoolean("onboarding_completed", false)

        val database = (application as SleepApplication).database
        val alarmRepository = AlarmRepository(database.alarmDao(), AlarmScheduler(this))
        val streakRepository = StreakRepository(database.streakDao(), this)

        setContent {
            SleepIfYouCanTheme {
                val startupPermissionState = remember(permissionRefreshKey) {
                    buildStartupPermissionUiState()
                }

                if (!permissionGateDismissed && !startupPermissionState.allGranted) {
                    StartupPermissionScreen(
                        state = startupPermissionState,
                        onGrantMissingClick = { startStartupPermissionFlow() },
                        onContinueAnywayClick = { permissionGateDismissed = true }
                    )
                } else if (showOnboarding) {
                    com.infusion.sleepifyoucan.ui.OnboardingFlow(
                        onComplete = {
                            getSharedPreferences("app_prefs", MODE_PRIVATE)
                                .edit()
                                .putBoolean("onboarding_completed", true)
                                .apply()
                            showOnboarding = false
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val snackbarHostState = remember { SnackbarHostState() }
                    val viewModel: AlarmViewModel = viewModel(factory = AlarmViewModel.Factory(alarmRepository))
                    val settingsViewModel: SettingsViewModel = viewModel()
                    val preferences by settingsViewModel.preferences.collectAsState()
                    var selectedTab by remember { mutableStateOf(AppDestination.ALARMS) }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isOnAddEdit = currentRoute?.startsWith("add_edit") == true

                    var currentStreak by remember { mutableIntStateOf(0) }
                    var weeklyProgress by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }
                    var motivationalMessage by remember { mutableStateOf("Start your streak today!") }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        scope.launch {
                            currentStreak = streakRepository.getCurrentStreakCount()
                            weeklyProgress = streakRepository.getWeeklyProgress()
                            motivationalMessage = streakRepository.getMotivationalMessage()
                        }
                    }

                    Scaffold(
                        containerColor = Canvas,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            if (!isOnAddEdit) {
                                BottomNavigationBar(
                                    currentDestination = selectedTab,
                                    onNavigate = { tab ->
                                        selectedTab = tab
                                        if (currentRoute != "main") {
                                            navController.popBackStack("main", inclusive = false)
                                        }
                                    }
                                )
                            }
                        }
                    ) { padding ->
                        NavHost(
                            navController = navController,
                            startDestination = "main",
                            modifier = Modifier.padding(padding)
                        ) {
                            composable("main") {
                                when (selectedTab) {
                                    AppDestination.ALARMS -> {
                                        AlarmListScreen(
                                            viewModel = viewModel,
                                            use24HourFormat = preferences.use24HourFormat,
                                            onAlarmClick = { alarm ->
                                                navController.navigate("add_edit?alarmId=${alarm.id}")
                                            },
                                            onAddAlarmClick = {
                                                if (checkExactAlarmPermission(this@MainActivity)) {
                                                    navController.navigate("add_edit")
                                                }
                                            }
                                        )
                                    }

                                    AppDestination.STREAK -> {
                                        StreakScreen(
                                            currentStreak = currentStreak,
                                            weeklyProgress = weeklyProgress,
                                            motivationalMessage = motivationalMessage
                                        )
                                    }

                                    AppDestination.SETTINGS -> {
                                        SettingsScreen(
                                            preferences = preferences,
                                            onMissionAudioChange = { settingsViewModel.updateMissionAudioBehavior(it) },
                                            onEscapeModeChange = { settingsViewModel.updateEscapePreventionMode(it) },
                                            onVolumeEscalationChange = { settingsViewModel.updateVolumeEscalation(it) },
                                            onDefaultMissionChange = { settingsViewModel.updateDefaultMissionType(it) },
                                            onMaxSnoozeChange = { settingsViewModel.updateMaxSnoozeCount(it) },
                                            onTimeFormatChange = { settingsViewModel.updateUse24HourFormat(it) }
                                        )
                                    }
                                }
                            }

                            composable(
                                "add_edit?alarmId={alarmId}",
                                arguments = listOf(navArgument("alarmId") {
                                    defaultValue = -1
                                    type = NavType.IntType
                                })
                            ) { backStackEntry ->
                                val alarmId = backStackEntry.arguments?.getInt("alarmId") ?: -1
                                val alarmToEdit = if (alarmId != -1) {
                                    viewModel.allAlarms.collectAsState(initial = emptyList()).value.find { it.id == alarmId }
                                } else {
                                    null
                                }

                                AddEditAlarmScreen(
                                    alarm = alarmToEdit,
                                    defaultMissionType = preferences.defaultMissionType,
                                    use24HourFormat = preferences.use24HourFormat,
                                    onSave = { alarm ->
                                        if (alarmId != -1) {
                                            viewModel.update(alarm)
                                        } else {
                                            viewModel.insert(alarm)
                                        }
                                        navController.popBackStack()

                                        val formattedTime = formatAlarmTime(alarm, preferences.use24HourFormat)
                                        val timeUntil = com.infusion.sleepifyoucan.utils.getTimeUntilAlarm(
                                            alarm.hour,
                                            alarm.minute,
                                            alarm.daysOfWeek
                                        )
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Alarm set for $formattedTime - $timeUntil",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStartupPermissions()
    }

    private fun startStartupPermissionFlow() {
        permissionGateDismissed = false
        startupPermissionFlowRunning = true
        attemptedSpecialPermissions.clear()

        val runtimePermissions = missingStartupRuntimePermissions()
        if (runtimePermissions.isNotEmpty()) {
            runtimePermissionLauncher.launch(runtimePermissions.toTypedArray())
        } else {
            openNextSpecialStartupPermission()
        }
    }

    private fun openNextSpecialStartupPermission() {
        refreshStartupPermissions()

        val request = nextSpecialStartupPermissionRequest()
        if (request == null) {
            startupPermissionFlowRunning = false
            attemptedSpecialPermissions.clear()
            return
        }

        attemptedSpecialPermissions.add(request.type)
        try {
            specialPermissionLauncher.launch(request.intent)
        } catch (e: ActivityNotFoundException) {
            openNextSpecialStartupPermission()
        } catch (e: SecurityException) {
            openNextSpecialStartupPermission()
        }
    }

    private fun nextSpecialStartupPermissionRequest(): SpecialStartupPermissionRequest? {
        return listOfNotNull(
            if (!hasExactAlarmPermission()) {
                SpecialStartupPermissionRequest(
                    StartupSpecialPermission.EXACT_ALARM,
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            } else {
                null
            },
            if (!hasFullScreenIntentPermission()) {
                SpecialStartupPermissionRequest(
                    StartupSpecialPermission.FULL_SCREEN_INTENT,
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            } else {
                null
            },
            if (!Settings.canDrawOverlays(this)) {
                SpecialStartupPermissionRequest(
                    StartupSpecialPermission.OVERLAY,
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                null
            },
            if (!isIgnoringBatteryOptimizations()) {
                SpecialStartupPermissionRequest(
                    StartupSpecialPermission.BATTERY_OPTIMIZATION,
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                null
            }
        ).firstOrNull { it.type !in attemptedSpecialPermissions }
    }

    private fun missingStartupRuntimePermissions(): List<String> = buildList {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            add(Manifest.permission.CAMERA)
        }
    }

    private fun buildStartupPermissionUiState(): StartupPermissionUiState {
        val notificationsGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        return StartupPermissionUiState(
            items = listOf(
                StartupPermissionUiItem(
                    title = "Notifications",
                    description = "Shows the ringing alarm and foreground alert.",
                    isGranted = notificationsGranted,
                    accentColor = AccentBlue
                ),
                StartupPermissionUiItem(
                    title = "Exact alarms",
                    description = "Lets alarms trigger at the time you choose.",
                    isGranted = hasExactAlarmPermission(),
                    accentColor = AccentYellow
                ),
                StartupPermissionUiItem(
                    title = "Full-screen alarm",
                    description = "Allows the alarm screen to open when the phone is locked.",
                    isGranted = hasFullScreenIntentPermission(),
                    accentColor = AccentGreen
                ),
                StartupPermissionUiItem(
                    title = "Display over apps",
                    description = "Helps force the alarm mission to the front.",
                    isGranted = Settings.canDrawOverlays(this),
                    accentColor = AccentRed
                ),
                StartupPermissionUiItem(
                    title = "Battery optimization",
                    description = "Prevents power saving from delaying alarm work.",
                    isGranted = isIgnoringBatteryOptimizations(),
                    accentColor = AccentYellow
                ),
                StartupPermissionUiItem(
                    title = "Camera",
                    description = "Needed for barcode wake-up missions.",
                    isGranted = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
                    accentColor = AccentBlue
                )
            )
        )
    }

    private fun hasExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    private fun hasFullScreenIntentPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val notificationManager = getSystemService(NotificationManager::class.java)
        return notificationManager.canUseFullScreenIntent()
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun refreshStartupPermissions() {
        permissionRefreshKey++
    }
}

private enum class StartupSpecialPermission {
    EXACT_ALARM,
    FULL_SCREEN_INTENT,
    OVERLAY,
    BATTERY_OPTIMIZATION
}

private data class SpecialStartupPermissionRequest(
    val type: StartupSpecialPermission,
    val intent: Intent
)

@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel,
    use24HourFormat: Boolean,
    onAlarmClick: (Alarm) -> Unit,
    onAddAlarmClick: () -> Unit
) {
    val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())
    var deleteTarget by remember { mutableStateOf<Alarm?>(null) }

    val sortedAlarms = remember(alarms) {
        alarms.sortedWith(
            compareBy<Alarm> { !it.isEnabled }
                .thenBy { alarm ->
                    if (alarm.isEnabled) {
                        AlarmScheduleCalculator.nextTriggerTimeMillis(alarm)
                    } else {
                        Long.MAX_VALUE
                    }
                }
        )
    }
    val nextAlarm = sortedAlarms.firstOrNull { it.isEnabled }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alarms",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onAddAlarmClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Canvas),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
        }

        if (sortedAlarms.isEmpty()) {
            item {
                EmptyAlarmState(onAddAlarmClick = onAddAlarmClick)
            }
        } else {
            item {
                if (nextAlarm != null) {
                    NextAlarmCard(
                        alarm = nextAlarm,
                        use24HourFormat = use24HourFormat,
                        onClick = { onAlarmClick(nextAlarm) },
                        onToggle = { viewModel.toggleEnabled(nextAlarm) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                Text(
                    text = "All alarms",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(sortedAlarms, key = { it.id }) { alarm ->
                AlarmItemCard(
                    alarm = alarm,
                    use24HourFormat = use24HourFormat,
                    onClick = { onAlarmClick(alarm) },
                    onToggle = { viewModel.toggleEnabled(alarm) },
                    onDelete = { deleteTarget = alarm }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    deleteTarget?.let { alarm ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = Surface,
            title = { Text("Delete alarm?", color = TextPrimary) },
            text = { Text("This wake-up routine will be removed.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(alarm)
                        deleteTarget = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Error, contentColor = Canvas)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun EmptyAlarmState(onAddAlarmClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 96.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = AccentBlue
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No alarms yet",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create your first wake-up routine.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddAlarmClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Canvas)
            ) {
                Text("Create Alarm")
            }
        }
    }
}

@Composable
private fun NextAlarmCard(
    alarm: Alarm,
    use24HourFormat: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AccentGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Next alarm",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentGreen
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatAlarmTime(alarm, use24HourFormat),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = com.infusion.sleepifyoucan.utils.getTimeUntilAlarm(
                            alarm.hour,
                            alarm.minute,
                            alarm.daysOfWeek
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AccentGreen
                    )
                }
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = alarmSwitchColors()
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MissionBadge(alarm = alarm)
                if (alarm.label?.isNotBlank() == true) {
                    SurfacePill(text = alarm.label)
                }
            }
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: Alarm,
    use24HourFormat: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatAlarmTime(alarm, use24HourFormat),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (alarm.isEnabled) TextPrimary else TextDisabled
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    MissionBadge(alarm = alarm)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (alarm.isEnabled) {
                        com.infusion.sleepifyoucan.utils.getTimeUntilAlarm(
                            alarm.hour,
                            alarm.minute,
                            alarm.daysOfWeek
                        )
                    } else {
                        "Off"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (alarm.isEnabled) TextSecondary else TextDisabled
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete alarm",
                        tint = TextTertiary
                    )
                }
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = alarmSwitchColors()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        DayBadges(alarm = alarm)
    }
}

@Composable
private fun MissionBadge(alarm: Alarm) {
    val missionType = getMissionType(alarm.missionConfig)
    val accent = missionAccent(missionType)
    val softAccent = missionAccentSoft(missionType)
    Box(
        modifier = Modifier
            .background(if (alarm.isEnabled) softAccent else SurfaceElevated, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = missionType.toIcon(),
                contentDescription = null,
                tint = if (alarm.isEnabled) accent else TextDisabled,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = missionType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = if (alarm.isEnabled) accent else TextDisabled
            )
        }
    }
}

@Composable
private fun SurfacePill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = Modifier
            .background(SurfaceElevated, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun DayBadges(alarm: Alarm) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val days = listOf(
            Calendar.MONDAY to "M",
            Calendar.TUESDAY to "T",
            Calendar.WEDNESDAY to "W",
            Calendar.THURSDAY to "T",
            Calendar.FRIDAY to "F",
            Calendar.SATURDAY to "S",
            Calendar.SUNDAY to "S"
        )

        days.forEach { (dayInt, dayStr) ->
            val isSelected = alarm.daysOfWeek.contains(dayInt)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .background(
                        color = when {
                            isSelected && alarm.isEnabled -> AccentBlueSoft
                            isSelected -> Surface
                            else -> Clear
                        },
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayStr,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected && alarm.isEnabled) AccentBlue else TextDisabled
                )
            }
        }
    }
}

@Composable
private fun alarmSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = AccentGreen,
    checkedTrackColor = AccentGreenSoft,
    checkedBorderColor = HairlineStrong,
    uncheckedThumbColor = Ash,
    uncheckedTrackColor = SurfaceElevated,
    uncheckedBorderColor = Hairline
)

private fun missionAccent(missionType: MissionType) = when (missionType) {
    MissionType.SHAKE -> AccentYellow
    MissionType.MATH -> AccentBlue
    MissionType.TYPING -> AccentGreen
    MissionType.BARCODE -> AccentRed
}

private fun missionAccentSoft(missionType: MissionType) = when (missionType) {
    MissionType.SHAKE -> AccentYellowSoft
    MissionType.MATH -> AccentBlueSoft
    MissionType.TYPING -> AccentGreenSoft
    MissionType.BARCODE -> AccentRedSoft
}

private fun formatAlarmTime(alarm: Alarm, use24HourFormat: Boolean): String {
    return if (use24HourFormat) {
        String.format("%02d:%02d", alarm.hour, alarm.minute)
    } else {
        String.format(
            "%d:%02d %s",
            if (alarm.hour % 12 == 0) 12 else alarm.hour % 12,
            alarm.minute,
            if (alarm.hour < 12) "AM" else "PM"
        )
    }
}

fun checkExactAlarmPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return false
        }
    }
    return true
}
