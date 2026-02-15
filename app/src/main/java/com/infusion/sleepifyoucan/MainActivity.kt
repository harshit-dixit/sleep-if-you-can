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
                        },
                        onSkip = {
                            getSharedPreferences("app_prefs", MODE_PRIVATE)
                                .edit()
                                .putBoolean("onboarding_completed", true)
                                .apply()
                            showOnboarding = false
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val viewModel: AlarmViewModel = viewModel(factory = AlarmViewModel.Factory(alarmRepository))
                    val settingsViewModel: SettingsViewModel = viewModel()

                    // Track current selected tab — default to ALARMS
                    var selectedTab by remember { mutableStateOf(NavigationTab.ALARMS) }

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
                        containerColor = DeepNavy,
                        bottomBar = {
                            // Hide bottom nav on add/edit screen
                            if (!isOnAddEdit) {
                                AppBottomNavigation(
                                    selectedTab = selectedTab,
                                    onTabSelected = { tab ->
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
                            if (selectedTab == NavigationTab.ALARMS && !isOnAddEdit) {
                                FloatingActionButton(
                                    onClick = {
                                        if (checkExactAlarmPermission(this@MainActivity)) {
                                            navController.navigate("add_edit")
                                        }
                                    },
                                    containerColor = Coral,
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
                                    NavigationTab.ALARMS -> {
                                        AlarmListScreen(
                                            viewModel = viewModel,
                                            onAlarmClick = { alarm ->
                                                navController.navigate("add_edit?alarmId=${alarm.id}")
                                            }
                                        )
                                    }
                                    NavigationTab.STREAK -> {
                                        StreakScreen(
                                            currentStreak = currentStreak,
                                            weeklyProgress = weeklyProgress,
                                            motivationalMessage = motivationalMessage
                                        )
                                    }
                                    NavigationTab.SETTINGS -> {
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
            .background(DeepNavy)
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
                        Text(
                            text = "⏰",
                            style = MaterialTheme.typography.displaySmall
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
@Composable
fun AlarmItemCard(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) TextPrimary else TextDisabled
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    if (alarm.label != null) {
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
                    Text(
                        text = if (alarm.daysOfWeek.isEmpty()) "One-time" else "Repeating",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
            }

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Coral,
                    checkedTrackColor = Coral.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextDisabled,
                    uncheckedTrackColor = GlassWhite
                ),
                modifier = Modifier.padding(end = 8.dp)
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun checkExactAlarmPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
            Toast.makeText(context, "Please grant 'Alarms & Reminders' permission", Toast.LENGTH_LONG).show()
            return false
        }
    }
    return true
}