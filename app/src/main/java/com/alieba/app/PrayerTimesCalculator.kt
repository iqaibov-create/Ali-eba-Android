package com.alieba.app

/** Data holder retained for compatibility. Times are fetched from alieba.ge. */
object PrayerTimesCalculator {
    data class Times(val fajr:String,val sunrise:String,val dhuhr:String,val asr:String,val maghrib:String,val isha:String)
}
