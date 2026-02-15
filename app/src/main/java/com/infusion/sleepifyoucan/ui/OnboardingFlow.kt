package com.infusion.sleepifyoucan.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infusion.sleepifyoucan.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val pages = listOf(
        OnboardingPage.Welcome,
        OnboardingPage.Missions,
        OnboardingPage.SleepTracking,
        OnboardingPage.SmartAlarms,
        OnboardingPage.GetStarted
    )
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    
    var showSkip by remember { mutableStateOf(true) }
    
    // Auto-advance through pages after delay
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < pages.size - 1) {
            delay(4000) // 4 seconds per page
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        } else {
            showSkip = false
        }
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Coral else TextTertiary.copy(alpha = 0.5f)
                        )
                )
            }
        }
        
        // Skip button
        AnimatedVisibility(
            visible = showSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TextButton(onClick = onSkip) {
                Text(
                    "Skip",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

sealed class OnboardingPage {
    object Welcome : OnboardingPage()
    object Missions : OnboardingPage()
    object SleepTracking : OnboardingPage()
    object SmartAlarms : OnboardingPage()
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
        OnboardingPage.SleepTracking -> SleepTrackingPage(onNext)
        OnboardingPage.SmartAlarms -> SmartAlarmsPage(onNext)
        OnboardingPage.GetStarted -> GetStartedPage(onComplete)
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    OnboardingBasePage(
        background = GradientPrimary,
        title = "Welcome to\nSleep If You Can",
        subtitle = "The most effective alarm app for heavy sleepers",
        illustration = { WelcomeIllustration() },
        onNext = onNext
    )
}

@Composable
private fun MissionsPage(onNext: () -> Unit) {
    OnboardingBasePage(
        background = GradientAccent,
        title = "Complete Missions\nto Wake Up",
        subtitle = "Solve math problems, shake your phone, take photos, and more!",
        illustration = { MissionsIllustration() },
        onNext = onNext
    )
}

@Composable
private fun SleepTrackingPage(onNext: () -> Unit) {
    OnboardingBasePage(
        background = GradientSuccess,
        title = "Track Your\nSleep Patterns",
        subtitle = "Monitor your sleep quality and get personalized insights",
        illustration = { SleepTrackingIllustration() },
        onNext = onNext
    )
}

@Composable
private fun SmartAlarmsPage(onNext: () -> Unit) {
    OnboardingBasePage(
        background = GradientWarning,
        title = "Smart Alarm\nTechnology",
        subtitle = "Volume escalation and wake-up checks ensure you never oversleep",
        illustration = { SmartAlarmsIllustration() },
        onNext = onNext
    )
}

@Composable
private fun GetStartedPage(onComplete: () -> Unit) {
    OnboardingBasePage(
        background = GradientSecondary,
        title = "Ready to Wake Up\nBetter?",
        subtitle = "Let's set up your first alarm and start your journey",
        illustration = { GetStartedIllustration() },
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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                illustration()
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                lineHeight = 50.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtitle
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Next button
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Coral,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Illustrations for each page
@Composable
private fun WelcomeIllustration() {
    // Animated moon and stars
    BreathingAnimation {
        Text("🌙", fontSize = 120.sp)
    }
}

@Composable
private fun MissionsIllustration() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulseAnimation { Text("🧮", fontSize = 48.sp) }
        ScaleFadeAnimation(visible = true) { Text("📱", fontSize = 48.sp) }
        PulseAnimation { Text("📷", fontSize = 48.sp) }
    }
}

@Composable
private fun SleepTrackingIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BreathingAnimation {
            Text("😴", fontSize = 64.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("📊", fontSize = 48.sp)
    }
}

@Composable
private fun SmartAlarmsIllustration() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulseAnimation { Text("🔊", fontSize = 48.sp) }
        ScaleFadeAnimation(visible = true) { Text("⏰", fontSize = 48.sp) }
        PulseAnimation { Text("✅", fontSize = 48.sp) }
    }
}

@Composable
private fun GetStartedIllustration() {
    BounceAnimation(isPressed = false) {
        Text("🚀", fontSize = 80.sp)
    }
}
