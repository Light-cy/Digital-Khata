package com.example

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.local.KhataDatabase
import com.example.data.repository.KhataRepository
import com.example.data.repository.NotificationRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.theme.DigitalKhataTheme
import com.example.util.AppThemeMode
import com.example.util.NotificationHelper
import com.example.util.SecurityManager
import com.example.util.ThemeManager
import com.example.util.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var database: KhataDatabase
    private lateinit var repository: KhataRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var securityManager: SecurityManager
    private lateinit var themeManager: ThemeManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var userManager: UserManager

    private var targetRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = KhataDatabase.getDatabase(applicationContext)
        repository = KhataRepository(
            transactionDao = database.transactionDao(),
            recurringTemplateDao = database.recurringTemplateDao(),
            budgetDao = database.budgetDao(),
            context = applicationContext
        )
        notificationRepository = NotificationRepository(
            notificationDao = database.notificationDao(),
            context = applicationContext
        )
        securityManager = SecurityManager(applicationContext)
        themeManager = ThemeManager(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        userManager = UserManager(applicationContext)

        // Initialize channels and verify alarms
        NotificationHelper.createNotificationChannels(applicationContext)
        if (notificationHelper.isWeeklyBackupReminderEnabled()) {
            val (dayOfWeek, hour, minute) = notificationHelper.getWeeklyBackupReminderSettings()
            notificationHelper.scheduleWeeklyBackupReminder(dayOfWeek, hour, minute)
        }

        handleIntentNavigation(intent)

        // Clear all previous seed data so the user has a completely clean, fresh ledger
        lifecycleScope.launch(Dispatchers.IO) {
            val prefs = getSharedPreferences("digital_khata_app_prefs", MODE_PRIVATE)
            val isCleaned = prefs.getBoolean("seed_data_cleaned_clean_slate", false)
            if (!isCleaned) {
                repository.clearAllData()
                prefs.edit().putBoolean("seed_data_cleaned_clean_slate", true).apply()
            }
        }

        setContent {
            val themeMode by themeManager.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM_DEFAULT -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            var showSplash by remember { mutableStateOf(true) }

            DigitalKhataTheme(darkTheme = darkTheme) {
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 400),
                    label = "splashCrossfade"
                ) { isSplashVisible ->
                    if (isSplashVisible) {
                        SplashScreen(
                            onSplashFinished = {
                                showSplash = false
                            }
                        )
                    } else {
                        MainScreen(
                            repository = repository,
                            notificationRepository = notificationRepository,
                            securityManager = securityManager,
                            themeManager = themeManager,
                            notificationHelper = notificationHelper,
                            userManager = userManager,
                            initialRoute = targetRoute
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentNavigation(intent)
    }

    private fun handleIntentNavigation(intent: Intent?) {
        val navigateTo = intent?.getStringExtra(NotificationHelper.EXTRA_NAVIGATE_TO)
        if (navigateTo == NotificationHelper.DESTINATION_SETTINGS_BACKUP) {
            targetRoute = Screen.Settings.route
        }
    }

    override fun onResume() {
        super.onResume()
        // Do not prematurely clear background time here before security check
    }

    override fun onStop() {
        super.onStop()
        securityManager.onAppBackgrounded()
    }
}
