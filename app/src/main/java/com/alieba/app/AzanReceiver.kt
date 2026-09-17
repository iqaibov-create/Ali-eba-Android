package com.alieba.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.app.NotificationCompat

class AzanReceiver : BroadcastReceiver() {

    override fun onReceive(c: Context, i: Intent) {
        val prayer = i.getStringExtra("prayer") ?: "Namaz"

        if (!AzanPrefs.isPrayerEnabled(c, prayer)) {
            return
        }

        val notification = NotificationCompat.Builder(c, "azan")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Ali-eba • $prayer vaxtıdır")
            .setContentText("Namaz vaxtı daxil oldu")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        c.getSystemService(NotificationManager::class.java)
            .notify(prayer.hashCode(), notification)

        MediaPlayer.create(c, AzanPrefs.res(c))?.apply {
            setOnCompletionListener { it.release() }
            start()
        }

        // Növbəti günün namaz alarmını yenidən planlaşdır.
        AzanScheduler.scheduleAll(c)
    }
}
