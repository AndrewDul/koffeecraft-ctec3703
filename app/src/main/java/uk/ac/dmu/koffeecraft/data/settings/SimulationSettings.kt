package uk.ac.dmu.koffeecraft.data.settings

import android.content.Context

object SimulationSettings {

    private const val PREFS_NAME = "koffeecraft_admin_settings"
    private const val KEY_SIMULATION_ENABLED = "simulation_enabled"

    // I keep simulation enabled by default for solo/demo mode.
    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SIMULATION_ENABLED, true)
    }

    // I persist the admin's choice so it survives app restarts.
    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SIMULATION_ENABLED, enabled).apply()
    }
}