package com.example.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    // Configurable currency symbol
    var currencySymbol: String = "Rs."

    private val formatter = (NumberFormat.getNumberInstance(Locale.US) as DecimalFormat).apply {
        applyPattern("#,##0.##")
    }

    fun format(amount: Double): String {
        return "$currencySymbol ${formatter.format(amount)}"
    }

    fun formatWithoutSymbol(amount: Double): String {
        return formatter.format(amount)
    }

    fun formatSigned(amount: Double): String {
        val prefix = if (amount > 0) "+ " else if (amount < 0) "- " else ""
        val absAmount = kotlin.math.abs(amount)
        return "$prefix$currencySymbol ${formatter.format(absAmount)}"
    }
}
