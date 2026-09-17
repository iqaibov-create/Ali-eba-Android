package com.alieba.app

import android.content.Context

object AzanPrefs {

    private const val PREFS = "azan_settings"

    fun sound(c: Context): String =
        c.getSharedPreferences(PREFS, 0)
            .getString("sound", "shia") ?: "shia"

    fun setSound(c: Context, v: String) {
        c.getSharedPreferences(PREFS, 0)
            .edit()
            .putString("sound", v)
            .apply()
    }

    fun isPrayerEnabled(c: Context, prayer: String): Boolean =
        c.getSharedPreferences(PREFS, 0)
            .getBoolean("prayer_enabled_$prayer", true)

    fun setPrayerEnabled(c: Context, prayer: String, enabled: Boolean) {
        c.getSharedPreferences(PREFS, 0)
            .edit()
            .putBoolean("prayer_enabled_$prayer", enabled)
            .apply()
    }

    fun res(c: Context): Int =
        when (sound(c)) {
            "beautiful1" -> R.raw.azan_beautiful_1
            "beautiful2" -> R.raw.azan_beautiful_2
            else -> R.raw.azan_shia
        }
}
