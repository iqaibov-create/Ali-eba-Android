package com.alieba.app

import android.app.*
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Receives server-published news; server FCM credentials will be configured separately. */
class NewsMessagingService: FirebaseMessagingService(){
    override fun onMessageReceived(message:RemoteMessage){
        if(message.data["type"]!="news") return
        if(!getSharedPreferences("alieba_setup",0).getBoolean("news_notifications",true))return
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return
        val n=getSystemService(NotificationManager::class.java)
        if(Build.VERSION.SDK_INT>=26)n.createNotificationChannel(NotificationChannel("alieba_news_v12","Alieba yenilikləri",NotificationManager.IMPORTANCE_HIGH))
        val title=(message.data["title"]?:message.notification?.title?:"Alieba yenilikləri").take(90)
        val body=(message.data["body"]?:message.notification?.body?:"Yeni xəbər əlavə edildi").take(200)
        val open=PendingIntent.getActivity(this,301,Intent(this,MainActivity::class.java).putExtra("open_news",true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        n.notify((System.currentTimeMillis()%Int.MAX_VALUE).toInt(),NotificationCompat.Builder(this,"alieba_news_v12").setSmallIcon(R.drawable.ic_notification_mosque).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(open).build())
    }
}
