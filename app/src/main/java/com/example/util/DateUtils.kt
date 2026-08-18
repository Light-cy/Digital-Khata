package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val displayDateFormatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    private val shortDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dayOfWeekFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    private val dayNumberFormatter = SimpleDateFormat("dd", Locale.getDefault())
    private val csvDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun formatDisplayDate(timeMillis: Long): String {
        val calendar = Calendar.getInstance()
        val today = getStartOfDay(System.currentTimeMillis())
        val targetDay = getStartOfDay(timeMillis)

        val diffDays = ((targetDay - today) / (1000 * 60 * 60 * 24)).toInt()
        return when (diffDays) {
            0 -> "Today (${shortDateFormatter.format(Date(timeMillis))})"
            -1 -> "Yesterday (${shortDateFormatter.format(Date(timeMillis))})"
            1 -> "Tomorrow (${shortDateFormatter.format(Date(timeMillis))})"
            else -> displayDateFormatter.format(Date(timeMillis))
        }
    }

    fun formatShortDate(timeMillis: Long): String {
        return shortDateFormatter.format(Date(timeMillis))
    }

    fun formatMonthYear(timeMillis: Long): String {
        return monthYearFormatter.format(Date(timeMillis))
    }

    fun formatDayOfWeek(timeMillis: Long): String {
        return dayOfWeekFormatter.format(Date(timeMillis))
    }

    fun formatDayNumber(timeMillis: Long): String {
        return dayNumberFormatter.format(Date(timeMillis))
    }

    fun formatCsvDate(timeMillis: Long): String {
        return csvDateFormatter.format(Date(timeMillis))
    }

    fun parseCsvDate(dateString: String): Long? {
        return try {
            csvDateFormatter.parse(dateString)?.time
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)?.time
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun getStartOfDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun addDays(timeMillis: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            add(Calendar.DAY_OF_YEAR, days)
        }
        return cal.timeInMillis
    }

    fun getStartOfWeek(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = timeMillis
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfWeek(timeMillis: Long): Long {
        val startOfWeek = getStartOfWeek(timeMillis)
        return getEndOfDay(addDays(startOfWeek, 6))
    }

    fun addWeeks(timeMillis: Long, weeks: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            add(Calendar.WEEK_OF_YEAR, weeks)
        }
        return cal.timeInMillis
    }

    fun getStartOfMonth(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfMonth(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun addMonths(timeMillis: Long, months: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            add(Calendar.MONTH, months)
        }
        return cal.timeInMillis
    }

    fun isSameDay(timeMillis1: Long, timeMillis2: Long): Boolean {
        return getStartOfDay(timeMillis1) == getStartOfDay(timeMillis2)
    }

    fun isSameMonth(timeMillis1: Long, timeMillis2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = timeMillis1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = timeMillis2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
    }

    fun formatRelativeTime(timeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = (now - timeMillis).coerceAtLeast(0)
        val diffSeconds = diffMillis / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays == 1L -> "Yesterday"
            diffDays < 7 -> "${diffDays}d ago"
            else -> shortDateFormatter.format(Date(timeMillis))
        }
    }
}
