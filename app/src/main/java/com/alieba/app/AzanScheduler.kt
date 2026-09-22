package com.alieba.app

import android.content.Context

object AzanScheduler {
    fun scheduleAll(context:Context){PrayerClock.fetchAndSchedule(context)}
    fun refreshTimes(context:Context):PrayerTimesCalculator.Times? {
        val t=PrayerClock.times(context)
        if(t.isEmpty())return null
        return PrayerTimesCalculator.Times(t["fajr"]?:"--:--",t["sunrise"]?:"--:--",t["dhuhr"]?:"--:--",t["asr"]?:"--:--",t["maghrib"]?:"--:--",t["isha"]?:"--:--")
    }
}
