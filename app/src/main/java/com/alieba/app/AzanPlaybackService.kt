package com.alieba.app

import android.app.*
import android.content.*
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

/** A cancellable notification remains visible while the azan is playing. */
class AzanPlaybackService: Service() {
    private var player:MediaPlayer?=null
    override fun onBind(i: Intent?)=null
    override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int {
        if(i?.action=="stop") { stopSelf();return START_NOT_STICKY }
        val name=i?.getStringExtra("prayer")?:"Namaz"
        val nm=getSystemService(NotificationManager::class.java)
        if(Build.VERSION.SDK_INT>=26) {
            nm.createNotificationChannel(NotificationChannel("alieba_azan_v12","Azan vaxtları",NotificationManager.IMPORTANCE_HIGH).apply {
                description="Alieba azan və namaz vaxtı bildirişləri"
                setSound(null,null)
            })
        }
        val close=PendingIntent.getService(this,2,Intent(this,AzanPlaybackService::class.java).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open=PendingIntent.getActivity(this,3,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val note=NotificationCompat.Builder(this,"alieba_azan_v12")
            .setSmallIcon(R.drawable.ic_notification_mosque).setContentTitle("Alieba • $name vaxtıdır")
            .setContentText("Azanı dayandırmaq üçün toxunun")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open).addAction(0,"Dayandır",close)
            .setDeleteIntent(close).setOngoing(false).build()
        startForeground(712,note)
        player?.release()
        try {
            player=MediaPlayer().apply {
                val sound=AzanPrefs.sound(this@AzanPlaybackService)
                if(sound=="custom") {
                    val path=getSharedPreferences("azan_settings",0).getString("custom_uri",null)
                    if(path==null) throw IllegalStateException("Audio seçilməyib")
                    setDataSource(this@AzanPlaybackService,Uri.parse(path))
                } else {
                    val afd=resources.openRawResourceFd(AzanPrefs.res(this@AzanPlaybackService))
                    setDataSource(afd.fileDescriptor,afd.startOffset,afd.length)
                    afd.close()
                }
                setOnCompletionListener { stopSelf() }
                setOnErrorListener { _,_,_->stopSelf();true }
                prepare();start()
            }
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() { player?.let { if(it.isPlaying) it.stop();it.release() };player=null;super.onDestroy() }
}
