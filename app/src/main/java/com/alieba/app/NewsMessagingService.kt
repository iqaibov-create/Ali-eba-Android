package com.alieba.app

import android.app.*
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NewsMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FirebaseAuth.getInstance().currentUser != null) {
            // NativeHome/Profile will register it again with a verified Firebase ID token.
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: "news"

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val channel = when (type) {
            "qa_answer" -> "alieba_qa_v21"
            "content" -> "alieba_content_v21"
            else -> "alieba_news_v21"
        }

        val channelName = when (type) {
            "qa_answer" -> "Alieba sual-cavab"
            "content" -> "Alieba yeni məzmun"
            else -> "Alieba yenilikləri"
        }

        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channel,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val title =
            (message.data["title"] ?: message.notification?.title ?: "Alieba")
                .take(100)

        val body =
            (message.data["body"] ?: message.notification?.body ?: "Yeni məlumat var")
                .take(260)

        val itemId = message.data["id"]?.toIntOrNull() ?: 0
        val section = message.data["section"] ?: ""

        val open = Intent(this, NativeHomeActivity::class.java)
            .putExtra("open_type", type)
            .putExtra("item_id", itemId)
            .putExtra("section", section)
            .addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            )

        val pending = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        manager.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            NotificationCompat.Builder(this, channel)
                .setSmallIcon(R.drawable.ic_notification_mosque)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
        )
    }
}
