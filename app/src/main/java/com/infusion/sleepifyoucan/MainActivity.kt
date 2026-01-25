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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.infusion.sleepifyoucan.data.AlarmScheduler
import com.infusion.sleepifyoucan.ui.theme.SleepIfYouCanTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission Denied! Alarm might not show.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()

        setContent {
            SleepIfYouCanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val alarmScheduler = remember { AlarmScheduler(context) }
    
    // Simple state for UI demo. In real app, bind to DataStore.
    var isAlarmSet by remember { mutableStateOf(false) }
    
    // TimePicker State
    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = false
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sleep If You Can",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        TimePicker(state = timePickerState)
        
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (checkExactAlarmPermission(context)) {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        
                        // If time is in past, add 1 day
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    
                    alarmScheduler.schedule(calendar.timeInMillis)
                    
                    // Save to SharedPreferences for BootReceiver
                    val sharedPref = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putLong("ALARM_TIME", calendar.timeInMillis)
                        apply()
                    }
                    
                    isAlarmSet = true
                    Toast.makeText(context, "Alarm Set for ${timePickerState.hour}:${timePickerState.minute}", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(text = "Set Alarm", fontSize = 18.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                alarmScheduler.cancel()
                
                // Clear from SharedPreferences
                val sharedPref = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    remove("ALARM_TIME")
                    apply()
                }
                
                isAlarmSet = false
                Toast.makeText(context, "Alarm Cancelled", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(text = "Cancel Alarm", fontSize = 18.sp)
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