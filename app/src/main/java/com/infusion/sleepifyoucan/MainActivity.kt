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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.infusion.sleepifyoucan.data.AlarmRepository
import com.infusion.sleepifyoucan.data.AlarmScheduler
import com.infusion.sleepifyoucan.ui.AddEditAlarmScreen
import com.infusion.sleepifyoucan.ui.AlarmViewModel
import com.infusion.sleepifyoucan.ui.theme.SleepIfYouCanTheme
import com.infusion.sleepifyoucan.data.Alarm

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
        val repository = AlarmRepository(database.alarmDao(), AlarmScheduler(this))

        setContent {
            SleepIfYouCanTheme {
                val navController = rememberNavController()
                val viewModel: AlarmViewModel = viewModel(factory = AlarmViewModel.Factory(repository))

                NavHost(navController = navController, startDestination = "list") {
                    composable("list") {
                        AlarmListScreen(
                            viewModel = viewModel,
                            onAddClick = {
                                if (checkExactAlarmPermission(this@MainActivity)) {
                                    navController.navigate("add_edit") 
                                }
                            },
                            onAlarmClick = { alarm -> 
                                // Navigate to Edit with ID
                                navController.navigate("add_edit?alarmId=${alarm.id}")
                            }
                        )
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
                            // Find alarm from list (synchronously from viewModel cache would be ideal, 
                            // but for now let's just collect the list or fetch it.
                            // Better: Pass the alarm object or fetch by ID in ViewModel.
                            // Simplest for now: Fetch from ViewModel flow (might be null initially if loading) or passed list??
                            // Re-querying is safest.
                            // Actually, let's just get it from the ViewModel list if available.
                            // But `allAlarms` is a Flow.
                            // Let's create a getById in ViewModel or just filter here (simple but robust enough for MVP)
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

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Critical: Overlay Permission for Alarm
        if (!Settings.canDrawOverlays(this)) {
            // Show toast and redirect (In a real app, use a Dialog explanation)
            Toast.makeText(this, "Please grant 'Display over other apps' to allow alarm to show.", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel,
    onAddClick: () -> Unit,
    onAlarmClick: (Alarm) -> Unit
) {
    val alarms by viewModel.allAlarms.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sleep If You Can") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alarms) { alarm ->
                AlarmItemCard(
                    alarm = alarm,
                    onToggle = { viewModel.toggleEnabled(alarm) },
                    onDelete = { viewModel.delete(alarm) }
                )
            }
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
                if (alarm.label != null) {
                    Text(text = alarm.label, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = if(alarm.daysOfWeek.isEmpty()) "One-time" else "Repeating",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 16.dp)
            )
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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