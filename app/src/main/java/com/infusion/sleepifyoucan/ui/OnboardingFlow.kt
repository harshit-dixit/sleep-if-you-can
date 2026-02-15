package com.infusion.sleepifyoucan.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.infusion.sleepifyoucan.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val pages = listOf(
        OnboardingPage.Welcome,
        OnboardingPage.Missions,
        OnboardingPage.Permissions,
        OnboardingPage.GetStarted
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    var showSkip by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage) {
        showSkip = pagerState.currentPage < pages.size - 1
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(
                page = pages[page],
                onNext = {
                    if (page < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(page + 1)
                        }
                    } else {
                        onComplete()
                    }
                },
                onComplete = onComplete
            )
        }

        // Page indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 28.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSelected) Coral else TextPrimary.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Skip button
        AnimatedVisibility(
            visible = showSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .statusBarsPadding(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassWhite)
            ) {
                Text(
                    "Skip",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

sealed class OnboardingPage {
    object Welcome : OnboardingPage()
    object Missions : OnboardingPage()
    object Permissions : OnboardingPage()
    object GetStarted : OnboardingPage()
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    onNext: () -> Unit,
    onComplete: () -> Unit
) {
    when (page) {
        OnboardingPage.Welcome -> WelcomePage(onNext)
        OnboardingPage.Missions -> MissionsPage(onNext)
        OnboardingPage.Permissions -> PermissionsPage(onNext)
        OnboardingPage.GetStarted -> GetStartedPage(onComplete)
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    OnboardingBasePage(
        background = OnboardingGradient1,
        title = "Wake Up\nSmarter",
        subtitle = "The alarm that actually makes sure you're awake — with fun missions to get you out of bed",
        illustration = {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Lavender.copy(alpha = 0.3f),
                                ElectricBlue.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                BreathingAnimation {
                    Text("⏰", fontSize = 80.sp)
                }
            }
        },
        onNext = onNext
    )
}

@Composable
private fun MissionsPage(onNext: () -> Unit) {
    OnboardingBasePage(
        background = OnboardingGradient2,
        title = "Complete Missions\nto Dismiss",
        subtitle = "Shake your phone, solve math, take photos, do squats — choose your wake-up challenge!",
        illustration = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MissionChip("🧮", "Math")
                    MissionChip("📱", "Shake")
                    MissionChip("📷", "Photo")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MissionChip("🏋️", "Squat")
                    MissionChip("⌨️", "Type")
                    MissionChip("🚶", "Steps")
                }
            }
        },
        onNext = onNext
    )
}

@Composable
private fun MissionChip(emoji: String, label: String) {
    GlassCard(
        modifier = Modifier.width(90.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            PulseAnimation {
                Text(emoji, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PermissionsPage(onNext: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Track which permissions are granted
    var notificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var activityGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // Refresh permissions on resume (especially for Overlay/Appear on Top which takes user to Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                overlayGranted = Settings.canDrawOverlays(context)
                // Also check others just in case user changed them in settings
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
                cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activityGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationGranted = it
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraGranted = it
    }
    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        activityGranted = it
    }

    OnboardingBasePage(
        background = OnboardingGradient3,
        title = "Quick Setup",
        subtitle = "We need a few permissions to make sure your alarm works perfectly",
        illustration = {
            // Use a scrollable column for the permissions list to prevent cutoff on small screens
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp) // Limit height to allow scrolling within the illustration area
                    .verticalScroll(rememberScrollState())
            ) {
                // Notification Permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        description = "Show alarm notifications",
                        isGranted = notificationGranted,
                        onRequest = {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }

                // Overlay Permission
                PermissionItem(
                    icon = Icons.Default.Visibility,
                    title = "Display Over Apps",
                    description = "Show alarm over lock screen",
                    isGranted = overlayGranted,
                    onRequest = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                // Camera Permission
                PermissionItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Camera",
                    description = "For photo & barcode missions",
                    isGranted = cameraGranted,
                    onRequest = {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                )

                // Activity Recognition
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    PermissionItem(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        title = "Activity Recognition",
                        description = "For step & squat missions",
                        isGranted = activityGranted,
                        onRequest = {
                            activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    )
                }
                
                // Add some bottom padding to ensure the last item is easily clickable/visible
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        onNext = onNext,
        buttonText = "Continue"
    )
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isGranted) Mint else Lavender,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (isGranted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = Mint,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                TextButton(onClick = onRequest) {
                    Text("Grant", color = Coral, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GetStartedPage(onComplete: () -> Unit) {
    OnboardingBasePage(
        background = OnboardingGradient4,
        title = "You're All Set!",
        subtitle = "Create your first alarm and never oversleep again",
        illustration = {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Coral.copy(alpha = 0.3f),
                                OrangeAccent.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                BounceAnimation(isPressed = false) {
                    Text("🚀", fontSize = 72.sp)
                }
            }
        },
        buttonText = "Get Started",
        onNext = onComplete
    )
}

@Composable
private fun OnboardingBasePage(
    background: Brush,
    title: String,
    subtitle: String,
    illustration: @Composable () -> Unit,
    buttonText: String = "Next",
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.8f))

            // Illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 280.dp),
                contentAlignment = Alignment.Center
            ) {
                illustration()
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Title — high contrast white on dark gradient
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                lineHeight = 42.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Next button with glassmorphism
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Coral,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
