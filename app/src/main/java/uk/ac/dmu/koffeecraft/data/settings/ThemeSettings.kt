package uk.ac.dmu.koffeecraft.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeSettings {

    private const val PREFS_NAME = "koffeecraft_ui_settings"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    fun applySavedTheme(context: Context) {
        applyThemeMode(isDarkModeEnabled(context))
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        applyThemeMode(enabled)
    }

    private fun applyThemeMode(isDarkModeEnabled: Boolean) {
        val mode = if (isDarkModeEnabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }
}