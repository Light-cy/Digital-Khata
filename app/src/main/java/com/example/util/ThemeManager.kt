package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM_DEFAULT,
    LIGHT,
    DARK
}

class ThemeManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("digital_khata_theme_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "app_theme_mode"
    }

    private val _themeMode = MutableStateFlow(getStoredTheme())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun getStoredTheme(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM_DEFAULT.name)
        return try {
            AppThemeMode.valueOf(name ?: AppThemeMode.SYSTEM_DEFAULT.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM_DEFAULT
        }
    }
}
