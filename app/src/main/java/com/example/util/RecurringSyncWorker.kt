package com.example.util

import com.example.data.model.RecurringFrequency
import com.example.data.model.Transaction
import com.example.data.repository.KhataRepository
import java.util.Calendar

object RecurringSyncWorker {

    /**
     * Checks all active recurring templates and creates transactions if due.
     * Returns the count of generated transactions.
     */
    suspend fun processRecurringEntries(repository: KhataRepository): Int {
        return syncDueRecurringTransactions(repository).size
    }

    /**
     * Checks all active recurring templates and creates transactions if due.
     * Returns a list of generated transaction titles.
     */
    suspend fun syncDueRecurringTransactions(repository: KhataRepository): List<String> {
        val activeTemplates = repository.getActiveTemplatesSnapshot()
        val generatedTitles = mutableListOf<String>()
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val currentDayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        for (template in activeTemplates) {
            val isDue = when (template.frequency) {
                RecurringFrequency.DAILY -> {
                    // Due if never generated or last generated before today
                    template.lastGeneratedDate == null || !DateUtils.isSameDay(template.lastGeneratedDate, now)
                }

                RecurringFrequency.WEEKLY -> {
                    // Due if today matches dayOfWeek AND not generated in this week
                    val matchesDay = currentDayOfWeek == template.dayOfWeek
                    val notGeneratedThisWeek = template.lastGeneratedDate == null ||
                            template.lastGeneratedDate < DateUtils.getStartOfWeek(now)
                    matchesDay && notGeneratedThisWeek
                }

                RecurringFrequency.MONTHLY -> {
                    // Due if today >= dayOfMonth AND not generated in this calendar month
                    val targetDay = template.dayOfMonth.coerceAtMost(calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    val matchesOrPassedDay = currentDayOfMonth >= targetDay
                    val notGeneratedThisMonth = template.lastGeneratedDate == null ||
                            !DateUtils.isSameMonth(template.lastGeneratedDate, now)
                    matchesOrPassedDay && notGeneratedThisMonth
                }
            }

            if (isDue) {
                // Auto-create transaction
                val transaction = Transaction(
                    date = now,
                    type = template.type,
                    amount = template.amount,
                    title = template.title,
                    category = template.category,
                    note = template.note ?: "Auto-generated recurring entry"
                )
                repository.insertTransaction(transaction)
                repository.updateRecurringLastGenerated(template.id, now)
                generatedTitles.add("${template.title} (${CurrencyUtils.format(template.amount)})")
            }
        }

        return generatedTitles
    }
}
