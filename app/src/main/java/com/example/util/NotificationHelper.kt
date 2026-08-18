package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.data.repository.NotificationRepository

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showDailyReminderNotification(context)
        // Insert in-app notification
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationRepository.getInstance(context).triggerDailyReminderAlert()
            } catch (e: Exception) {
                // Ignore
            }
        }
        // Schedule next day's alarm
        val notificationHelper = NotificationHelper(context)
        if (notificationHelper.isDailyReminderEnabled()) {
            val (hour, minute) = notificationHelper.getReminderTime()
            notificationHelper.scheduleDailyReminder(hour, minute)
        }
    }
}

class BackupReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showWeeklyBackupReminderNotification(context)
        // Insert in-app notification
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationRepository.getInstance(context).triggerWeeklyBackupAlert()
            } catch (e: Exception) {
                // Ignore
            }
        }
        // Schedule next week's backup alarm
        val notificationHelper = NotificationHelper(context)
        if (notificationHelper.isWeeklyBackupReminderEnabled()) {
            val (dayOfWeek, hour, minute) = notificationHelper.getWeeklyBackupReminderSettings()
            notificationHelper.scheduleWeeklyBackupReminder(dayOfWeek, hour, minute)
        }
    }
}

class NotificationHelper(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("digital_khata_notifications", Context.MODE_PRIVATE)

    companion object {
        const val CHANNEL_DAILY_ID = "daily_khata_reminders"
        const val CHANNEL_BACKUP_ID = "weekly_backup_reminders"
        const val DAILY_NOTIFICATION_ID = 1001
        const val BACKUP_NOTIFICATION_ID = 2001

        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val DESTINATION_SETTINGS_BACKUP = "settings_backup"

        private const val KEY_DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
        private const val KEY_DAILY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_DAILY_REMINDER_MINUTE = "reminder_minute"

        private const val KEY_BACKUP_REMINDER_ENABLED = "weekly_backup_reminder_enabled"
        private const val KEY_BACKUP_DAY_OF_WEEK = "backup_day_of_week"
        private const val KEY_BACKUP_HOUR = "backup_hour"
        private const val KEY_BACKUP_MINUTE = "backup_minute"

        private const val KEY_BUDGET_ALERTS_ENABLED = "budget_alerts_enabled"
        private const val KEY_LOAN_REMINDERS_ENABLED = "loan_reminders_enabled"
        private const val KEY_RECURRING_ALERTS_ENABLED = "recurring_alerts_enabled"

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Daily Reminder Channel
                val dailyChannel = NotificationChannel(
                    CHANNEL_DAILY_ID,
                    "Daily Khata Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders to log daily earnings, expenses, and loans"
                }
                notificationManager.createNotificationChannel(dailyChannel)

                // Weekly Backup Reminder Channel
                val backupChannel = NotificationChannel(
                    CHANNEL_BACKUP_ID,
                    "Weekly Backup Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Weekly reminders to back up and safeguard your financial ledger"
                }
                notificationManager.createNotificationChannel(backupChannel)
            }
        }

        fun showDailyReminderNotification(context: Context) {
            createNotificationChannels(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_ID)
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle("Digital Khata Daily Reminder")
                .setContentText("Have you logged today's earnings, expenses, and loans?")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Keep your finances clear and accurate! Take 30 seconds to record what you earned, spent, lent, or borrowed today.")
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            try {
                NotificationManagerCompat.from(context).notify(DAILY_NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                // Ignore if notification permission is denied
            }
        }

        fun showWeeklyBackupReminderNotification(context: Context) {
            createNotificationChannels(context)

            // Primary intent opening Backup / Settings screen
            val openBackupIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAVIGATE_TO, DESTINATION_SETTINGS_BACKUP)
            }
            val contentPendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                201,
                openBackupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Direct action button intent
            val actionPendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                202,
                openBackupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_BACKUP_ID)
                .setSmallIcon(android.R.drawable.ic_menu_save)
                .setContentTitle("Weekly Khata Backup Reminder")
                .setContentText("Don't forget to back up your Khata data this week")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Don't forget to back up your Khata data this week! Tap below to export an offline CSV / Excel copy to Google Drive, WhatsApp, or local storage.")
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentPendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_save,
                    "Backup Now",
                    actionPendingIntent
                )
                .setAutoCancel(true)

            try {
                NotificationManagerCompat.from(context).notify(BACKUP_NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                // Ignore if notification permission is denied
            }
        }
    }

    // ==========================================
    // Daily Reminder Functions
    // ==========================================

    fun isDailyReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_DAILY_REMINDER_ENABLED, false)
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_REMINDER_ENABLED, enabled).apply()
        if (enabled) {
            val (hour, minute) = getReminderTime()
            scheduleDailyReminder(hour, minute)
        } else {
            cancelDailyReminder()
        }
    }

    fun getReminderTime(): Pair<Int, Int> {
        val hour = prefs.getInt(KEY_DAILY_REMINDER_HOUR, 21) // Default 9:00 PM
        val minute = prefs.getInt(KEY_DAILY_REMINDER_MINUTE, 0)
        return Pair(hour, minute)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_DAILY_REMINDER_HOUR, hour)
            .putInt(KEY_DAILY_REMINDER_MINUTE, minute)
            .apply()
        if (isDailyReminderEnabled()) {
            scheduleDailyReminder(hour, minute)
        }
    }

    fun scheduleDailyReminder(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelDailyReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // ==========================================
    // Weekly Backup Reminder Functions
    // ==========================================

    fun isWeeklyBackupReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_BACKUP_REMINDER_ENABLED, true) // Default enabled to keep data safe
    }

    fun setWeeklyBackupReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKUP_REMINDER_ENABLED, enabled).apply()
        if (enabled) {
            val (dayOfWeek, hour, minute) = getWeeklyBackupReminderSettings()
            scheduleWeeklyBackupReminder(dayOfWeek, hour, minute)
        } else {
            cancelWeeklyBackupReminder()
        }
    }

    fun getWeeklyBackupReminderSettings(): Triple<Int, Int, Int> {
        val dayOfWeek = prefs.getInt(KEY_BACKUP_DAY_OF_WEEK, Calendar.SUNDAY)
        val hour = prefs.getInt(KEY_BACKUP_HOUR, 20) // Default Sunday 8:00 PM
        val minute = prefs.getInt(KEY_BACKUP_MINUTE, 0)
        return Triple(dayOfWeek, hour, minute)
    }

    fun setWeeklyBackupReminderSchedule(dayOfWeek: Int, hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_BACKUP_DAY_OF_WEEK, dayOfWeek)
            .putInt(KEY_BACKUP_HOUR, hour)
            .putInt(KEY_BACKUP_MINUTE, minute)
            .apply()
        if (isWeeklyBackupReminderEnabled()) {
            scheduleWeeklyBackupReminder(dayOfWeek, hour, minute)
        }
    }

    fun scheduleWeeklyBackupReminder(dayOfWeek: Int = Calendar.SUNDAY, hour: Int = 20, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BackupReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the scheduled time is in the past, advance to next week
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelWeeklyBackupReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BackupReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // ==========================================
    // Additional In-App Alert Preferences
    // ==========================================

    fun isBudgetAlertsEnabled(): Boolean {
        return prefs.getBoolean(KEY_BUDGET_ALERTS_ENABLED, true)
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BUDGET_ALERTS_ENABLED, enabled).apply()
    }

    fun isLoanRemindersEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOAN_REMINDERS_ENABLED, true)
    }

    fun setLoanRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOAN_REMINDERS_ENABLED, enabled).apply()
    }

    fun isRecurringAlertsEnabled(): Boolean {
        return prefs.getBoolean(KEY_RECURRING_ALERTS_ENABLED, true)
    }

    fun setRecurringAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RECURRING_ALERTS_ENABLED, enabled).apply()
    }
}

