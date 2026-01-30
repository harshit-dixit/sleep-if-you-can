package com.infusion.sleepifyoucan

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.infusion.sleepifyoucan.data.AlarmRepository
import com.infusion.sleepifyoucan.data.AlarmScheduler
import com.infusion.sleepifyoucan.data.StreakRepository
import com.infusion.sleepifyoucan.ui.*
import com.infusion.sleepifyoucan.ui.theme.*
import com.infusion.sleepifyoucan.data.Alarm
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
           // Good
        } else {
            Toast.makeText(this, "Permission Denied! Alarm might not show.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()

        val database = (application as SleepApplication).database
        val alarmRepository = AlarmRepository(database.alarmDao(), AlarmScheduler(this))
        val streakRepository = StreakRepository(database.streakDao(), this)

        setContent {
            SleepIfYouCanTheme {
                val navController = rememberNavController()
                val viewModel: AlarmViewModel = viewModel(factory = AlarmViewModel.Factory(alarmRepository))
                
                // Track current selected tab
                var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }
                
                // Collect alarms
                val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())
                val nextAlarm = alarms.filter { it.isEnabled }.minByOrNull { 
                    it.hour * 60 + it.minute 
                }
                
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
                    containerColor = BlackMute,
                    bottomBar = {
                        AppBottomNavigation(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    },
                    floatingActionButton = {
                        if (selectedTab == NavigationTab.ALARMS) {
                            FloatingActionButton(
                                onClick = {
                                    if (checkExactAlarmPermission(this@MainActivity)) {
                                        navController.navigate("add_edit")
                                    }
                                },
                                containerColor = PurpleNight,
                                contentColor = TextPrimary
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
                                NavigationTab.HOME -> {
                                    HomeScreen(
                                        nextAlarm = nextAlarm,
                                        currentStreak = currentStreak,
                                        onAlarmClick = { selectedTab = NavigationTab.ALARMS },
                                        onEditSleepTime = { /* TODO: Implement sleep time editor */ }
                                    )
                                }
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
                            }
                        }
                        composable(
                            "add_edit?alarmId={alarmId}",
                            arguments = listOf(androidx.navigation.navArgument("alarmId") { 
                                defaultValue = -1 
                                type = androidx.navigation.NavType.IntType
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

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Critical: Overlay Permission for Alarm
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant 'Display over other apps' to allow alarm to show.", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}

/**
 * Redesigned Alarm List Screen with cards matching the app theme.
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
            .background(BlackMute)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Text(
            text = "Alarms",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No alarms yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to create your first alarm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDisabled
                    )
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
 * Redesigned Alarm Item Card with modern styling.
 */
@Composable
fun AlarmItemCard(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = BlackMuteSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        text = if(alarm.daysOfWeek.isEmpty()) "One-time" else "Repeating",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
            }
            
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PurpleNight,
                    checkedTrackColor = PurpleNight.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextDisabled,
                    uncheckedTrackColor = BlackMuteDark
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = OrangeJuice.copy(alpha = 0.8f)
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