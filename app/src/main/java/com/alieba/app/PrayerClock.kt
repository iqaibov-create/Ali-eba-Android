package com.alieba.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Prayer alarm scheduler.
 *
 * V19.7:
 * - uses AlarmClockInfo when exact alarms are available so Doze / locked screen cannot defer azan;
 * - keeps a second allow-while-idle fallback alarm;
 * - recreates alarms after reboot, date/time/timezone and app updates;
 * - never replays a prayer that is already in the past when the app is opened.
 */
object PrayerClock {
    private const val PREF = "alieba_prayer_clock"

    val displayNames = listOf("Fəcr", "Günəş\ndoğuşu", "Zöhr", "Günəş\nbatımı", "Məğrib", "Gecə\nyarısı")
    val keys = listOf("fajr", "sunrise", "dhuhr", "sunset", "maghrib", "midnight")
    private val fetchKeys = (keys + listOf("asr", "isha")).distinct()

    private val alarmNames = listOf("Fəcr", "Zöhr", "Əsr", "Məğrib", "İşa")
    private val alarmKeys = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

    fun activeKey(c: Context): String? {
        val t = times(c)
        if (t.isEmpty()) return null
        val now = Calendar.getInstance().run {
            get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)
        }
        return keys.mapNotNull { k ->
            val m = Regex("""^([0-2]\d):([0-5]\d)$""").matchEntire(t[k] ?: "")
                ?: return@mapNotNull null
            val h = m.groupValues[1].toInt()
            if (h > 23) return@mapNotNull null
            val minute = h * 60 + m.groupValues[2].toInt()
            if (minute <= now) k to minute else null
        }.maxByOrNull { it.second }?.first
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())

    private fun dateKey(c: Context): String {
        val s = c.getSharedPreferences("alieba_setup", Context.MODE_PRIVATE)
        return today() + "|" + TimeZone.getDefault().id + "|" +
            s.getString("latitude", "") + "|" + s.getString("longitude", "")
    }

    fun times(c: Context): Map<String, String> {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (p.getString("date_key", "") != dateKey(c)) return emptyMap()
        return fetchKeys.mapNotNull { k ->
            p.getString(k, null)?.let { k to it }
        }.toMap()
    }

    fun fetchAndSchedule(c: Context, done: ((Boolean) -> Unit)? = null) {
        Thread {
            var ok = false
            try {
                val s = c.getSharedPreferences("alieba_setup", Context.MODE_PRIVATE)
                val lat = s.getString("latitude", null)?.toDoubleOrNull()
                val lon = s.getString("longitude", null)?.toDoubleOrNull()

                if (lat != null && lon != null &&
                    lat in -90.0..90.0 && lon in -180.0..180.0
                ) {
                    val tz = java.net.URLEncoder.encode(TimeZone.getDefault().id, "UTF-8")
                    val url = URL("https://alieba.ge/api/prayer-times.php?lat=$lat&lng=$lon&tz=$tz")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                        setRequestProperty("Accept", "application/json")
                    }

                    try {
                        if (conn.responseCode == 200) {
                            val json = JSONObject(
                                conn.inputStream.bufferedReader().use { it.readText() }
                            )
                            val first = when {
                                json.optJSONArray("days") != null ->
                                    json.getJSONArray("days").optJSONObject(0)

                                json.optJSONArray("results") != null ->
                                    json.getJSONArray("results").optJSONObject(0)
                                        ?.optJSONArray("days")?.optJSONObject(0)

                                else ->
                                    json.optJSONObject("data")
                                        ?.optJSONArray("days")?.optJSONObject(0)
                            }

                            val responseDate = first?.optString("date", "") ?: ""
                            val t = first?.optJSONObject("local") ?: first

                            if (t != null && (responseDate.isEmpty() || responseDate == today())) {
                                val parsed = fetchKeys.mapNotNull { key ->
                                    val raw = if (key == "midnight") {
                                        t.optString(
                                            "midnight",
                                            t.optString("night_midpoint", "")
                                        ).trim()
                                    } else {
                                        t.optString(key, "").trim()
                                    }

                                    val hm = Regex(
                                        """(?:^|T|\s)([0-2]\d:[0-5]\d)"""
                                    ).find(raw)?.groupValues?.get(1)

                                    if (hm != null && hm.substring(0, 2).toInt() <= 23) {
                                        key to hm
                                    } else null
                                }.toMap()

                                if (listOf("fajr", "sunrise", "dhuhr", "maghrib")
                                        .all { parsed.containsKey(it) }
                                ) {
                                    c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                                        .edit().apply {
                                            fetchKeys.forEach { remove(it) }
                                            putString("date_key", dateKey(c))
                                            parsed.forEach { (k, v) -> putString(k, v) }
                                        }.commit()

                                    scheduleToday(c, parsed)
                                    cancelRetry(c)
                                    ok = true
                                }
                            }
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
            } catch (_: Exception) {
            }

            if (!ok) {
                val cached = times(c)
                if (cached.isNotEmpty()) scheduleToday(c, cached)
                scheduleRetry(c)
            }

            done?.let {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    it(ok)
                }
            }
        }.start()
    }

    private fun pending(
        c: Context,
        code: Int,
        intent: Intent
    ): PendingIntent = PendingIntent.getBroadcast(
        c,
        code,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun showIntent(c: Context, code: Int): PendingIntent =
        PendingIntent.getActivity(
            c,
            code,
            Intent(c, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun schedulePrimary(
        c: Context,
        am: AlarmManager,
        at: Long,
        op: PendingIntent,
        showCode: Int
    ) {
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                // Strongest Android alarm API for a user-visible alarm.
                // It wakes from Doze and while the screen is locked.
                am.setAlarmClock(
                    AlarmManager.AlarmClockInfo(at, showIntent(c, showCode)),
                    op
                )
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        }
    }

    fun scheduleToday(c: Context, t: Map<String, String> = times(c)) {
        if (t.isEmpty()) {
            scheduleRefresh(c)
            scheduleRetry(c)
            return
        }

        val am = c.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        val enabled = c.getSharedPreferences(
            "alieba_setup",
            Context.MODE_PRIVATE
        ).getBoolean("notifications", false)

        for (i in alarmKeys.indices) {
            val name = alarmNames[i]

            val baseIntent = Intent(c, AzanReceiver::class.java)
                .putExtra("prayer", name)
                .putExtra("date", today())

            val primary = pending(c, 7000 + i, baseIntent)
            val fallback = pending(
                c,
                7100 + i,
                Intent(c, AzanReceiver::class.java)
                    .putExtra("prayer", name)
                    .putExtra("date", today())
                    .putExtra("fallback", true)
            )

            am.cancel(primary)
            am.cancel(fallback)

            if (!enabled || !AzanPrefs.isPrayerEnabled(c, name)) continue

            val hm = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")
                .matchEntire(t[alarmKeys[i]] ?: "") ?: continue

            val at = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hm.groupValues[1].toInt())
                set(Calendar.MINUTE, hm.groupValues[2].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (at <= now) continue

            schedulePrimary(c, am, at, primary, 7200 + i)

            // Independent backup. AzanPlaybackService deduplicates it if
            // the primary alarm already played.
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                at + 2 * 60 * 1000L,
                fallback
            )
        }

        scheduleRefresh(c)
    }

    fun cancelPrayerBackups(c: Context, prayer: String) {
        val index = alarmNames.indexOf(prayer)
        if (index < 0) return
        val am = c.getSystemService(AlarmManager::class.java)
        val base = Intent(c, AzanReceiver::class.java)
            .putExtra("prayer", prayer)
            .putExtra("date", today())

        am.cancel(pending(c, 7000 + index, base))
        am.cancel(
            pending(
                c,
                7100 + index,
                Intent(c, AzanReceiver::class.java)
                    .putExtra("prayer", prayer)
                    .putExtra("date", today())
                    .putExtra("fallback", true)
            )
        )
    }

    private fun scheduleRetry(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        val p = pending(c, 7801, Intent(c, PrayerRefreshReceiver::class.java))
        am.cancel(p)
        am.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 30 * 60 * 1000L,
            p
        )
    }

    private fun cancelRetry(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(
            pending(c, 7801, Intent(c, PrayerRefreshReceiver::class.java))
        )
    }

    fun scheduleRefresh(c: Context) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val am = c.getSystemService(AlarmManager::class.java)
        val p = pending(c, 7800, Intent(c, PrayerRefreshReceiver::class.java))
        am.cancel(p)

        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    p
                )
            } else {
                am.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    p
                )
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                p
            )
        }
    }
}
