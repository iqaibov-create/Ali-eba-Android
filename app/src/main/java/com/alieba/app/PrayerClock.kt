package com.alieba.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object PrayerClock {
    private const val PREF = "alieba_prayer_clock"
    val displayNames = listOf("Fəcr","Günəş\ndoğuşu","Zöhr","Günəş\nbatımı","Məğrib","Gecə\nyarısı")
    val keys = listOf("fajr","sunrise","dhuhr","sunset","maghrib","midnight")
    private val fetchKeys=(keys+listOf("asr","isha")).distinct()
    private val alarmNames=listOf("Fəcr","Zöhr","Əsr","Məğrib","İşa")
    private val alarmKeys=listOf("fajr","dhuhr","asr","maghrib","isha")

    fun displayTimes(c:Context):Map<String,String>{
        val p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE)
        return fetchKeys.mapNotNull { k->p.getString(k,null)?.let{k to it} }.toMap()
    }
    fun activeKey(c:Context):String?{
        val t=displayTimes(c); if(t.isEmpty())return null
        val now=Calendar.getInstance().run{get(Calendar.HOUR_OF_DAY)*60+get(Calendar.MINUTE)}
        return keys.mapNotNull{k->
            val m=Regex("""^([0-2]\d):([0-5]\d)$""").matchEntire(t[k]?:"")?:return@mapNotNull null
            val h=m.groupValues[1].toInt();if(h>23)return@mapNotNull null
            val minute=h*60+m.groupValues[2].toInt()
            if(minute<=now)k to minute else null
        }.maxByOrNull{it.second}?.first
    }
    private fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(java.util.Date())
    private fun todayParts():Triple<Int,Int,Int>{val c=Calendar.getInstance();return Triple(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH))}
    private fun dateMatches(raw:String?):Boolean{
        val v=raw?.trim().orEmpty();if(v.isEmpty()||v.startsWith(today()))return true
        val n=Regex("""\d+""").findAll(v).map{it.value.toIntOrNull()}.filterNotNull().toList();if(n.size<3)return false
        val(y,m,d)=todayParts();val a=n[0];val b=n[1];val cc=n[2]
        return when{a>=1900->a==y&&b==m&&cc==d;cc>=1900->cc==y&&b==m&&a==d;else->false}
    }
    private fun chooseDay(days:JSONArray?):JSONObject?{
        if(days==null||days.length()==0)return null;var undated:JSONObject?=null
        for(i in 0 until days.length()){val o=days.optJSONObject(i)?:continue;val d=o.optString("date","");if(d.isBlank()&&undated==null)undated=o;if(dateMatches(d))return o}
        return if(days.length()==1)days.optJSONObject(0) else undated
    }
    private fun findTodayNode(j:JSONObject):JSONObject?{
        chooseDay(j.optJSONArray("days"))?.let{return it}
        j.optJSONArray("results")?.let{a->for(i in 0 until a.length()){val r=a.optJSONObject(i)?:continue;chooseDay(r.optJSONArray("days"))?.let{return it};if((r.has("local")||r.has("times"))&&dateMatches(r.optString("date","")))return r}}
        j.optJSONObject("data")?.let{d->chooseDay(d.optJSONArray("days"))?.let{return it};if((d.has("local")||d.has("times"))&&dateMatches(d.optString("date","")))return d}
        if((j.has("local")||j.has("times")||j.has("fajr"))&&dateMatches(j.optString("date","")))return j
        return null
    }
    private fun dateKey(c:Context):String{val s=c.getSharedPreferences("alieba_setup",Context.MODE_PRIVATE);return today()+"|"+TimeZone.getDefault().id+"|"+s.getString("latitude","")+"|"+s.getString("longitude","")}
    fun times(c:Context):Map<String,String>{val p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);if(p.getString("date_key","")!=dateKey(c))return emptyMap();return displayTimes(c)}
    fun fetchAndSchedule(c:Context,done:((Boolean)->Unit)?=null){
        Thread{var ok=false
            try{
                val s=c.getSharedPreferences("alieba_setup",Context.MODE_PRIVATE);val lat=s.getString("latitude",null)?.toDoubleOrNull();val lon=s.getString("longitude",null)?.toDoubleOrNull()
                if(lat!=null&&lon!=null&&lat in -90.0..90.0&&lon in -180.0..180.0){
                    val tz=java.net.URLEncoder.encode(TimeZone.getDefault().id,"UTF-8");val con=(URL("https://alieba.ge/api/prayer-times.php?lat=$lat&lng=$lon&tz=$tz").openConnection() as HttpURLConnection).apply{connectTimeout=6000;readTimeout=6000;setRequestProperty("Accept","application/json");setRequestProperty("Cache-Control","no-cache")}
                    try{if(con.responseCode==200){val j=JSONObject(con.inputStream.bufferedReader().use{it.readText()});val node=findTodayNode(j);val t=node?.optJSONObject("local")?:node?.optJSONObject("times")?:node
                        if(t!=null&&dateMatches(node?.optString("date",""))){val parsed=fetchKeys.mapNotNull{k->val raw=if(k=="midnight")t.optString("midnight",t.optString("night_midpoint","")).trim() else t.optString(k,"").trim();val hit=Regex("""(?:^|T|\s)([0-2]?\d):([0-5]\d)""").find(raw);if(hit!=null){val h=hit.groupValues[1].toInt();val m=hit.groupValues[2].toInt();if(h in 0..23)k to String.format(Locale.US,"%02d:%02d",h,m) else null}else null}.toMap()
                            if(listOf("fajr","sunrise","dhuhr","maghrib").all{parsed.containsKey(it)}){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().apply{fetchKeys.forEach{remove(it)};putString("date_key",dateKey(c));parsed.forEach{(k,v)->putString(k,v)}}.commit();scheduleToday(c,parsed);cancelRetry(c);PrayerStatusNotifier.show(c);ok=true}}}
                    }finally{con.disconnect()}
                }
            }catch(_:Exception){}
            if(!ok){val cached=times(c);if(cached.isNotEmpty())scheduleToday(c,cached);PrayerStatusNotifier.show(c);scheduleRetry(c)}
            done?.let{android.os.Handler(android.os.Looper.getMainLooper()).post{it(ok)}}
        }.start()
    }
    private fun pending(c:Context,code:Int,i:Intent)=PendingIntent.getBroadcast(c,code,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun showIntent(c:Context,code:Int)=PendingIntent.getActivity(c,code,Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun schedulePrimary(c:Context,am:AlarmManager,at:Long,op:PendingIntent,showCode:Int){try{if(Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms())am.setAlarmClock(AlarmManager.AlarmClockInfo(at,showIntent(c,showCode)),op) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,op)}catch(_:SecurityException){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,op)}}
    fun scheduleToday(c:Context,t:Map<String,String> = times(c)){
        if(t.isEmpty()){scheduleRefresh(c);scheduleRetry(c);PrayerStatusNotifier.show(c);return}
        val am=c.getSystemService(AlarmManager::class.java);val now=System.currentTimeMillis();val enabled=c.getSharedPreferences("alieba_setup",Context.MODE_PRIVATE).getBoolean("notifications",false)
        for(i in alarmKeys.indices){val name=alarmNames[i];val base=Intent(c,AzanReceiver::class.java).putExtra("prayer",name).putExtra("date",today());val primary=pending(c,7000+i,base);val fallback=pending(c,7100+i,Intent(c,AzanReceiver::class.java).putExtra("prayer",name).putExtra("date",today()).putExtra("fallback",true));am.cancel(primary);am.cancel(fallback);if(!enabled||!AzanPrefs.isPrayerEnabled(c,name))continue
            val hm=Regex("""^([01]\d|2[0-3]):([0-5]\d)$""").matchEntire(t[alarmKeys[i]]?:"")?:continue;val at=Calendar.getInstance().apply{set(Calendar.HOUR_OF_DAY,hm.groupValues[1].toInt());set(Calendar.MINUTE,hm.groupValues[2].toInt());set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis;if(at<=now)continue;schedulePrimary(c,am,at,primary,7200+i);am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at+120000L,fallback)}
        scheduleRefresh(c);PrayerStatusNotifier.show(c)
    }
    fun cancelPrayerBackups(c:Context,prayer:String){val i=alarmNames.indexOf(prayer);if(i<0)return;val am=c.getSystemService(AlarmManager::class.java);am.cancel(pending(c,7000+i,Intent(c,AzanReceiver::class.java).putExtra("prayer",prayer).putExtra("date",today())));am.cancel(pending(c,7100+i,Intent(c,AzanReceiver::class.java).putExtra("prayer",prayer).putExtra("date",today()).putExtra("fallback",true)))}
    private fun scheduleRetry(c:Context){val am=c.getSystemService(AlarmManager::class.java);val p=pending(c,7801,Intent(c,PrayerRefreshReceiver::class.java));am.cancel(p);am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+300000L,p)}
    private fun cancelRetry(c:Context){c.getSystemService(AlarmManager::class.java).cancel(pending(c,7801,Intent(c,PrayerRefreshReceiver::class.java)))}
    fun scheduleRefresh(c:Context){val cal=Calendar.getInstance().apply{add(Calendar.DAY_OF_YEAR,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,3);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)};val am=c.getSystemService(AlarmManager::class.java);val p=pending(c,7800,Intent(c,PrayerRefreshReceiver::class.java));am.cancel(p);try{if(Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.timeInMillis,p) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.timeInMillis,p)}catch(_:SecurityException){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.timeInMillis,p)}}
}
