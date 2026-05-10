package com.infusion.sleepifyoucan

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.infusion.sleepifyoucan.data.AlarmRepository
import com.infusion.sleepifyoucan.data.AlarmScheduler
import com.infusion.sleepifyoucan.data.StreakRepository
import com.infusion.sleepifyoucan.ui.*
import com.infusion.sleepifyoucan.ui.theme.*
import com.infusion.sleepifyoucan.data.Alarm
import com.infusion.sleepifyoucan.data.AppPreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var showOnboarding by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if onboarding has been completed
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        showOnboarding = !onboardingCompleted

        val database = (application as SleepApplication).database
        val alarmRepository = AlarmRepository(database.alarmDao(), AlarmScheduler(this))
        val streakRepository = StreakRepository(database.streakDao(), this)

        setContent {
            SleepIfYouCanTheme {
                if (showOnboarding) {
                    OnboardingFlow(
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

                    // Track current selected tab — default to ALARMS
                    var selectedTab by remember { mutableStateOf(AppDestination.ALARMS) }

                    // Track if we're on add/edit route
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isOnAddEdit = currentRoute?.startsWith("add_edit") == true

                    // Collect alarms
                    val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())

                    // Streak data
                    var currentStreak by remember { mutableIntStateOf(0) }
                    var weeklyProgress by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }
                    var motivationalMessage by remember { mutableStateOf("Start your streak today!") }

                    // Load streak data
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(Unit) {
                        scope.launch {
                            currentStreak = streakRepository.getCurrentStreakCount()
                            weeklyProgress = streakRepository.getWeeklyProgress()
                            motivationalMessage = streakRepository.getMotivationalMessage()
                        }
                    }

                    Scaffold(
                        containerColor = Charcoal,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            // Hide bottom nav on add/edit screen
                            if (!isOnAddEdit) {
                                BottomNavigationBar(
                                    currentDestination = selectedTab,
                                    onNavigate = { tab ->
                                        selectedTab = tab
                                        // Navigate back to main if on a sub-route
                                        if (currentRoute != "main") {
                                            navController.popBackStack("main", inclusive = false)
                                        }
                                    }
                                )
                            }
                        },
                        floatingActionButton = {
                            // Show FAB only on ALARMS tab and not on add/edit screen
                            if (selectedTab == AppDestination.ALARMS && !isOnAddEdit) {
                                FloatingActionButton(
                                    onClick = {
                                        if (checkExactAlarmPermission(this@MainActivity)) {
                                            navController.navigate("add_edit")
                                        }
                                    },
                                    containerColor = Terracotta,
                                    contentColor = TextOnAccent,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Alarm")
                                }
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
                                            onAlarmClick = { alarm ->
                                                navController.navigate("add_edit?alarmId=${alarm.id}")
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
                                        val preferences by settingsViewModel.preferences.collectAsState()

                                        SettingsScreen(
                                            preferences = preferences,
                                            onMissionAudioChange = { settingsViewModel.updateMissionAudioBehavior(it) },
                                            onEscapeModeChange = { settingsViewModel.updateEscapePreventionMode(it) },
                                            onVolumeEscalationChange = { settingsViewModel.updateVolumeEscalation(it) }
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
                                } else null

                                AddEditAlarmScreen(
                                    alarm = alarmToEdit,
                                    onSave = { alarm ->
                                        if (alarmId != -1) {
                                            viewModel.update(alarm)
                                        } else {
                                            viewModel.insert(alarm)
                                        }
                                        navController.popBackStack()
                                        
                                        val formattedTime = String.format("%d:%02d %s", if (alarm.hour % 12 == 0) 12 else alarm.hour % 12, alarm.minute, if (alarm.hour < 12) "AM" else "PM")
                                        val timeUntil = com.infusion.sleepifyoucan.utils.getTimeUntilAlarm(alarm.hour, alarm.minute, alarm.daysOfWeek)
                                        
                                        // Show snackbar confirmation
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Alarm set for $formattedTime — $timeUntil",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onCancel = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Redesigned Alarm List Screen with glassmorphism cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel,
    onAlarmClick: (Alarm) -> Unit
) {
    val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Text(
            text = "Alarms",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "No alarms",
                            modifier = Modifier.size(64.dp),
                            tint = Terracotta.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No alarms yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to create your first alarm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { /* Will be handled by FAB anyway, or could pass navController */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                        ) {
                            Text("Create Alarm", color = TextOnAccent)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(alarms) { alarm ->
                    AlarmItemCard(
                        alarm = alarm,
                        onClick = { onAlarmClick(alarm) },
                        onToggle = { viewModel.toggleEnabled(alarm) },
                        onDelete = { viewModel.delete(alarm) }
                    )
                }
            }
        }
    }
}

/**
 * Alarm Item Card with glassmorphism styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmItemCard(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by androidx.compose.animation.animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> DustyRose
                    else -> Clear
                }, label = "swipe_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Icon",
                    tint = TextOnAccent
                )
            }
        }
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (alarm.isEnabled) TextPrimary else TextDisabled
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Mission Type Badge
                            val missionType = getMissionType(alarm.missionConfig)
                            Box(
                                modifier = Modifier
                                    .background(if (alarm.isEnabled) Terracotta.copy(alpha = 0.2f) else WarmBrown, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = missionType.toIcon(),
                                        contentDescription = null,
                                        tint = if (alarm.isEnabled) Terracotta else TextDisabled,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = missionType.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (alarm.isEnabled) Terracotta else TextDisabled
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (alarm.label != null && alarm.label.isNotBlank()) {
                                Text(
                                    text = alarm.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = " • ",
                                    color = TextDisabled
                                )
                            }
                            
                            // Time until alarm
                            if (alarm.isEnabled) {
                                val timeUntil = com.infusion.sleepifyoucan.utils.getTimeUntilAlarm(alarm.hour, alarm.minute, alarm.daysOfWeek)
                                Text(
                                    text = timeUntil,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Sage
                                )
                            } else {
                                Text(
                                    text = "Off",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextDisabled
                                )
                            }
                        }
                    }

                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Terracotta,
                            checkedTrackColor = Terracotta.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextDisabled,
                            uncheckedTrackColor = WarmBrown
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Day Schedule Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val days = listOf(
                        java.util.Calendar.MONDAY to "M",
                        java.util.Calendar.TUESDAY to "T",
                        java.util.Calendar.WEDNESDAY to "W",
                        java.util.Calendar.THURSDAY to "T",
                        java.util.Calendar.FRIDAY to "F",
                        java.util.Calendar.SATURDAY to "S",
                        java.util.Calendar.SUNDAY to "S"
                    )
                    
                    days.forEach { (dayInt, dayStr) ->
                        val isSelected = alarm.daysOfWeek.contains(dayInt)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .background(
                                    color = if (isSelected) 
                                        (if (alarm.isEnabled) Terracotta.copy(alpha = 0.15f) else WarmBrown) 
                                        else Clear,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayStr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) 
                                    (if (alarm.isEnabled) Terracotta else TextSecondary) 
                                    else TextDisabled
                            )
                        }
                    }
                }
            }
        }
    }
}

fun checkExactAlarmPermission(context: Context): Boolean {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            // Navigate to the exact alarm settings page — the only reliable way to grant this
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return false
        }
    }
    return true
}