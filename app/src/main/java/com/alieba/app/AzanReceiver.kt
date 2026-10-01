package com.alieba.app
import android.content.*
import android.os.*
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
class AzanReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  val prayer=intent.getStringExtra("prayer")?:return;val date=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
  if(intent.getStringExtra("date")!=date||!AzanPrefs.isPrayerEnabled(context,prayer))return
  if(!context.getSharedPreferences("alieba_setup",Context.MODE_PRIVATE).getBoolean("notifications",false))return
  val wake=context.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Alieba:AzanReceiver").apply{setReferenceCounted(false);acquire(45000L)}
  try{PrayerClock.cancelPrayerBackups(context,prayer);PrayerStatusNotifier.show(context);val play=Intent(context,AzanPlaybackService::class.java).putExtra("prayer",prayer).putExtra("date",date);if(Build.VERSION.SDK_INT>=26)context.startForegroundService(play) else context.startService(play)}
  catch(e:Exception){Log.w("AliebaAzan","Azan servisi başlamadı",e)}
  finally{Handler(Looper.getMainLooper()).postDelayed({try{if(wake.isHeld)wake.release()}catch(_:Exception){}},12000L)}
 }
}