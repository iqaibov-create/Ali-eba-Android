package com.alieba.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AzanScheduler {

    private val prayers = listOf(
        "Fəcr" to "05:19",
        "Zöhr" to "12:55",
        "Əsr" to "16:19",
        "Məğrib" to "19:26",
        "İşa" to "00:14"
    )

    fun scheduleAll(context: Context) {
        val prefs = context.getSharedPreferences(
            "prayer_settings",
            Context.MODE_PRIVATE
        )

        prayers.forEachIndexed { index, (name, defaultTime) ->
            val time = prefs.getString(
                "${name}_time",
                defaultTime
            ) ?: defaultTime

            schedulePrayer(context, name, time, 7000 + index)
        }
    }

    private fun schedulePrayer(
        context: Context,
        prayer: String,
        time: String,
        requestCode: Int
    ) {
        val parts = time.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AzanReceiver::class.java).apply {
            putExtra("prayer", prayer)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
