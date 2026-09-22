package com.alieba.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerRefreshReceiver: BroadcastReceiver() {
    override fun onReceive(context:Context, intent:Intent) {
        val result=goAsync()
        PrayerClock.fetchAndSchedule(context.applicationContext) { result.finish() }
        PrayerClock.scheduleRefresh(context)
    }
}
