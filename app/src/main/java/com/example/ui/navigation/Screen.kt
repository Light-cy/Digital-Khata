package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val filledIcon: ImageVector? = null,
    val outlinedIcon: ImageVector? = null
) {
    // 5 Main Bottom Navigation Tabs
    data object Home : Screen("home", "Daily", Icons.Filled.Home, Icons.Outlined.Home)
    data object Weekly : Screen("weekly", "Weekly", Icons.Filled.ViewWeek, Icons.Outlined.ViewWeek)
    data object Monthly : Screen("monthly", "Monthly", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object Loans : Screen("loans", "Loans", Icons.Filled.Handshake, Icons.Outlined.Handshake)
    data object More : Screen("more", "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)

    // Sub-screens under More tab / Header shortcuts
    data object History : Screen("history", "All Transactions")
    data object Budgets : Screen("budgets", "Manage Budgets")
    data object Recurring : Screen("recurring", "Recurring Entries")
    data object Settings : Screen("settings", "Settings & Backup")
    data object Notifications : Screen("notifications", "Notifications")
}

