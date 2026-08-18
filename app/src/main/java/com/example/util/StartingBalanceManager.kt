package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PaymentMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StartingBalanceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _cashStartingBalance = MutableStateFlow(loadCashStartingBalance())
    val cashStartingBalance: StateFlow<Double> = _cashStartingBalance.asStateFlow()

    private val _accountStartingBalance = MutableStateFlow(loadAccountStartingBalance())
    val accountStartingBalance: StateFlow<Double> = _accountStartingBalance.asStateFlow()

    private val _startingDateMillis = MutableStateFlow(loadStartingDateMillis())
    val startingDateMillis: StateFlow<Long> = _startingDateMillis.asStateFlow()

    private val _lastUsedPaymentMode = MutableStateFlow(loadLastUsedPaymentMode())
    val lastUsedPaymentMode: StateFlow<PaymentMode> = _lastUsedPaymentMode.asStateFlow()

    private fun loadCashStartingBalance(): Double {
        return prefs.getString(KEY_CASH_STARTING, "0.0")?.toDoubleOrNull() ?: 0.0
    }

    private fun loadAccountStartingBalance(): Double {
        return prefs.getString(KEY_ACCOUNT_STARTING, "0.0")?.toDoubleOrNull() ?: 0.0
    }

    private fun loadStartingDateMillis(): Long {
        return prefs.getLong(KEY_STARTING_DATE, 0L)
    }

    private fun loadLastUsedPaymentMode(): PaymentMode {
        val name = prefs.getString(KEY_LAST_PAYMENT_MODE, PaymentMode.CASH.name)
        return try {
            if (name != null) PaymentMode.valueOf(name) else PaymentMode.CASH
        } catch (e: Exception) {
            PaymentMode.CASH
        }
    }

    fun setStartingBalances(cash: Double, account: Double, dateMillis: Long = System.currentTimeMillis()) {
        val finalCash = cash.coerceAtLeast(0.0)
        val finalAccount = account.coerceAtLeast(0.0)
        prefs.edit()
            .putString(KEY_CASH_STARTING, finalCash.toString())
            .putString(KEY_ACCOUNT_STARTING, finalAccount.toString())
            .putLong(KEY_STARTING_DATE, dateMillis)
            .apply()

        _cashStartingBalance.value = finalCash
        _accountStartingBalance.value = finalAccount
        _startingDateMillis.value = dateMillis
    }

    fun setLastUsedPaymentMode(mode: PaymentMode) {
        prefs.edit()
            .putString(KEY_LAST_PAYMENT_MODE, mode.name)
            .apply()
        _lastUsedPaymentMode.value = mode
    }

    fun getCashStartingBalance(): Double = _cashStartingBalance.value
    fun getAccountStartingBalance(): Double = _accountStartingBalance.value
    fun getTotalStartingBalance(): Double = _cashStartingBalance.value + _accountStartingBalance.value
    fun getStartingDateMillis(): Long = _startingDateMillis.value
    fun getLastUsedPaymentMode(): PaymentMode = _lastUsedPaymentMode.value

    fun clearStartingBalances() {
        prefs.edit()
            .remove(KEY_CASH_STARTING)
            .remove(KEY_ACCOUNT_STARTING)
            .remove(KEY_STARTING_DATE)
            .apply()
        _cashStartingBalance.value = 0.0
        _accountStartingBalance.value = 0.0
        _startingDateMillis.value = 0L
    }

    companion object {
        private const val PREFS_NAME = "digital_khata_starting_balances"
        private const val KEY_CASH_STARTING = "key_cash_starting_balance"
        private const val KEY_ACCOUNT_STARTING = "key_account_starting_balance"
        private const val KEY_STARTING_DATE = "key_starting_date_millis"
        private const val KEY_LAST_PAYMENT_MODE = "key_last_used_payment_mode"
    }
}
