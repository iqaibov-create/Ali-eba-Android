package com.alieba.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AzanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra("prayer") ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (intent.getStringExtra("date") != date) return
        if (!AzanPrefs.isPrayerEnabled(context, prayer)) return
        if (!context.getSharedPreferences("alieba_setup", Context.MODE_PRIVATE)
                .getBoolean("notifications", false)
        ) return
        if (PrayerClock.times(context).isEmpty()) return

        val power = context.getSystemService(PowerManager::class.java)
        val wake = power.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Alieba:AzanReceiver"
        ).apply {
            setReferenceCounted(false)
            acquire(45_000L)
        }

        try {
            // Whichever of primary/fallback fires first cancels the sibling.
            PrayerClock.cancelPrayerBackups(context, prayer)

            val play = Intent(context, AzanPlaybackService::class.java)
                .putExtra("prayer", prayer)
                .putExtra("date", date)

            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(play)
            } else {
                context.startService(play)
            }
        } catch (error: Exception) {
            Log.w("AliebaAzan", "Android azan servisini başlatmadı", error)
        } finally {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (wake.isHeld) wake.release()
                } catch (_: Exception) {
                }
            }, 12_000L)
        }
    }
}
