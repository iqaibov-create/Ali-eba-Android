package com.alieba.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AzanReceiver:BroadcastReceiver() {
    override fun onReceive(c:Context,i:Intent) {
        val name=i.getStringExtra("prayer") ?: return
        val date=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
        if(i.getStringExtra("date")!=date) return
        if(PrayerClock.times(c).isEmpty() || !AzanPrefs.isPrayerEnabled(c,name)) return
        if(!c.getSharedPreferences("alieba_setup",0).getBoolean("notifications",false)) return
        if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val service=Intent(c,AzanPlaybackService::class.java).putExtra("prayer",name)
        try { if(Build.VERSION.SDK_INT>=26) c.startForegroundService(service) else c.startService(service) } catch (_: Exception) { }
    }
}
