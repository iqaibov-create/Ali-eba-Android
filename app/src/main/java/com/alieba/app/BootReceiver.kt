package com.alieba.app

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val actions=setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
        if(i.action !in actions) return
        if(i.action==AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED &&
            Build.VERSION.SDK_INT>=31 &&
            !c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) return
        // Saved prayer alarms are recreated without requiring MainActivity to be opened.
        PrayerClock.scheduleToday(c.applicationContext)
        val pendingResult=goAsync()
        PrayerClock.fetchAndSchedule(c.applicationContext) { pendingResult.finish() }
    }
}
