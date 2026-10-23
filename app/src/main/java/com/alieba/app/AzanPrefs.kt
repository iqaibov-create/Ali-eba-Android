package com.alieba.app

import android.content.Context

object AzanPrefs {
    private const val PREFS = "azan_settings"

    /** Migrate installs that previously selected removed Azan 1. */
    fun sound(c: Context): String {
        val prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val selected = prefs.getString("sound", "shia") ?: "shia"
        if (selected == "beautiful1") {
            prefs.edit().putString("sound", "beautiful2").apply()
            return "beautiful2"
        }
        return selected
    }

    fun setSound(c: Context, v: String) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("sound", v).apply()
    }

    fun isPrayerEnabled(c: Context, prayer: String): Boolean =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("prayer_enabled_$prayer", true)

    fun setPrayerEnabled(c: Context, prayer: String, enabled: Boolean) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("prayer_enabled_$prayer", enabled).apply()
    }

    fun res(c: Context): Int = when (sound(c)) {
        "beautiful2" -> R.raw.azan_beautiful_2
        else -> R.raw.azan_shia
    }
}
