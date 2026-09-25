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

/** Plays azan even when Alieba is not open and the display is locked. */
class AzanPlaybackService : Service() {
    private var player: MediaPlayer? = null
    private var audioFocus: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val prayer = intent?.getStringExtra("prayer") ?: "Namaz"
        val date = intent?.getStringExtra("date") ?: ""
        val playKey = "$date|$prayer"

        val runtime = getSharedPreferences("alieba_azan_runtime", MODE_PRIVATE)
        val lastKey = runtime.getString("last_key", "")
        val lastAt = runtime.getLong("last_at", 0L)

        // Primary + two-minute fallback must never play twice.
        if (lastKey == playKey &&
            System.currentTimeMillis() - lastAt < 20 * 60 * 1000L
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (player != null) return START_NOT_STICKY

        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    "Azan vaxtları",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alieba namaz vaxtı və azan"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AzanPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = PendingIntent.getActivity(
            this,
            3,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_mosque)
            .setContentTitle("Alieba • $prayer vaxtıdır")
            .setContentText("Azanı dayandırmaq üçün Dayandır düyməsinə basın")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(0, "Dayandır", stopIntent)
            .setDeleteIntent(stopIntent)
            .setOngoing(false)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    712,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(712, notification)
            }
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Alieba:AzanPlayback"
                ).apply {
                    setReferenceCounted(false)
                    acquire(10 * 60 * 1000L)
                }

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioManager = getSystemService(AudioManager::class.java)

            if (Build.VERSION.SDK_INT >= 26) {
                val request = AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener { change ->
                        if (change == AudioManager.AUDIOFOCUS_LOSS) stopSelf()
                    }
                    .build()

                audioFocus = request
                audioManager?.requestAudioFocus(request)
            }

            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(attributes)
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            mp.setVolume(1f, 1f)

            val sound = AzanPrefs.sound(this)

            if (sound == "custom") {
                val source = getSharedPreferences("azan_settings", MODE_PRIVATE)
                    .getString("custom_uri", null)
                    ?: error("Azan səsi seçilməyib")
                mp.setDataSource(this, Uri.parse(source))
            } else {
                resources.openRawResourceFd(AzanPrefs.res(this)).use { afd ->
                    requireNotNull(afd) { "Azan faylı tapılmadı" }
                    mp.setDataSource(
                        afd.fileDescriptor,
                        afd.startOffset,
                        afd.length
                    )
                }
            }

            mp.isLooping = false
            mp.setOnCompletionListener { stopSelf() }
            mp.setOnErrorListener { _, _, _ ->
                stopSelf()
                true
            }

            mp.prepare()
            mp.start()

            // Record only after playback really starts.
            runtime.edit()
                .putString("last_key", playKey)
                .putLong("last_at", System.currentTimeMillis())
                .apply()

        } catch (_: Exception) {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
            } catch (_: Exception) {
            }
            try {
                mp.release()
            } catch (_: Exception) {
            }
        }
        player = null

        if (Build.VERSION.SDK_INT >= 26) {
            audioFocus?.let { request ->
                audioManager?.abandonAudioFocusRequest(request)
            }
        }
        audioFocus = null
        audioManager = null

        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null

        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.alieba.app.STOP_AZAN"
        private const val CHANNEL = "alieba_azan_playback_v19_7"
    }
}
