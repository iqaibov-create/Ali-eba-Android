package com.alieba.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AzanScheduler {
    fun refreshTimes(context: Context): PrayerTimesCalculator.Times? {
        val setup = context.getSharedPreferences("alieba_setup", Context.MODE_PRIVATE)
        val lat = setup.getString("latitude", null)?.toDoubleOrNull() ?: return null
        val lon = setup.getString("longitude", null)?.toDoubleOrNull() ?: return null
        val times = PrayerTimesCalculator.calculate(lat, lon)
        context.getSharedPreferences("prayer_settings", Context.MODE_PRIVATE).edit()
            .putString("Fəcr_time", times.fajr)
            .putString("Günəş_time", times.sunrise)
            .putString("Zöhr_time", times.dhuhr)
            .putString("Əsr_time", times.asr)
            .putString("Məğrib_time", times.maghrib)
            .putString("İşa_time", times.isha)
            .apply()
        return times
    }

    fun scheduleAll(context: Context) {
        val times = refreshTimes(context) ?: return
        val prayers = listOf(
            "Fəcr" to times.fajr,
            "Zöhr" to times.dhuhr,
            "Əsr" to times.asr,
            "Məğrib" to times.maghrib,
            "İşa" to times.isha
        )
        prayers.forEachIndexed { index, pair ->
            schedulePrayer(context, pair.first, pair.second, 7000 + index)
        }
    }

    private fun schedulePrayer(context: Context, prayer: String, time: String, requestCode: Int) {
        val parts = time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(context, AzanReceiver::class.java).putExtra("prayer", prayer)
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
        }
    }
}
