package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.KhataRepository
import com.example.data.repository.NotificationRepository
import com.example.ui.components.PremiumFloatingBottomBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.budgets.BudgetsScreen
import com.example.ui.screens.budgets.BudgetsViewModel
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.history.HistoryViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.loans.LoansScreen
import com.example.ui.screens.loans.LoansViewModel
import com.example.ui.screens.lock.LockScreen
import com.example.ui.screens.monthly.MonthlyScreen
import com.example.ui.screens.monthly.MonthlyViewModel
import com.example.ui.screens.more.MoreScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.notifications.NotificationsViewModel
import com.example.ui.screens.recurring.RecurringScreen
import com.example.ui.screens.recurring.RecurringViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.weekly.WeeklyScreen
import com.example.ui.screens.weekly.WeeklyViewModel
import com.example.util.NotificationHelper
import com.example.util.RecurringSyncWorker
import com.example.util.SecurityManager
import com.example.util.ThemeManager
import com.example.util.UserManager

@Composable
fun MainScreen(
    repository: KhataRepository,
    notificationRepository: NotificationRepository? = null,
    securityManager: SecurityManager,
    themeManager: ThemeManager? = null,
    notificationHelper: NotificationHelper? = null,
    userManager: UserManager? = null,
    initialRoute: String? = null,
    modifier: Modifier = Modifier
) {
    var isAppLocked by remember {
        mutableStateOf<Boolean>(securityManager.isLocked())
    }

    val unreadCount by (notificationRepository?.unreadCount?.collectAsStateWithLifecycle(initialValue = 0) ?: remember { mutableStateOf(0) })

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, securityManager) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (securityManager.shouldLock()) {
                        isAppLocked = true
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    securityManager.onAppBackgrounded()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-process due recurring templates & triggers at app startup
    LaunchedEffect(Unit) {
        notificationRepository?.cleanOldNotifications(30)
        val generatedTransactions = RecurringSyncWorker.syncDueRecurringTransactions(repository)
        if (generatedTransactions.isNotEmpty()) {
            notificationRepository?.triggerRecurringAddedAlert(generatedTransactions)
        }
        notificationRepository?.checkAndTriggerLoanAlerts(repository)
        notificationRepository?.checkAndTriggerBudgetAlerts(repository)
    }

    if (isAppLocked) {
        LockScreen(
            isBiometricEnabled = securityManager.isBiometricEnabled,
            onVerifyPin = { pin ->
                val isValid = securityManager.verifyPin(pin)
                if (isValid) {
                    securityManager.unlock()
                }
                isValid
            },
            onUnlocked = {
                securityManager.unlock()
                isAppLocked = false
            },
            modifier = modifier
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Handle deep link / notification navigation to specific screen (e.g. Settings / Backup)
    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrEmpty()) {
            navController.navigate(initialRoute) {
                launchSingleTop = true
            }
        }
    }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Weekly,
        Screen.Monthly,
        Screen.Loans,
        Screen.More
    )

    val isSubScreen = currentRoute in listOf(
        Screen.History.route,
        Screen.Budgets.route,
        Screen.Recurring.route,
        Screen.Settings.route,
        Screen.Notifications.route
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isSubScreen) {
                PremiumFloatingBottomBar(
                    screens = bottomNavItems,
                    currentRoute = currentRoute,
                    onTabSelected = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Home Screen (Daily View)
            composable(Screen.Home.route) {
                val homeViewModel = remember { HomeViewModel(repository) }
                HomeScreen(
                    viewModel = homeViewModel,
                    userManager = userManager,
                    notificationHelper = notificationHelper,
                    unreadNotificationsCount = unreadCount,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNotificationClick = { navController.navigate(Screen.Notifications.route) }
                )
            }

            // Weekly Screen
            composable(Screen.Weekly.route) {
                val weeklyViewModel = remember { WeeklyViewModel(repository) }
                WeeklyScreen(
                    viewModel = weeklyViewModel,
                    notificationHelper = notificationHelper,
                    unreadNotificationsCount = unreadCount,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                    onDayClick = { dayMillis ->
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // Monthly Screen
            composable(Screen.Monthly.route) {
                val monthlyViewModel = remember { MonthlyViewModel(repository) }
                MonthlyScreen(
                    viewModel = monthlyViewModel,
                    notificationHelper = notificationHelper,
                    unreadNotificationsCount = unreadCount,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNotificationClick = { navController.navigate(Screen.Notifications.route) }
                )
            }

            // Loans Screen
            composable(Screen.Loans.route) {
                val loansViewModel = remember { LoansViewModel(repository) }
                LoansScreen(
                    viewModel = loansViewModel,
                    notificationHelper = notificationHelper,
                    unreadNotificationsCount = unreadCount,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNotificationClick = { navController.navigate(Screen.Notifications.route) }
                )
            }

            // More Hub Screen
            composable(Screen.More.route) {
                MoreScreen(
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Sub-screen: History
            composable(Screen.History.route) {
                val historyViewModel = remember { HistoryViewModel(repository) }
                HistoryScreen(
                    viewModel = historyViewModel,
                    notificationHelper = notificationHelper,
                    unreadNotificationsCount = unreadCount,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Budgets
            composable(Screen.Budgets.route) {
                val budgetsViewModel = remember { BudgetsViewModel(repository) }
                BudgetsScreen(
                    viewModel = budgetsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Recurring
            composable(Screen.Recurring.route) {
                val recurringViewModel = remember { RecurringViewModel(repository) }
                RecurringScreen(
                    viewModel = recurringViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Notifications
            composable(Screen.Notifications.route) {
                if (notificationRepository != null) {
                    val notificationsViewModel = remember { NotificationsViewModel(notificationRepository) }
                    NotificationsScreen(
                        viewModel = notificationsViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToTarget = { target, _ ->
                            when (target.lowercase()) {
                                "budgets" -> navController.navigate(Screen.Budgets.route)
                                "loans" -> navController.navigate(Screen.Loans.route)
                                "recurring" -> navController.navigate(Screen.Recurring.route)
                                "settings_backup", "settings" -> navController.navigate(Screen.Settings.route)
                                "home" -> navController.navigate(Screen.Home.route)
                                else -> navController.popBackStack()
                            }
                        }
                    )
                }
            }

            // Sub-screen: Settings
            composable(Screen.Settings.route) {
                val settingsViewModel = remember {
                    SettingsViewModel(
                        repository = repository,
                        securityManager = securityManager,
                        themeManager = themeManager,
                        notificationHelper = notificationHelper,
                        userManager = userManager
                    )
                }
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
