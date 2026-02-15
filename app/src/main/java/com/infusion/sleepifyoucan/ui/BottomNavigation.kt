package com.infusion.sleepifyoucan.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.infusion.sleepifyoucan.ui.theme.*

/**
 * Bottom navigation bar with glassmorphism styling.
 * Three tabs: Alarms, Streak, Settings.
 */
@Composable
fun AppBottomNavigation(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xE0101020),
        contentColor = TextSecondary,
        tonalElevation = 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab

            NavigationBarItem(
                icon = {
                    BounceAnimation(isPressed = isSelected) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Coral,
                    selectedTextColor = Coral,
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary,
                    indicatorColor = Coral.copy(alpha = 0.12f)
                )
            )
        }
    }
}

/**
 * Navigation tabs — Alarms, Streak, Settings only.
 */
enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ALARMS(
        title = "Alarms",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    ),
    STREAK(
        title = "Streak",
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.Star
    ),
    SETTINGS(
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}
