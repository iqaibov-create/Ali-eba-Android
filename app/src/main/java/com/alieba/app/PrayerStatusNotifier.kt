package com.alieba.app
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
object PrayerStatusNotifier{
 private const val CHANNEL="alieba_prayer_times_v20";private const val ID=713
 fun show(c:Context){
  val p=c.getSharedPreferences("alieba_setup",Context.MODE_PRIVATE);if(!p.getBoolean("prayer_status_notification",true)){cancel(c);return}
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
  val t=PrayerClock.displayTimes(c);if(t.isEmpty())return
  val nm=c.getSystemService(NotificationManager::class.java)
  if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(NotificationChannel(CHANNEL,"Namaz vaxtları",NotificationManager.IMPORTANCE_LOW).apply{setSound(null,null);enableVibration(false)})
  fun v(k:String)=t[k]?:"--:--"
  val a="Fəcr ${v("fajr")} • Zöhr ${v("dhuhr")} • Məğrib ${v("maghrib")}";val b="Günəş ${v("sunrise")} • Batım ${v("sunset")} • Gecə ${v("midnight")}"
  val pi=PendingIntent.getActivity(c,713,Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  nm.notify(ID,NotificationCompat.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification_mosque).setContentTitle("Alieba • Namaz vaxtları").setContentText(a).setStyle(NotificationCompat.BigTextStyle().bigText("$a\n$b")).setContentIntent(pi).setOnlyAlertOnce(true).setOngoing(true).setSilent(true).setPriority(NotificationCompat.PRIORITY_LOW).build())
 }
 fun cancel(c:Context){c.getSystemService(NotificationManager::class.java).cancel(ID)}
}