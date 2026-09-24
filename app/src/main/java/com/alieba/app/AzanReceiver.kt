package com.alieba.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
                .getBoolean("notifications", false)) return
        // Today's stored schedule must exist, even if MainActivity is not running.
        if (PrayerClock.times(context).isEmpty()) return
        try {
            val play = Intent(context, AzanPlaybackService::class.java).putExtra("prayer", prayer)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(play)
            else context.startService(play)
        } catch (error: Exception) {
            // Do not crash an alarm broadcast if the device forbids background starts.
            Log.w("AliebaAzan", "Android azan servisini başlatmadı", error)
        }
    }
}
