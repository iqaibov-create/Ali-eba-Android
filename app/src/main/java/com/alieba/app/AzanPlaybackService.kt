package com.alieba.app

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/** Plays one user-selected azan independently of the activity and locked screen. */
class AzanPlaybackService : Service() {
    private var player: MediaPlayer? = null
    private var audioFocus: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Do not restart the same clip if a second alarm/intent arrives while it plays.
        if (player != null) return START_NOT_STICKY
        val prayer = intent?.getStringExtra("prayer") ?: "Namaz"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, "Azan vaxtları", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alieba namaz vaxtı və azan"
            setSound(null, null) // The MediaPlayer owns the sound; no second notification chime.
        })
        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, AzanPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 3, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_mosque)
            .setContentTitle("Alieba • $prayer vaxtıdır")
            .setContentText("Azanı dayandırmaq üçün Dayandır düyməsinə basın")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(0, "Dayandır", stopIntent)
            .setDeleteIntent(stopIntent)
            .setOngoing(false)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(712, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(712, notification)
            }
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioManager = getSystemService(AudioManager::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener { change ->
                        if (change == AudioManager.AUDIOFOCUS_LOSS) stopSelf()
                    }.build()
                audioFocus = request
                audioManager?.requestAudioFocus(request)
            }
            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(attributes)
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            val sound = AzanPrefs.sound(this)
            if (sound == "custom") {
                val source = getSharedPreferences("azan_settings", MODE_PRIVATE)
                    .getString("custom_uri", null) ?: error("Azan səsi seçilməyib")
                mp.setDataSource(this, Uri.parse(source))
            } else {
                resources.openRawResourceFd(AzanPrefs.res(this)).use { afd ->
                    requireNotNull(afd) { "Azan faylı tapılmadı" }
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            }
            mp.isLooping = false
            mp.setOnCompletionListener { stopSelf() }
            mp.setOnErrorListener { _, _, _ -> stopSelf(); true }
            mp.prepare()
            mp.start()
        } catch (_: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.let { mp ->
            try { if (mp.isPlaying) mp.stop() } catch (_: Exception) { }
            try { mp.release() } catch (_: Exception) { }
        }
        player = null
        if (Build.VERSION.SDK_INT >= 26) {
            audioFocus?.let { request -> audioManager?.abandonAudioFocusRequest(request) }
        }
        audioFocus = null
        audioManager = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.alieba.app.STOP_AZAN"
        private const val CHANNEL = "alieba_azan_playback_v19_2"
    }
}
