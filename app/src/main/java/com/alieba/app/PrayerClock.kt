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

/** The same public prayer API used by alieba.ge. Never infer timings from a stale day. */
object PrayerClock {
    private const val PREF = "alieba_prayer_clock"
    val displayNames = listOf("Fəcr", "Günəş\ndoğuşu", "Zöhr", "Günəş\nbatımı", "Məğrib", "Gecə\nyarısı")
    val keys = listOf("fajr", "sunrise", "dhuhr", "sunset", "maghrib", "midnight")
    private val fetchKeys=(keys + listOf("asr", "isha")).distinct()
    /** Highlight the current time segment only from today's confirmed API data. */
    fun activeKey(c: Context):String? {
        val t=times(c)
        if(t.isEmpty())return null
        val now=Calendar.getInstance().run{get(Calendar.HOUR_OF_DAY)*60+get(Calendar.MINUTE)}
        val passed=keys.mapNotNull { k ->
            val m=Regex("""^([0-2]\d):([0-5]\d)$""").matchEntire(t[k]?:"")
                ?:return@mapNotNull null
            val h=m.groupValues[1].toInt()
            if(h>23)return@mapNotNull null
            val minute=h*60+m.groupValues[2].toInt()
            if(minute<=now) k to minute else null
        }
        return passed.maxByOrNull{it.second}?.first
    }
    private val alarmNames = listOf("Fəcr", "Zöhr", "Əsr", "Məğrib", "İşa")
    private val alarmKeys = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
    private fun dateKey(c: Context): String {
        val s=c.getSharedPreferences("alieba_setup", Context.MODE_PRIVATE)
        return today()+"|"+TimeZone.getDefault().id+"|"+s.getString("latitude", "")+"|"+s.getString("longitude", "")
    }
    fun times(c: Context): Map<String,String> {
        val p=c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (p.getString("date_key", "") != dateKey(c)) return emptyMap()
        return fetchKeys.mapNotNull { k -> p.getString(k, null)?.let { k to it } }.toMap()
    }
    fun fetchAndSchedule(c: Context, done: ((Boolean)->Unit)? = null) {
        Thread {
            var ok=false
            try {
                val s=c.getSharedPreferences("alieba_setup", Context.MODE_PRIVATE)
                val lat=s.getString("latitude", null)?.toDoubleOrNull()
                val lon=s.getString("longitude", null)?.toDoubleOrNull()
                if(lat!=null && lon!=null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    val tz=java.net.URLEncoder.encode(TimeZone.getDefault().id, "UTF-8")
                    val url=URL("https://alieba.ge/api/prayer-times.php?lat=$lat&lng=$lon&tz=$tz")
                    val conn=(url.openConnection() as HttpURLConnection).apply {
                        connectTimeout=3500;readTimeout=3500;setRequestProperty("Accept", "application/json")
                    }
                    try {
                        if(conn.responseCode==200) {
                            val json=JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                            val first = when {
                                json.optJSONArray("days") != null -> json.getJSONArray("days").optJSONObject(0)
                                json.optJSONArray("results") != null -> json.getJSONArray("results").optJSONObject(0)?.optJSONArray("days")?.optJSONObject(0)
                                else -> json.optJSONObject("data")?.optJSONArray("days")?.optJSONObject(0)
                            }
                            val responseDate=first?.optString("date", "") ?: ""
                            val t=first?.optJSONObject("local") ?: first
                            // The site cache can return the previous date around midnight.
                            if(t != null && (responseDate.isEmpty() || responseDate==today())) {
                                val parsed=fetchKeys.mapNotNull { key ->
                                    val raw=if(key=="midnight") t.optString("midnight",t.optString("night_midpoint","")).trim() else t.optString(key, "").trim()
                                    val hm=Regex("(?:^|T|\\s)([0-2]\\d:[0-5]\\d)").find(raw)?.groupValues?.get(1)
                                    if(hm != null && hm.substring(0,2).toInt() <= 23) key to hm else null
                                }.toMap()
                                if(listOf("fajr","sunrise","dhuhr","maghrib").all { parsed.containsKey(it) }) {
                                    // Clear optional fields from previous API responses before writing today's data.
                                    c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().apply {
                                        fetchKeys.forEach { remove(it) }
                                        putString("date_key",dateKey(c))
                                        parsed.forEach { (k,v) -> putString(k,v) }
                                    }.commit()
                                    scheduleToday(c,parsed)
                                    // An old retry must not wake the phone once today's data is available.
                                    c.getSystemService(AlarmManager::class.java).cancel(
                                        pending(c,7801,Intent(c,PrayerRefreshReceiver::class.java))
                                    )
                                    ok=true
                                }
                            }
                        }
                    } finally { conn.disconnect() }
                }
            } catch (_: Exception) { }
            if(!ok) {
                // A service outage or a late midnight refresh must recover while the app is closed.
                // Keep today's valid cached alarms; retry the API later for fresh data.
                val cached=times(c)
                if(cached.isNotEmpty()) scheduleToday(c,cached)
                scheduleRetry(c)
            }
            if(done != null) {
                val main=android.os.Handler(android.os.Looper.getMainLooper())
                main.post { done(ok) }
            }
        }.start()
    }
    private fun pending(c:Context,code:Int, intent:Intent):PendingIntent = PendingIntent.getBroadcast(c,code,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun scheduleToday(c:Context, t: Map<String,String> = times(c)) {
        if(t.isEmpty()) { scheduleRefresh(c); scheduleRetry(c); return }
        val am=c.getSystemService(AlarmManager::class.java)
        val now=System.currentTimeMillis()
        for (i in alarmKeys.indices) {
            val name=alarmNames[i]
            val p=pending(c,7000+i,Intent(c,AzanReceiver::class.java).putExtra("prayer",name).putExtra("date",today()))
            am.cancel(p)
            if(!c.getSharedPreferences("alieba_setup",Context.MODE_PRIVATE).getBoolean("notifications",false)
                || !AzanPrefs.isPrayerEnabled(c,name)) continue
            val hm=Regex("^([01]\\d|2[0-3]):([0-5]\\d)$").matchEntire(t[alarmKeys[i]] ?: "") ?: continue
            val at=Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY,hm.groupValues[1].toInt())
                set(Calendar.MINUTE,hm.groupValues[2].toInt())
                set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)
            }.timeInMillis
            if(at<=now) continue // don't replay a past prayer on app launch
            try {
                if(Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms())
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)
            } catch (_:SecurityException) {
                // The exact-alarm permission can be revoked between checking and scheduling.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)
            }
        }
        scheduleRefresh(c)
    }
    private fun scheduleRetry(c:Context){
        // Re-check while the app is closed. A single failed fetch must not disable tomorrow's azan.
        val at=System.currentTimeMillis()+30*60*1000L
        val am=c.getSystemService(AlarmManager::class.java)
        val p=pending(c,7801,Intent(c,PrayerRefreshReceiver::class.java))
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)
    }
    fun scheduleRefresh(c: Context) {
        val calendar=Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,5);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }
        val am=c.getSystemService(AlarmManager::class.java)
        val p=pending(c,7800,Intent(c,PrayerRefreshReceiver::class.java))
        am.cancel(p)
        try {
            if(Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms())
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,p)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,p)
        } catch (_:SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,p)
        }
    }
}
