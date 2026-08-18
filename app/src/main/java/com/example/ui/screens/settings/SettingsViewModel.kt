package com.example.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.KhataRepository
import com.example.util.AppThemeMode
import com.example.util.ExportImportManager
import com.example.util.ExportImportManager.ImportResult
import com.example.util.NotificationHelper
import com.example.util.SecurityManager
import com.example.util.StartingBalanceManager
import com.example.util.ThemeManager
import com.example.util.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: KhataRepository,
    private val securityManager: SecurityManager,
    private val themeManager: ThemeManager? = null,
    private val notificationHelper: NotificationHelper? = null,
    private val userManager: UserManager? = null,
    private val startingBalanceManager: StartingBalanceManager? = null
) : ViewModel() {

    val cashStartingBalance: StateFlow<Double> = startingBalanceManager?.cashStartingBalance
        ?: MutableStateFlow(0.0).asStateFlow()

    val accountStartingBalance: StateFlow<Double> = startingBalanceManager?.accountStartingBalance
        ?: MutableStateFlow(0.0).asStateFlow()

    private val _userName = MutableStateFlow(userManager?.getUserName() ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(securityManager.isPinSet())
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(securityManager.isBiometricEnabled)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _lockTimeoutSeconds = MutableStateFlow(securityManager.lockTimeoutSeconds)
    val lockTimeoutSeconds: StateFlow<Int> = _lockTimeoutSeconds.asStateFlow()

    private val _themeMode = MutableStateFlow(themeManager?.themeMode?.value ?: AppThemeMode.SYSTEM_DEFAULT)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isReminderEnabled = MutableStateFlow(notificationHelper?.isDailyReminderEnabled() ?: false)
    val isReminderEnabled: StateFlow<Boolean> = _isReminderEnabled.asStateFlow()

    private val _reminderTime = MutableStateFlow(notificationHelper?.getReminderTime() ?: Pair(21, 0))
    val reminderTime: StateFlow<Pair<Int, Int>> = _reminderTime.asStateFlow()

    private val _isWeeklyBackupReminderEnabled = MutableStateFlow(notificationHelper?.isWeeklyBackupReminderEnabled() ?: true)
    val isWeeklyBackupReminderEnabled: StateFlow<Boolean> = _isWeeklyBackupReminderEnabled.asStateFlow()

    private val _weeklyBackupSchedule = MutableStateFlow(notificationHelper?.getWeeklyBackupReminderSettings() ?: Triple(java.util.Calendar.SUNDAY, 20, 0))
    val weeklyBackupSchedule: StateFlow<Triple<Int, Int, Int>> = _weeklyBackupSchedule.asStateFlow()

    private val _isBudgetAlertsEnabled = MutableStateFlow(notificationHelper?.isBudgetAlertsEnabled() ?: true)
    val isBudgetAlertsEnabled: StateFlow<Boolean> = _isBudgetAlertsEnabled.asStateFlow()

    private val _isLoanRemindersEnabled = MutableStateFlow(notificationHelper?.isLoanRemindersEnabled() ?: true)
    val isLoanRemindersEnabled: StateFlow<Boolean> = _isLoanRemindersEnabled.asStateFlow()

    private val _isRecurringAlertsEnabled = MutableStateFlow(notificationHelper?.isRecurringAlertsEnabled() ?: true)
    val isRecurringAlertsEnabled: StateFlow<Boolean> = _isRecurringAlertsEnabled.asStateFlow()

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    fun setUserName(name: String) {
        userManager?.setUserName(name)
        _userName.value = name.trim()
    }

    fun setThemeMode(mode: AppThemeMode) {
        themeManager?.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun setReminderEnabled(enabled: Boolean) {
        notificationHelper?.setDailyReminderEnabled(enabled)
        _isReminderEnabled.value = enabled
    }

    fun setReminderTime(hour: Int, minute: Int) {
        notificationHelper?.setReminderTime(hour, minute)
        _reminderTime.value = Pair(hour, minute)
    }

    fun setWeeklyBackupReminderEnabled(enabled: Boolean) {
        notificationHelper?.setWeeklyBackupReminderEnabled(enabled)
        _isWeeklyBackupReminderEnabled.value = enabled
    }

    fun setWeeklyBackupReminderSchedule(dayOfWeek: Int, hour: Int, minute: Int) {
        notificationHelper?.setWeeklyBackupReminderSchedule(dayOfWeek, hour, minute)
        _weeklyBackupSchedule.value = Triple(dayOfWeek, hour, minute)
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        notificationHelper?.setBudgetAlertsEnabled(enabled)
        _isBudgetAlertsEnabled.value = enabled
    }

    fun setLoanRemindersEnabled(enabled: Boolean) {
        notificationHelper?.setLoanRemindersEnabled(enabled)
        _isLoanRemindersEnabled.value = enabled
    }

    fun setRecurringAlertsEnabled(enabled: Boolean) {
        notificationHelper?.setRecurringAlertsEnabled(enabled)
        _isRecurringAlertsEnabled.value = enabled
    }

    fun testWeeklyBackupNotification(context: Context) {
        NotificationHelper.showWeeklyBackupReminderNotification(context)
    }

    fun setPin(pin: String) {
        securityManager.setPin(pin)
        _isPinEnabled.value = true
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.isBiometricEnabled = enabled
        _isBiometricEnabled.value = enabled
    }

    fun setLockTimeout(seconds: Int) {
        securityManager.lockTimeoutSeconds = seconds
        _lockTimeoutSeconds.value = seconds
    }

    fun disablePin() {
        securityManager.removePin()
        _isPinEnabled.value = false
        _isBiometricEnabled.value = false
    }

    fun exportData(context: Context) {
        viewModelScope.launch {
            try {
                _exportStatus.value = "Exporting..."
                ExportImportManager.shareExportedFile(context, repository)
                _exportStatus.value = null
            } catch (e: Exception) {
                _exportStatus.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importData(context: Context, uri: Uri, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            _importStatus.value = "Importing..."
            val result = ExportImportManager.importFromUri(context, uri, repository)
            _importStatus.value = null
            onResult(result)
        }
    }

    fun setStartingBalances(cash: Double, account: Double) {
        startingBalanceManager?.setStartingBalances(cash, account)
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            startingBalanceManager?.clearStartingBalances()
            onComplete()
        }
    }
}
