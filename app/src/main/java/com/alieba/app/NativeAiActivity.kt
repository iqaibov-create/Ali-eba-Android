package com.alieba.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NativeAiActivity : Activity() {
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var send: TextView
    private var busy = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        NativeUi.setBars(this)
        window.statusBarColor = 0xff0b4339.toInt()
        setContentView(build())
        addBot("Salam 👋 Mən Alieba AI-yam. Yalnız İslam dini ilə bağlı suallarda kömək edirəm.")
    }

    private fun build(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xfffbfaf4.toInt())
        }

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff11695a.toInt(), 0xff0b4339.toInt())
            )
        }
        val back = text("‹", 36f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        head.addView(back, LinearLayout.LayoutParams(dp(44), dp(44)))

        val title = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), 0, 0, 0)
        }
        title.addView(text("Alieba AI", 18f, Color.WHITE, true))
        title.addView(text("Yalnız dini mövzular", 10.5f, 0xffd8e9e4.toInt()).apply { setPadding(0, dp(3), 0, 0) })
        head.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(head, LinearLayout.LayoutParams(-1, -2))

        scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(0xfffbfaf4.toInt())
        }
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(14), dp(13), dp(18))
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val quick = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(0xfffbfaf4.toInt())
        }
        val qr = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(3), dp(12), dp(8))
        }
        listOf("Namaz" to "Namaz haqqında məlumat ver", "Quran" to "Qurani-Kərim haqqında məlumat ver", "Dini sual" to "Dini sual vermək istəyirəm").forEach { item ->
            qr.addView(
                text(item.first, 11f, 0xff0b4339.toInt(), true).apply {
                    gravity = Gravity.CENTER
                    setPadding(dp(13), 0, dp(13), 0)
                    background = rounded(Color.WHITE, 18, 0xffdbe7e2.toInt())
                    setOnClickListener { ask(item.second) }
                },
                LinearLayout.LayoutParams(-2, dp(37)).apply { rightMargin = dp(7) }
            )
        }
        quick.addView(qr)
        root.addView(quick, LinearLayout.LayoutParams(-1, dp(48)))

        val compose = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(dp(10), dp(9), dp(10), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        input = EditText(this).apply {
            hint = "Dini sualınızı yazın..."
            setHintTextColor(0xff82918c.toInt())
            setTextColor(0xff18312b.toInt())
            textSize = 14f
            maxLines = 4
            minLines = 1
            setPadding(dp(13), dp(10), dp(13), dp(10))
            background = rounded(0xfffcfdfc.toInt(), 16, 0xffdbe7e2.toInt())
        }
        compose.addView(input, LinearLayout.LayoutParams(0, -2, 1f))

        send = text("➤", 22f, 0xff17362f.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = rounded(0xffd8b45a.toInt(), 15)
            setOnClickListener { ask(input.text.toString()) }
        }
        compose.addView(send, LinearLayout.LayoutParams(dp(48), dp(48)).apply { leftMargin = dp(8) })
        root.addView(compose, LinearLayout.LayoutParams(-1, -2))
        return root
    }

    private fun addBot(value: String) = addBubble(value, false)
    private fun addUser(value: String) = addBubble(value, true)

    private fun addBubble(value: String, user: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (user) Gravity.END else Gravity.START
        }
        val bubble = text(value, 13f, if (user) Color.WHITE else 0xff18312b.toInt()).apply {
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(
                if (user) 0xff0b4339.toInt() else Color.WHITE,
                16,
                if (user) null else 0xffdbe7e2.toInt()
            )
        }
        row.addView(bubble, LinearLayout.LayoutParams(-2, -2).apply { width = (resources.displayMetrics.widthPixels * 0.82f).toInt() })
        messages.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun ask(raw: String) {
        val question = raw.trim()
        if (question.isEmpty() || busy) return
        input.setText("")
        addUser(question)
        busy = true
        send.alpha = 0.45f

        val waiting = text("Cavab hazırlanır…", 11f, 0xff73867f.toInt()).apply { setPadding(dp(4), dp(2), 0, dp(7)) }
        messages.addView(waiting)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

        Thread {
            var answer = "Hazırda AI xidmətinə qoşulmaq alınmadı."
            try {
                val conn = (URL("https://alieba.ge/api/ai-chat.php").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 35000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("X-Requested-With", "XMLHttpRequest")
                }
                val payload = JSONObject()
                    .put("message", question)
                    .put("history", JSONArray())
                    .put("lang", "az")
                    .toString()
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (body.isNotBlank()) {
                    val json = JSONObject(body)
                    answer = if (json.optBoolean("ok")) {
                        json.optString("answer", answer)
                    } else {
                        json.optString("error", answer)
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
            }

            runOnUiThread {
                messages.removeView(waiting)
                addBot(answer)
                busy = false
                send.alpha = 1f
            }
        }.start()
    }
}
