package com.alieba.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver:BroadcastReceiver(){
    override fun onReceive(c:Context,i:Intent){
        if(i.action in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_TIME_CHANGED)) {
            PrayerClock.scheduleRefresh(c)
            val pending=goAsync()
            PrayerClock.fetchAndSchedule(c.applicationContext) { pending.finish() }
        }
    }
}
