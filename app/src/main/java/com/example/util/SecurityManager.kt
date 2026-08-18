package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("digital_khata_security", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_ENABLED = "is_pin_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "is_biometric_enabled"
        private const val KEY_LOCK_TIMEOUT_SECONDS = "lock_timeout_seconds"
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"
        
        // Defaults: 0s (Immediately upon locking / closing app)
        const val DEFAULT_TIMEOUT_SECONDS = 0
    }

    // In-memory flag that is always false on cold start/reboot
    private var isSessionUnlocked: Boolean = false

    var isPinEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PIN_ENABLED, value).apply()
            if (!value) {
                isSessionUnlocked = false
            }
        }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var lockTimeoutSeconds: Int
        get() = prefs.getInt(KEY_LOCK_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS)
        set(value) = prefs.edit().putInt(KEY_LOCK_TIMEOUT_SECONDS, value).apply()

    fun isPinSet(): Boolean = isPinEnabled

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_PIN_ENABLED, true)
            .apply()
        isSessionUnlocked = true
    }

    fun removePin() {
        disablePin()
    }

    fun disablePin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
        isSessionUnlocked = false
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val isValid = storedHash == hashPin(pin)
        if (isValid) {
            unlock()
        }
        return isValid
    }

    fun isLocked(): Boolean = shouldLock()

    fun isAppLocked(): Boolean = shouldLock()

    fun shouldLock(): Boolean {
        if (!isPinEnabled) return false
        
        // Cold start or reboot session has not been unlocked yet
        if (!isSessionUnlocked) return true

        val lastBackground = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (lastBackground == 0L) return false

        val timeoutMillis = lockTimeoutSeconds * 1000L
        val elapsed = System.currentTimeMillis() - lastBackground
        return elapsed >= timeoutMillis
    }

    fun unlock() {
        isSessionUnlocked = true
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, 0L).apply()
    }

    fun onAppBackgrounded() {
        if (isPinEnabled) {
            prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, System.currentTimeMillis()).apply()
        }
    }

    fun onAppForegrounded(): Boolean {
        val lockRequired = shouldLock()
        if (lockRequired) {
            isSessionUnlocked = false
        }
        return lockRequired
    }

    fun forceLock() {
        isSessionUnlocked = false
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, 1L).apply()
    }

    fun updateActiveTime() {
        // App is actively in foreground
        if (isSessionUnlocked) {
            prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, 0L).apply()
        }
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
