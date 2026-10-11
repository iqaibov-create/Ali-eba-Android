package com.alieba.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NativeAiActivity : Activity() {

    private lateinit var answerText: TextView
    private lateinit var voiceOrb: FrameLayout
    private lateinit var keyboardButton: FrameLayout
    private lateinit var inputRow: LinearLayout
    private lateinit var input: EditText
    private lateinit var send: TextView

    private var busy = false
    private var speakNext = false
    private var mediaPlayer: MediaPlayer? = null
    private val speechSerial = AtomicInteger(0)

    private val micPermissionRequest = 779
    private var recorder: MediaRecorder? = null
    private var voiceFile: File? = null
    private var recording = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun round(
        color: Int,
        radius: Int,
        stroke: Int? = null,
        strokeWidth: Int = 1
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(strokeWidth), stroke)
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.42f)
        window.statusBarColor = 0x66000000
        window.navigationBarColor = 0xff020318.toInt()

        setContentView(build())
    }

    override fun onDestroy() {
        speechSerial.incrementAndGet()
        if (recording) runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        voiceFile?.delete()
        voiceFile = null
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    private fun build(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { finish() }
        }

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(15), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(0xff020318.toInt())
                cornerRadius = dp(38).toFloat()
                setStroke(dp(2), 0xff4854ff.toInt())
            }
            elevation = dp(18).toFloat()
            setOnClickListener { }
        }

        val title = text("✦  Alieba Süni Zəka Yardımı", 12.5f, 0xff807eff.toInt())
        title.gravity = Gravity.CENTER
        sheet.addView(
            title,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(8)
            }
        )

        voiceOrb = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff754eff.toInt(), 0xffff5a86.toInt())
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), 0xffb370ff.toInt())
            }
            elevation = dp(12).toFloat()
            setOnClickListener { maybeStartVoice() }
        }

        voiceOrb.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.ic_alieba_ai)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(18), dp(18), dp(18), dp(18))
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        sheet.addView(
            voiceOrb,
            LinearLayout.LayoutParams(dp(70), dp(70)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(10)
            }
        )

        answerText = text(
            "Necə kömək edə biləcəyimi söyləyin",
            14f,
            0xffc7c8d8.toInt(),
            true
        ).apply {
            gravity = Gravity.CENTER
            setLineSpacing(dp(3).toFloat(), 1f)
            setOnClickListener {
                val value = text?.toString().orEmpty()
                if (value.isNotBlank()) speak(value)
            }
        }

        val answerScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = true
            isFillViewport = false
            addView(answerText, FrameLayout.LayoutParams(-1, -2))
        }
        sheet.addView(
            answerScroll,
            LinearLayout.LayoutParams(-1, 0, 1f).apply {
                leftMargin = dp(8)
                rightMargin = dp(8)
                bottomMargin = dp(8)
            }
        )

        val quickScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
        }

        val quick = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        listOf(
            "Mənə Dua Kumeyli anlat" to "Dua Kumeyl haqqında qısa məlumat ver",
            "Vaqiə surəsi" to "Vaqiə surəsi haqqında məlumat ver",
            "Namaz" to "Namaz haqqında məlumat ver"
        ).forEach { item ->
            quick.addView(
                text(item.first, 11.5f, Color.WHITE).apply {
                    gravity = Gravity.CENTER
                    setPadding(dp(14), 0, dp(14), 0)
                    background = round(0xff090a20.toInt(), 20, 0xff44465e.toInt())
                    setOnClickListener {
                        speakNext = true
                        ask(item.second)
                    }
                },
                LinearLayout.LayoutParams(-2, dp(34)).apply {
                    rightMargin = dp(8)
                }
            )
        }

        quickScroll.addView(quick)
        sheet.addView(
            quickScroll,
            LinearLayout.LayoutParams(-1, dp(40)).apply {
                bottomMargin = dp(6)
            }
        )

        inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(0, 0, 0, dp(12))
        }

        input = EditText(this).apply {
            hint = "Dini sualınızı yazın..."
            setHintTextColor(0xff77798d.toInt())
            setTextColor(Color.WHITE)
            textSize = 14f
            maxLines = 3
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = round(0xff0d0e25.toInt(), 17, 0xff40425d.toInt())
        }

        inputRow.addView(input, LinearLayout.LayoutParams(0, -2, 1f))

        send = text("➤", 21f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = round(0xff5e5ff4.toInt(), 16)
            setOnClickListener {
                val q = input.text.toString().trim()
                if (q.isNotEmpty()) {
                    speakNext = false
                    ask(q)
                    input.setText("")
                }
            }
        }

        inputRow.addView(
            send,
            LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                leftMargin = dp(8)
            }
        )

        sheet.addView(inputRow, LinearLayout.LayoutParams(-1, -2))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val language = FrameLayout(this).apply {
            background = round(0xff17182d.toInt(), 16)
        }
        language.addView(
            text("◎", 23f, Color.WHITE, true).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(-1, -1)
        )
        bottom.addView(language, LinearLayout.LayoutParams(dp(44), dp(44)))

        bottom.addView(
            text("Ortadakı işarəyə basıb danış", 11.5f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        keyboardButton = FrameLayout(this).apply {
            background = round(0xff17182d.toInt(), 16)
            setOnClickListener { toggleKeyboard() }
        }
        keyboardButton.addView(
            text("⌨", 22f, Color.WHITE, true).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(-1, -1)
        )
        bottom.addView(keyboardButton, LinearLayout.LayoutParams(dp(44), dp(44)))

        sheet.addView(bottom, LinearLayout.LayoutParams(-1, dp(44)))

        val screenH = resources.displayMetrics.heightPixels
        val desired = (screenH * 0.43f).toInt()
            .coerceIn(dp(320), dp(445))
            .coerceAtMost(screenH - dp(48))

        root.addView(
            sheet,
            FrameLayout.LayoutParams(-1, desired, Gravity.BOTTOM)
        )

        return root
    }

    private fun toggleKeyboard() {
        if (inputRow.visibility == View.VISIBLE) {
            inputRow.visibility = View.GONE
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(input.windowToken, 0)
        } else {
            inputRow.visibility = View.VISIBLE
            input.requestFocus()
            input.postDelayed({
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }, 120)
        }
    }

    private fun maybeStartVoice() {
        if (
            android.os.Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                micPermissionRequest
            )
        } else {
            startVoice()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == micPermissionRequest) {
            val granted =
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (granted) {
                window.decorView.postDelayed({
                    if (!isFinishing) startVoice()
                }, 200)
            } else {
                Toast.makeText(
                    this,
                    "Səsli Alieba AI üçün mikrofon icazəsi lazımdır.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ElevenLabs Scribe: Google speech recognizer işlədilmir.
    // Mikrofon ikonuna birinci basanda yazılır, ikinci basanda göndərilir.
    private fun startVoice() {
        if (recording) {
            finishVoice()
            return
        }
        var localFile: File? = null
        try {
            localFile = File.createTempFile("alieba-mic-", ".m4a", cacheDir)
            @Suppress("DEPRECATION")
            val mr = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            recorder = mr
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(96000)
            mr.setAudioSamplingRate(44100)
            mr.setOutputFile(localFile.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            voiceFile = localFile
            recording = true
            voiceOrb.alpha = 0.60f
            answerText.text = "Dinləyirəm… Bitirmək üçün yenidən mikrofona toxun."
            voiceOrb.postDelayed({
                if (recording && !isFinishing) finishVoice()
            }, 25000L)
        } catch (_: Exception) {
            runCatching { recorder?.release() }
            recorder = null
            recording = false
            localFile?.delete()
            voiceOrb.alpha = 1f
            Toast.makeText(this, "Mikrofonu açmaq mümkün olmadı.", Toast.LENGTH_LONG).show()
        }
    }

    private fun finishVoice() {
        if (!recording) return
        recording = false
        val file = voiceFile
        voiceFile = null
        val okay = runCatching { recorder?.stop(); true }.getOrDefault(false)
        runCatching { recorder?.release() }
        recorder = null
        voiceOrb.alpha = 1f
        if (!okay || file == null || !file.exists() || file.length() < 1200) {
            file?.delete()
            Toast.makeText(this, "Səs yazısı alınmadı. Yenidən yoxla.", Toast.LENGTH_SHORT).show()
            return
        }
        answerText.text = "Səs ElevenLabs ilə tanınır…"
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val c = URL("https://alieba.ge/api/ai-stt.php")
                    .openConnection() as HttpURLConnection
                connection = c
                c.requestMethod = "POST"
                c.connectTimeout = 10000
                c.readTimeout = 40000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "audio/mp4")
                c.setRequestProperty("Accept", "application/json")
                file.inputStream().use { src ->
                    c.outputStream.use { dst -> src.copyTo(dst) }
                }
                val response = (if (c.responseCode in 200..299) c.inputStream else c.errorStream)
                    ?: throw IllegalStateException("Empty STT response")
                val json = JSONObject(response.bufferedReader(Charsets.UTF_8).use { it.readText() })
                if (!json.optBoolean("ok")) throw IllegalStateException("STT service not configured")
                val spoken = json.optString("text").trim()
                if (spoken.isEmpty()) throw IllegalArgumentException("Speech not recognized")
                runOnUiThread {
                    if (!isFinishing) {
                        speakNext = true
                        ask(spoken)
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!isFinishing) {
                        answerText.text = "Səs tanınmadı. ElevenLabs server ayarını yoxla."
                        Toast.makeText(this, "ElevenLabs səs tanıma mümkün olmadı.", Toast.LENGTH_LONG).show()
                    }
                }
            } finally {
                connection?.disconnect()
                file.delete()
            }
        }.start()
    }

    // Səsləndirmə ElevenLabs-a server üzərindən ötürülür.
    // API açarı APK-yə yazılmır: alieba.ge/api/ai-tts.php serverdə saxlayır.
    private fun speak(value: String) {
        if (value.isBlank()) return
        val token = speechSerial.incrementAndGet()
        Thread {
            var tmp: File? = null
            var conn: HttpURLConnection? = null
            try {
                val connection = URL("https://alieba.ge/api/ai-tts.php")
                    .openConnection() as HttpURLConnection
                conn = connection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "audio/mpeg")
                val data = JSONObject().put("text", value.take(1500)).toString()
                connection.outputStream.use { it.write(data.toByteArray(Charsets.UTF_8)) }
                if (connection.responseCode !in 200..299 ||
                    !connection.contentType.orEmpty().startsWith("audio/")) {
                    throw IllegalStateException("ElevenLabs audio endpoint is not ready")
                }
                val file = File.createTempFile("alieba-voice-", ".mp3", cacheDir)
                tmp = file
                connection.inputStream.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (token != speechSerial.get() || isFinishing) {
                    file.delete()
                    return@Thread
                }
                runOnUiThread {
                    if (token != speechSerial.get() || isFinishing) {
                        file.delete()
                        return@runOnUiThread
                    }
                    mediaPlayer?.release()
                    try {
                        val player = MediaPlayer()
                        player.setDataSource(file.absolutePath)
                        player.setOnPreparedListener { it.start() }
                        player.setOnCompletionListener {
                            it.release()
                            if (mediaPlayer === it) mediaPlayer = null
                            file.delete()
                        }
                        player.setOnErrorListener { failed, _, _ ->
                            failed.release()
                            if (mediaPlayer === failed) mediaPlayer = null
                            file.delete()
                            true
                        }
                        mediaPlayer = player
                        player.prepareAsync()
                    } catch (_: Exception) {
                        mediaPlayer = null
                        file.delete()
                    }
                }
            } catch (_: Exception) {
                tmp?.delete()
                if (token == speechSerial.get()) {
                    runOnUiThread {
                        if (!isFinishing) Toast.makeText(
                            this,
                            "ElevenLabs səsləndirmə xidməti serverdə hələ hazır deyil.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    private fun ask(raw: String) {
        val question = raw.trim()
        if (question.isEmpty() || busy) return

        busy = true
        answerText.text = "Cavab hazırlanır…"
        answerText.setTextColor(0xff85879b.toInt())
        voiceOrb.alpha = 0.60f
        send.alpha = 0.55f

        Thread {
            var answer = "Hazırda AI xidmətinə qoşulmaq alınmadı."

            try {
                val conn =
                    URL("https://alieba.ge/api/ai-chat.php")
                        .openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.connectTimeout = 10000
                conn.readTimeout = 35000
                conn.doOutput = true
                conn.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty(
                    "X-Requested-With",
                    "XMLHttpRequest"
                )

                val payload = JSONObject()
                    .put("message", question)
                    .put("history", JSONArray())
                    .put("lang", "az")
                    .toString()

                conn.outputStream.use {
                    it.write(payload.toByteArray(Charsets.UTF_8))
                }

                val stream =
                    if (conn.responseCode in 200..299)
                        conn.inputStream
                    else
                        conn.errorStream

                val body =
                    stream?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()

                if (body.isNotBlank()) {
                    val json = JSONObject(body)
                    answer =
                        if (json.optBoolean("ok"))
                            json.optString("answer", answer)
                        else
                            json.optString("error", answer)
                }

                conn.disconnect()
            } catch (_: Exception) {
            }

            runOnUiThread {
                answerText.text = answer
                answerText.setTextColor(0xffd9dae8.toInt())
                voiceOrb.alpha = 1f
                send.alpha = 1f

                if (speakNext) {
                    speakNext = false
                    speak(answer)
                }

                busy = false
            }
        }.start()
    }
}
