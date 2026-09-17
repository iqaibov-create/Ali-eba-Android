package com.alieba.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.NotificationCompat

class AzanReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val prayer = i.getStringExtra("prayer") ?: "Namaz"
        // Always refresh tomorrow's alarms, even when this prayer is disabled.
        AzanScheduler.scheduleAll(c)
        if (!AzanPrefs.isPrayerEnabled(c, prayer)) return

        val manager = c.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel("azan") == null) {
            manager.createNotificationChannel(NotificationChannel("azan", "Azan və namaz vaxtları", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Namaz vaxtı bildirişləri"
                setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            })
        }
        val open = PendingIntent.getActivity(c, 9001, Intent(c, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(c, "azan")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Ali-eba • $prayer vaxtıdır")
            .setContentText("Azanı açmaq üçün toxunun")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(prayer.hashCode(), notification)
        MediaPlayer.create(c, AzanPrefs.res(c))?.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }
}
