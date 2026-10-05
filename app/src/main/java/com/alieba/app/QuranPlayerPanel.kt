package com.alieba.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import java.net.URL

/** V21 clean native Quran player. */
class QuranPlayerPanel(context: Context) : LinearLayout(context) {

    private val green = 0xff176454.toInt()
    private val muted = 0xff6f817a.toInt()

    private var ayahs = JSONArray()
    private var surah = 1
    private var index = 0
    private var media: MediaPlayer? = null
    private var prepared = false

    private val main = Handler(Looper.getMainLooper())

    private val title = TextView(context)
    private val play = TextView(context)
    private val seeker = SeekBar(context)
    private val timer = TextView(context)

    private var onVerse: ((Int, Int) -> Unit)? = null

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()

    private fun circle(color: Int) =
        GradientDrawable().apply {
            setColor(color)
            shape = GradientDrawable.OVAL
        }

    init {
        orientation = VERTICAL
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadii = floatArrayOf(
                dp(18).toFloat(), dp(18).toFloat(),
                dp(18).toFloat(), dp(18).toFloat(),
                0f, 0f, 0f, 0f
            )
            setStroke(dp(1), 0xffdce8e2.toInt())
        }
        elevation = dp(5).toFloat()
        setPadding(dp(12), dp(7), dp(12), dp(4))

        val top = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        title.apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            text = "Quran qiraəti"
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        top.addView(title, LayoutParams(0, dp(25), 1f))

        timer.apply {
            text = "00:00 / 00:00"
            textSize = 10f
            setTextColor(muted)
        }
        top.addView(timer)

        addView(top)

        val controls = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val prev = TextView(context).apply {
            text = "‹"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(green)
            contentDescription = "Əvvəlki ayə"
            setOnClickListener {
                if (ayahs.length() > 0) playAt((index - 1).coerceAtLeast(0))
            }
        }
        controls.addView(prev, LayoutParams(dp(38), dp(42)))

        play.apply {
            text = "▶"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = circle(green)
            contentDescription = "Oxut və ya dayandır"
            setOnClickListener { toggle() }
        }
        controls.addView(play, LayoutParams(dp(42), dp(42)))

        val next = TextView(context).apply {
            text = "›"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(green)
            contentDescription = "Növbəti ayə"
            setOnClickListener {
                if (index + 1 < ayahs.length()) playAt(index + 1)
            }
        }
        controls.addView(next, LayoutParams(dp(38), dp(42)))

        seeker.max = 1000
        seeker.progressTintList =
            android.content.res.ColorStateList.valueOf(0xff2e9580.toInt())
        seeker.thumbTintList =
            android.content.res.ColorStateList.valueOf(0xff2e9580.toInt())

        controls.addView(seeker, LayoutParams(0, dp(38), 1f))
        addView(controls)

        seeker.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (prepared) {
                        runCatching {
                            media?.let {
                                if (it.duration > 0) {
                                    it.seekTo(
                                        (
                                            it.duration.toLong() *
                                                seeker.progress /
                                                1000
                                            ).toInt()
                                    )
                                }
                            }
                        }
                    }
                }

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    value: Int,
                    fromUser: Boolean
                ) {
                }
            }
        )

        visibility = View.GONE

        main.post(
            object : Runnable {
                override fun run() {
                    if (prepared) {
                        runCatching {
                            media?.let {
                                if (it.duration > 0) {
                                    seeker.progress =
                                        (
                                            it.currentPosition.toLong() *
                                                1000 /
                                                it.duration
                                            ).toInt()

                                    timer.text =
                                        "${fmt(it.currentPosition)} / ${fmt(it.duration)}"
                                }
                            }
                        }
                    }
                    main.postDelayed(this, 450L)
                }
            }
        )
    }

    private fun fmt(ms: Int): String =
        "%02d:%02d".format(
            java.util.Locale.US,
            ms / 60000,
            (ms / 1000) % 60
        )

    fun bind(
        surahNumber: Int,
        verses: JSONArray,
        lastAyah: Int = 1,
        verseChange: ((Int, Int) -> Unit)? = null
    ) {
        releasePlayer()
        surah = surahNumber
        ayahs = verses
        onVerse = verseChange

        index = (0 until verses.length()).firstOrNull {
            verses.optJSONObject(it)?.optInt("number") == lastAyah
        } ?: 0

        visibility = if (verses.length() > 0) View.VISIBLE else View.GONE
        showTitle()
    }

    private fun showTitle() {
        val ayah = ayahs.optJSONObject(index)?.optInt("number") ?: 0
        title.text = "${surah}. surə • $ayah. ayə"
        play.text =
            if (prepared && media?.isPlaying == true) "Ⅱ"
            else "▶"
    }

    private fun safeMedia(raw: String): String? = try {
        val url = URL(raw)
        if (
            url.protocol == "https" &&
            url.host in setOf(
                "cdn.islamic.network",
                "audio.alquran.cloud",
                "api.alquran.cloud",
                "everyayah.com",
                "alieba.ge"
            )
        ) url.toString() else null
    } catch (_: Exception) {
        null
    }

    fun playAt(position: Int) {
        val verse = ayahs.optJSONObject(position) ?: return

        val url = safeMedia(verse.optString("audio")) ?: run {
            Toast.makeText(
                context,
                "Bu ayənin audio faylı yoxdur",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        releasePlayer()

        index = position
        seeker.progress = 0
        timer.text = "00:00 / 00:00"
        title.text =
            "${surah}. surə • ${verse.optInt("number")}. ayə • hazırlanır"

        onVerse?.invoke(surah, verse.optInt("number"))

        val player = MediaPlayer()
        media = player

        try {
            player.setDataSource(url)

            player.setOnPreparedListener {
                if (media === it) {
                    prepared = true
                    it.start()
                    showTitle()
                }
            }

            player.setOnCompletionListener {
                if (media === it) {
                    if (index + 1 < ayahs.length()) {
                        playAt(index + 1)
                    } else {
                        releasePlayer()
                        showTitle()
                    }
                }
            }

            player.setOnErrorListener { current, _, _ ->
                if (media === current) {
                    releasePlayer()
                    showTitle()
                    Toast.makeText(
                        context,
                        "Qiraət yüklənmədi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }

            player.prepareAsync()

        } catch (_: Exception) {
            releasePlayer()
            showTitle()
            Toast.makeText(
                context,
                "Qiraət açıla bilmədi",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggle() {
        if (ayahs.length() == 0) return

        if (!prepared) {
            if (media == null) playAt(index)
            return
        }

        runCatching {
            media?.let {
                if (it.isPlaying) it.pause()
                else it.start()
            }
        }

        showTitle()
    }

    private fun releasePlayer() {
        val current = media
        media = null
        prepared = false

        if (current != null) {
            runCatching {
                current.reset()
                current.release()
            }
        }

        play.text = "▶"
    }

    fun stopAndHide() {
        releasePlayer()
        visibility = View.GONE
        ayahs = JSONArray()
    }

    fun close() {
        stopAndHide()
        main.removeCallbacksAndMessages(null)
    }
}
