package com.infusion.sleepifyoucan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.infusion.sleepifyoucan.ui.theme.*

enum class NavDestination {
    ALARMS, STREAK, SETTINGS
}

@Composable
fun BottomNavigationBar(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassNavBar() // Uses warm gradients from Glassmorphism.kt
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(
            selected = currentDestination == NavDestination.ALARMS,
            onClick = { onNavigate(NavDestination.ALARMS) },
            icon = if (currentDestination == NavDestination.ALARMS) Icons.Filled.Alarm else Icons.Outlined.Alarm,
            label = "Alarms"
        )
        NavItem(
            selected = currentDestination == NavDestination.STREAK,
            onClick = { onNavigate(NavDestination.STREAK) },
            icon = if (currentDestination == NavDestination.STREAK) Icons.Filled.LocalFireDepartment else Icons.Outlined.LocalFireDepartment,
            label = "Streak"
        )
        NavItem(
            selected = currentDestination == NavDestination.SETTINGS,
            onClick = { onNavigate(NavDestination.SETTINGS) },
            icon = if (currentDestination == NavDestination.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
            label = "Settings"
        )
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = "$label tab, ${if (selected) "selected" else "unselected"}" }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (selected) Terracotta.copy(alpha = 0.15f) else Clear),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Terracotta else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Terracotta
            )
        }
    }
}
