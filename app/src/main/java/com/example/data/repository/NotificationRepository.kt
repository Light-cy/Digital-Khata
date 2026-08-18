package com.example.data.repository

import android.content.Context
import com.example.data.local.KhataDatabase
import com.example.data.local.NotificationDao
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import com.example.data.model.TransactionType
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class NotificationRepository(
    private val notificationDao: NotificationDao,
    private val context: Context? = null
) {
    val allNotifications: Flow<List<AppNotification>> = notificationDao.getAllNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    private fun getHelper(): NotificationHelper? {
        return context?.let { NotificationHelper(it) }
    }

    suspend fun insertNotification(
        type: NotificationType,
        title: String,
        message: String,
        relatedScreen: String? = null,
        relatedId: Long? = null
    ): Long {
        val notification = AppNotification(
            type = type,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            relatedScreen = relatedScreen,
            relatedId = relatedId
        )
        return notificationDao.insert(notification)
    }

    suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun deleteNotification(id: Long) {
        notificationDao.deleteById(id)
    }

    suspend fun clearAllNotifications() {
        notificationDao.deleteAll()
    }

    suspend fun cleanOldNotifications(maxAgeDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxAgeDays.toLong())
        notificationDao.deleteOlderThan(cutoff)
    }

    suspend fun triggerDailyReminderAlert() {
        val helper = getHelper()
        if (helper != null && !helper.isDailyReminderEnabled()) return

        // Prevent inserting more than once per day
        val startOfToday = DateUtils.getStartOfDay(System.currentTimeMillis())
        val recent = notificationDao.getRecentByType(NotificationType.DAILY_REMINDER, startOfToday)
        if (recent.isEmpty()) {
            insertNotification(
                type = NotificationType.DAILY_REMINDER,
                title = "Daily Khata Reminder",
                message = "Take a quick moment to log today's earnings, expenses, or loan updates.",
                relatedScreen = "home"
            )
        }
    }

    suspend fun triggerWeeklyBackupAlert() {
        val helper = getHelper()
        if (helper != null && !helper.isWeeklyBackupReminderEnabled()) return

        val startOfWeek = DateUtils.getStartOfWeek(System.currentTimeMillis())
        val recent = notificationDao.getRecentByType(NotificationType.BACKUP_REMINDER, startOfWeek)
        if (recent.isEmpty()) {
            insertNotification(
                type = NotificationType.BACKUP_REMINDER,
                title = "Weekly Backup Reminder",
                message = "Safeguard your accounts! Export an offline CSV or Excel backup to your cloud or storage.",
                relatedScreen = "settings_backup"
            )
        }
    }

    suspend fun triggerRecurringAddedAlert(generatedItems: List<String>) {
        val helper = getHelper()
        if (helper != null && !helper.isRecurringAlertsEnabled()) return
        if (generatedItems.isEmpty()) return

        val itemSummary = if (generatedItems.size == 1) {
            generatedItems.first()
        } else {
            "${generatedItems.size} recurring entries: " + generatedItems.take(2).joinToString(", ") + if (generatedItems.size > 2) "..." else ""
        }

        insertNotification(
            type = NotificationType.RECURRING_ADDED,
            title = "Recurring Transaction Auto-Logged",
            message = "$itemSummary was automatically logged into your Khata today.",
            relatedScreen = "recurring"
        )
    }

    suspend fun checkAndTriggerLoanAlerts(khataRepository: KhataRepository) {
        val helper = getHelper()
        if (helper != null && !helper.isLoanRemindersEnabled()) return

        val now = System.currentTimeMillis()
        val fifteenDaysAgo = now - TimeUnit.DAYS.toMillis(15)
        val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)

        val allLoans = khataRepository.getAllTransactionsSnapshot().filter {
            (it.type == TransactionType.LOAN_GIVEN || it.type == TransactionType.LOAN_TAKEN) && !it.isSettled
        }

        for (loan in allLoans) {
            if (loan.date <= fifteenDaysAgo) {
                // Check if already notified about this specific loan in the last 7 days
                val recentForLoan = notificationDao.getRecentByTypeAndRelatedId(
                    NotificationType.LOAN_REMINDER,
                    loan.id,
                    sevenDaysAgo
                )

                if (recentForLoan.isEmpty()) {
                    val isLent = loan.type == TransactionType.LOAN_GIVEN
                    val title = if (isLent) "Unsettled Lent Money" else "Pending Loan Payment"
                    val days = ((now - loan.date) / TimeUnit.DAYS.toMillis(1)).toInt()
                    val message = if (isLent) {
                        "${loan.title} (${CurrencyUtils.format(loan.amount)}) has been receivable for $days days. Tap to review or mark settled."
                    } else {
                        "Borrowed loan from ${loan.title} (${CurrencyUtils.format(loan.amount)}) has been pending for $days days."
                    }

                    insertNotification(
                        type = NotificationType.LOAN_REMINDER,
                        title = title,
                        message = message,
                        relatedScreen = "loans",
                        relatedId = loan.id
                    )
                }
            }
        }
    }

    suspend fun checkAndTriggerBudgetAlerts(khataRepository: KhataRepository) {
        val helper = getHelper()
        if (helper != null && !helper.isBudgetAlertsEnabled()) return

        val now = System.currentTimeMillis()
        val monthStart = DateUtils.getStartOfMonth(now)
        val monthEnd = DateUtils.getEndOfMonth(now)
        val threeDaysAgo = now - TimeUnit.DAYS.toMillis(3)

        val budgets = khataRepository.getAllBudgetsSnapshot()
        if (budgets.isEmpty()) return

        val transactions = khataRepository.getAllTransactionsSnapshot().filter {
            it.date in monthStart..monthEnd && it.type == TransactionType.EXPENSE && it.category != null
        }

        val spentMap = mutableMapOf<String, Double>()
        for (tx in transactions) {
            val cat = tx.category ?: continue
            spentMap[cat] = (spentMap[cat] ?: 0.0) + tx.amount
        }

        for (budget in budgets) {
            val spent = spentMap[budget.category] ?: 0.0
            if (budget.monthlyLimit <= 0) continue

            val ratio = spent / budget.monthlyLimit
            if (ratio >= 1.0) {
                val recent = notificationDao.getRecentByTypeAndRelatedId(
                    NotificationType.BUDGET_ALERT,
                    budget.id,
                    threeDaysAgo
                )
                if (recent.isEmpty()) {
                    insertNotification(
                        type = NotificationType.BUDGET_ALERT,
                        title = "Budget Exceeded (${budget.category})",
                        message = "${budget.category} has spent ${CurrencyUtils.format(spent)} out of ${CurrencyUtils.format(budget.monthlyLimit)} monthly limit (${(ratio * 100).toInt()}%).",
                        relatedScreen = "budgets",
                        relatedId = budget.id
                    )
                }
            } else if (ratio >= 0.90) {
                val recent = notificationDao.getRecentByTypeAndRelatedId(
                    NotificationType.BUDGET_ALERT,
                    budget.id,
                    threeDaysAgo
                )
                if (recent.isEmpty()) {
                    insertNotification(
                        type = NotificationType.BUDGET_ALERT,
                        title = "Budget Warning: 90% Reached",
                        message = "${budget.category} is nearing its limit: ${CurrencyUtils.format(spent)} of ${CurrencyUtils.format(budget.monthlyLimit)} used.",
                        relatedScreen = "budgets",
                        relatedId = budget.id
                    )
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: NotificationRepository? = null

        fun getInstance(context: Context): NotificationRepository {
            return INSTANCE ?: synchronized(this) {
                val db = KhataDatabase.getDatabase(context)
                val instance = NotificationRepository(db.notificationDao(), context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
