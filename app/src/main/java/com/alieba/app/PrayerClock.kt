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
    val displayNames = listOf("Fəcr", "Günəş", "Zöhr", "Əsr", "Məğrib", "İşa")
    val keys = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
    private val fetchKeys=keys + listOf("sunset")
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
                        connectTimeout=8000;readTimeout=8000;setRequestProperty("Accept", "application/json")
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
                                    val raw=t.optString(key, "").trim()
                                    val hm=Regex("(?:^|T|\\s)([0-2]\\d:[0-5]\\d)").find(raw)?.groupValues?.get(1)
                                    if(hm != null && hm.substring(0,2).toInt() <= 23) key to hm else null
                                }.toMap()
                                if(listOf("fajr","sunrise","dhuhr","maghrib").all { parsed.containsKey(it) }) {
                                    c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().apply {
                                        putString("date_key",dateKey(c))
                                        parsed.forEach { (k,v) -> putString(k,v) }
                                    }.apply()
                                    scheduleToday(c,parsed)
                                    ok=true
                                }
                            }
                        }
                    } finally { conn.disconnect() }
                }
            } catch (_: Exception) { }
            if(!ok && times(c).isEmpty()) scheduleRetry(c)
            if(done != null) {
                val main=android.os.Handler(android.os.Looper.getMainLooper())
                main.post { done(ok) }
            }
        }.start()
    }
    private fun pending(c:Context,code:Int, intent:Intent):PendingIntent = PendingIntent.getBroadcast(c,code,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun scheduleToday(c:Context, t: Map<String,String> = times(c)) {
        if(t.isEmpty()) { scheduleRefresh(c); return }
        val am=c.getSystemService(AlarmManager::class.java)
        val now=System.currentTimeMillis()
        for (i in alarmKeys.indices) {
            val name=alarmNames[i]
            val p=pending(c,7000+i,Intent(c,AzanReceiver::class.java).putExtra("prayer",name).putExtra("date",today()))
            am.cancel(p)
            if(!AzanPrefs.isPrayerEnabled(c,name)) continue
            val x=t[alarmKeys[i]]?.split(':') ?: continue
            if(x.size!=2) continue
            val at=Calendar.getInstance().apply {set(Calendar.HOUR_OF_DAY,x[0].toInt());set(Calendar.MINUTE,x[1].toInt());set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis
            if(at<=now) continue // never schedule yesterday's time for tomorrow
            if(Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)
        }
        scheduleRefresh(c)
    }
    private fun scheduleRetry(c:Context){
        val at=System.currentTimeMillis()+60*60*1000L
        val am=c.getSystemService(AlarmManager::class.java)
        val p=pending(c,7801,Intent(c,PrayerRefreshReceiver::class.java))
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)
    }
    fun scheduleRefresh(c: Context) {
        val calendar=Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,5);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }
        val am=c.getSystemService(AlarmManager::class.java)
        val p=pending(c,7800,Intent(c,PrayerRefreshReceiver::class.java))
        am.cancel(p)
        if(Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,p)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,p)
    }
}
