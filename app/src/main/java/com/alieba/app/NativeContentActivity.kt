package com.alieba.app

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import coil.load
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * V21 Native:
 * - No WebView
 * - Quran is a real Android screen
 * - Arabic + Azerbaijani translation
 * - Verse-by-verse audio
 * - Persistent local Quran cache
 * - Continue reading + local bookmarks
 * - Clean white/light-green Alieba design
 */
class NativeContentActivity : Activity() {

    private val ink = 0xff173f36.toInt()
    private val green = 0xff176454.toInt()
    private val green2 = 0xff2d8068.toInt()
    private val softGreen = 0xffedf7f2.toInt()
    private val bg = 0xfff8fbf9.toInt()
    private val line = 0xffdce8e2.toInt()
    private val muted = 0xff71847d.toInt()
    private val gold = 0xffb79242.toInt()

    private lateinit var body: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var quranPlayer: QuranPlayerPanel

    private var section = "mafatih"
    private var categoryId = 0
    private var categories = JSONArray()
    private var entries = JSONArray()
    private var query = ""
    private var pendingEntryId = 0

    private var audio: MediaPlayer? = null

    private val favorites by lazy {
        getSharedPreferences("alieba_saved_native", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = bg
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        pendingEntryId = intent.getIntExtra("entry_id", 0)

        section = intent.getStringExtra("section")
            ?.takeIf {
                it in setOf(
                    "quran", "mafatih", "ahkam", "hadis",
                    "mersiye", "kitabxana", "news", "saved"
                )
            } ?: "mafatih"

        baseScreen(titleFor(section))

        when (section) {
            "quran" -> surahList()
            "news" -> loadNews()
            "saved" -> showSaved()
            else -> loadContent()
        }
    }

    override fun onDestroy() {
        stopAudio()
        if (::quranPlayer.isInitialized) quranPlayer.close()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun shape(
        color: Int,
        radius: Int = 16,
        strokeColor: Int? = null,
        strokeWidth: Int = 1
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (strokeColor != null) setStroke(dp(strokeWidth), strokeColor)
    }

    private fun text(
        value: String,
        size: Float = 16f,
        color: Int = ink,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun titleFor(value: String) = when (value) {
        "quran" -> "Qurani-Kərim"
        "mafatih" -> "Məfatih"
        "ahkam" -> "Əhkam"
        "hadis" -> "Hədislər"
        "mersiye" -> "Mərsiyələr"
        "kitabxana" -> "Kitabxana"
        "news" -> "Yeniliklər"
        "saved" -> "Yadda saxlananlar"
        else -> "Alieba"
    }

    private fun baseScreen(title: String) {
        if (::quranPlayer.isInitialized) quranPlayer.close()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(7), dp(14), dp(7))
            setBackgroundColor(Color.WHITE)
        }

        bar.addView(
            text("‹", 34f, ink).apply {
                gravity = Gravity.CENTER
                contentDescription = "Geri"
                setOnClickListener { finish() }
            },
            LinearLayout.LayoutParams(dp(44), dp(48))
        )

        val barTitle = text(title, 21f, ink, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), 0, 0, 0)
        }
        bar.addView(barTitle, LinearLayout.LayoutParams(0, dp(50), 1f))

        val brand = TextView(this).apply {
            text = "☪"
            textSize = 23f
            setTextColor(green)
            gravity = Gravity.CENTER
        }
        bar.addView(brand, LinearLayout.LayoutParams(dp(42), dp(42)))

        root.addView(bar, LinearLayout.LayoutParams(-1, -2))

        View(this).apply { setBackgroundColor(line) }.also {
            root.addView(it, LinearLayout.LayoutParams(-1, dp(1)))
        }

        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setBackgroundColor(bg)
        }

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(15), dp(15), dp(28))
        }

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        quranPlayer = QuranPlayerPanel(this)
        root.addView(quranPlayer, LinearLayout.LayoutParams(-1, dp(89)))

        root.addView(
            NativeNav.make(this, section),
            LinearLayout.LayoutParams(-1, dp(87))
        )

        setContentView(root)
    }

    private fun heading(value: String) {
        body.addView(
            text(value, 26f, ink, true),
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(9)
            }
        )
    }

    private fun message(value: String) {
        body.addView(
            text(value, 14f, muted).apply {
                setPadding(dp(8), dp(14), dp(8), dp(14))
            },
            LinearLayout.LayoutParams(-1, -2)
        )
    }

    private fun button(label: String, action: () -> Unit): TextView {
        val view = text(label, 14f, ink, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(12), dp(15), dp(12))
            background = shape(Color.WHITE, 13, line)
            setOnClickListener { action() }
        }

        body.addView(
            view,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(9)
            }
        )
        return view
    }

    // ---------------------------------------------------------------------
    // NETWORK / CACHE
    // ---------------------------------------------------------------------

    private fun fetch(path: String, finished: (JSONObject?) -> Unit) {
        Thread {
            val value = try {
                val url = URL("https://alieba.ge$path")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 11000
                    readTimeout = 16000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Alieba-Android/21")
                }

                try {
                    if (connection.responseCode in 200..299) {
                        JSONObject(
                            connection.inputStream.bufferedReader().use { it.readText() }
                        )
                    } else null
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                null
            }

            runOnUiThread {
                if (!isFinishing && !isDestroyed) finished(value)
            }
        }.start()
    }

    private fun quranCacheFile(number: Int): File {
        val folder = File(filesDir, "quran_cache")
        if (!folder.exists()) folder.mkdirs()
        return File(folder, "surah_$number.json")
    }

    private fun cachedSurah(number: Int): JSONObject? = try {
        val file = quranCacheFile(number)
        if (!file.isFile) null else JSONObject(file.readText())
    } catch (_: Exception) {
        null
    }

    private fun saveSurahCache(number: Int, json: JSONObject) {
        Thread {
            try {
                quranCacheFile(number).writeText(json.toString())
            } catch (_: Exception) {
            }
        }.start()
    }

    /**
     * Cached Quran UX:
     * 1) cached surah appears immediately
     * 2) fresh server copy replaces it silently
     * 3) if offline, cached copy still works
     */
    private fun loadSurahJson(
        number: Int,
        cached: (JSONObject) -> Unit,
        fresh: (JSONObject?) -> Unit
    ) {
        cachedSurah(number)?.let(cached)

        fetch("/api/quran.php?surah=$number") { json ->
            if (json?.optBoolean("ok") == true) {
                saveSurahCache(number, json)
                fresh(json)
            } else {
                fresh(null)
            }
        }
    }

    // ---------------------------------------------------------------------
    // NORMAL CONTENT
    // ---------------------------------------------------------------------

    private fun loadContent() {
        body.removeAllViews()
        heading(titleFor(section))
        message("Kateqoriyalar yüklənir…")

        fetch("/api/content-v19.php?section=$section") { json ->
            body.removeAllViews()
            heading(titleFor(section))

            if (json?.optBoolean("ok") != true) {
                message("Məzmun hazırda yüklənmədi.")
                button("Yenidən yoxla") { loadContent() }
                return@fetch
            }

            categories = json.optJSONArray("categories") ?: JSONArray()
            entries = json.optJSONArray("items") ?: JSONArray()

            if (pendingEntryId > 0) {
                var opened = false
                for (i in 0 until entries.length()) {
                    val item = entries.optJSONObject(i) ?: continue
                    if (item.optInt("id") == pendingEntryId) {
                        opened = true
                        pendingEntryId = 0
                        showEntry(item)
                        break
                    }
                }
                if (!opened) drawContent()
            } else {
                drawContent()
            }
        }
    }

    private fun drawContent() {
        body.removeAllViews()
        heading(titleFor(section))

        val finder = EditText(this).apply {
            hint = "Məzmun axtar…"
            setSingleLine(true)
            textSize = 15f
            background = shape(Color.WHITE, 14, line)
            setPadding(dp(15), 0, dp(15), 0)
            setText(query)
        }
        body.addView(
            finder,
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                bottomMargin = dp(11)
            }
        )

        val chips = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val chipLine = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        val listBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun refresh() {
            listBox.removeAllViews()
            var count = 0

            for (i in 0 until entries.length()) {
                val entry = entries.optJSONObject(i) ?: continue

                if (categoryId != 0 && entry.optInt("category_id") != categoryId) continue

                val title = entry.optString("title")
                val summary = entry.optString("summary")

                if (
                    query.isNotBlank() &&
                    !("$title $summary").contains(query, ignoreCase = true)
                ) continue

                count++

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = shape(Color.WHITE, 17, line)
                    elevation = dp(2).toFloat()
                    clipToOutline = true
                    setOnClickListener { showEntry(entry) }
                }

                val imageUrl = entry.optString("image_file")
                if (imageUrl.isNotBlank()) {
                    row.addView(
                        ImageView(this).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            load(MobileApi.absolute(imageUrl)) {
                                crossfade(true)
                                memoryCacheKey("content_${section}_${entry.optInt("id")}")
                                diskCacheKey("content_${section}_${entry.optInt("id")}")
                            }
                        },
                        LinearLayout.LayoutParams(-1, dp(205))
                    )
                }

                val copy = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(14), dp(16), dp(15))
                }
                copy.addView(text(title, 21f, ink, true))
                if (summary.isNotBlank()) {
                    copy.addView(
                        text(summary, 12f, muted).apply {
                            setMaxLines(2)
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            setPadding(0, dp(6), 0, 0)
                        }
                    )
                }
                row.addView(copy)

                listBox.addView(
                    row,
                    LinearLayout.LayoutParams(-1, -2).apply {
                        bottomMargin = dp(13)
                    }
                )
            }

            if (count == 0) {
                listBox.addView(
                    text("Bu kateqoriyada hələ məzmun yoxdur.", 14f, muted).apply {
                        setPadding(dp(7), dp(20), dp(7), 0)
                    }
                )
            }
        }

        fun chip(name: String, id: Int) {
            val selected = id == categoryId
            val chip = text(
                name,
                13f,
                if (selected) Color.WHITE else ink,
                selected
            ).apply {
                gravity = Gravity.CENTER
                background = shape(
                    if (selected) green else Color.WHITE,
                    22,
                    if (selected) null else line
                )
                setPadding(dp(15), dp(9), dp(15), dp(9))
                setOnClickListener {
                    categoryId = id
                    drawContent()
                }
            }

            chipLine.addView(
                chip,
                LinearLayout.LayoutParams(-2, dp(39)).apply {
                    rightMargin = dp(7)
                }
            )
        }

        chip("Hamısı", 0)
        for (i in 0 until categories.length()) {
            val category = categories.optJSONObject(i) ?: continue
            chip(category.optString("name"), category.optInt("id"))
        }

        chips.addView(chipLine)
        body.addView(
            chips,
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                bottomMargin = dp(11)
            }
        )
        body.addView(listBox)

        finder.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                query = s?.toString() ?: ""
                refresh()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        refresh()
    }

    private fun showEntry(entry: JSONObject) {
        stopAudio()
        body.removeAllViews()

        val imageUrl = entry.optString("image_file")
        if (imageUrl.isNotBlank()) {
            body.addView(
                ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = shape(0xffeef3f1.toInt(), 16)
                    clipToOutline = true
                    load(MobileApi.absolute(imageUrl)) {
                        crossfade(true)
                        memoryCacheKey("content_reader_${section}_${entry.optInt("id")}")
                        diskCacheKey("content_reader_${section}_${entry.optInt("id")}")
                    }
                },
                LinearLayout.LayoutParams(-1, dp(245)).apply {
                    bottomMargin = dp(15)
                }
            )
        }

        heading(entry.optString("title"))

        val summary = entry.optString("summary")
        if (summary.isNotBlank()) message(summary)

        val arabic = entry.optString("arabic_text")
        if (arabic.isNotBlank()) {
            val arabicBox = text(arabic, 25f, ink).apply {
                gravity = Gravity.RIGHT
                textDirection = View.TEXT_DIRECTION_RTL
                typeface = Typeface.create("serif", Typeface.NORMAL)
                setTextIsSelectable(true)
                setPadding(dp(16), dp(20), dp(16), dp(20))
                setLineSpacing(dp(7).toFloat(), 1.22f)
                background = shape(Color.WHITE, 15, line)
            }
            body.addView(
                arabicBox,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(11)
                }
            )
        }

        val az = entry.optString("az_text")
        if (az.isNotBlank()) {
            val azBox = text(az, 16f, ink).apply {
                setTextIsSelectable(true)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                setLineSpacing(dp(5).toFloat(), 1.08f)
                background = shape(softGreen, 15, 0xffd2e6dc.toInt())
            }
            body.addView(
                azBox,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(11)
                }
            )
        }

        val audioUrl = entry.optString("audio_url")
        if (audioUrl.isNotBlank()) {
            button("▶  Dinlə / dayandır") { playAudio(audioUrl) }
        }

        val pdf = entry.optString("pdf_file")
        if (pdf.isNotBlank()) {
            button("↓  PDF-i telefona yüklə") {
                downloadPdf(pdf, entry.optString("title"))
            }
        }

        val entryId = entry.optInt("id").toString()
        button(
            if (isSaved(section, entryId)) "♥  Yadda saxlanıb"
            else "♡  Yadda saxla"
        ) {
            toggleSaved(section, entryId, entry.optString("title"))
            showEntry(entry)
        }

        button("‹  Siyahıya qayıt") {
            stopAudio()
            drawContent()
        }
    }

    // ---------------------------------------------------------------------
    // NEWS
    // ---------------------------------------------------------------------

    private fun loadNews() {
        body.removeAllViews()
        heading("Alieba yenilikləri")
        message("Yeniliklər yüklənir…")

        fetch("/api/news.php") { json ->
            body.removeAllViews()
            heading("Alieba yenilikləri")

            val list = json?.optJSONArray("items")

            if (json?.optBoolean("ok") != true) {
                message("Yeniliklər hazırda yüklənmədi.")
                button("Yenilə") { loadNews() }
                return@fetch
            }

            if (list == null || list.length() == 0) {
                message("Hələ yenilik yoxdur.")
                return@fetch
            }

            for (i in 0 until list.length()) {
                val news = list.optJSONObject(i) ?: continue

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(15), dp(16), dp(15))
                    background = shape(Color.WHITE, 15, line)
                }

                row.addView(text(news.optString("title"), 18f, ink, true))
                row.addView(
                    text(news.optString("created_at").take(16), 11f, gold).apply {
                        setPadding(0, dp(6), 0, dp(6))
                    }
                )
                row.addView(
                    text(news.optString("body"), 15f, ink).apply {
                        setLineSpacing(dp(5).toFloat(), 1f)
                    }
                )

                body.addView(
                    row,
                    LinearLayout.LayoutParams(-1, -2).apply {
                        bottomMargin = dp(11)
                    }
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // QURAN
    // ---------------------------------------------------------------------

    private fun quranIntroCard(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = shape(softGreen, 20, 0xffd7e9e0.toInt())

            addView(
                text("القرآن الكريم", 28f, green, true).apply {
                    gravity = Gravity.CENTER
                    typeface = Typeface.create("serif", Typeface.BOLD)
                }
            )

            addView(
                text("Qurani-Kərim", 25f, ink, true).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(7), 0, 0)
                }
            )

            addView(
                text(
                    "114 surə • Ərəb mətni • Azərbaycan tərcüməsi • Qiraət",
                    12f,
                    muted
                ).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(5), 0, 0)
                }
            )
        }
    }

    private fun surahList() {
        quranPlayer.stopAndHide()
        body.removeAllViews()

        body.addView(
            quranIntroCard(),
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(13)
            }
        )

        val lastSurah = favorites.getInt("last_surah", 0)
        val lastAyah = if (lastSurah > 0) {
            favorites.getInt("last_ayah_$lastSurah", 1)
        } else 0

        if (lastSurah in 1..114) {
            val continueCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = shape(Color.WHITE, 15, line)
                setOnClickListener { showSurah(lastSurah, lastAyah) }

                addView(text("QALDIĞIN YER", 9f, gold, true))
                addView(
                    text(
                        "${lastSurah}. ${surahs[lastSurah - 1]} • $lastSurah:$lastAyah",
                        17f,
                        ink,
                        true
                    ).apply { setPadding(0, dp(6), 0, dp(4)) }
                )
                addView(text("Oxumağa davam et  →", 12f, green, true))
            }

            body.addView(
                continueCard,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(12)
                }
            )
        }

        val search = EditText(this).apply {
            hint = "Surə axtar…"
            setSingleLine(true)
            textSize = 15f
            background = shape(Color.WHITE, 14, line)
            setPadding(dp(15), 0, dp(15), 0)
        }

        body.addView(
            search,
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                bottomMargin = dp(12)
            }
        )

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        body.addView(list)

        fun draw(filter: String) {
            list.removeAllViews()

            surahs.forEachIndexed { index, name ->
                val number = index + 1
                if (
                    filter.isNotBlank() &&
                    !name.contains(filter, true) &&
                    !number.toString().contains(filter)
                ) return@forEachIndexed

                val row = LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(11), dp(10), dp(11), dp(10))
                    background = shape(Color.WHITE, 15, line)
                    setOnClickListener { showSurah(number) }
                }

                val numberBox = TextView(this).apply {
                    text = number.toString()
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                    background = shape(green, 12)
                }
                row.addView(
                    numberBox,
                    LinearLayout.LayoutParams(dp(43), dp(43))
                )

                val labels = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), 0, 0, 0)
                }
                labels.addView(text(name, 16f, ink, true))
                labels.addView(
                    text(
                        surahMeta[index],
                        11f,
                        muted
                    ).apply { setPadding(0, dp(2), 0, 0) }
                )
                row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))

                row.addView(
                    text("›", 27f, green).apply {
                        gravity = Gravity.CENTER
                    },
                    LinearLayout.LayoutParams(dp(30), dp(43))
                )

                list.addView(
                    row,
                    LinearLayout.LayoutParams(-1, -2).apply {
                        bottomMargin = dp(8)
                    }
                )
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                draw(s?.toString() ?: "")
            }
        })

        draw("")
    }

    private fun showSurah(number: Int, focusAyah: Int = 0) {
        section = "quran"
        stopAudio()
        quranPlayer.stopAndHide()
        body.removeAllViews()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(16), dp(17), dp(16))
            background = shape(green, 19)
        }

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val back = text("‹  Surələr", 13f, Color.WHITE, true).apply {
            setOnClickListener { surahList() }
        }
        top.addView(back, LinearLayout.LayoutParams(0, dp(38), 1f))

        if (number > 1) {
            top.addView(
                text("‹", 26f, Color.WHITE).apply {
                    gravity = Gravity.CENTER
                    setOnClickListener { showSurah(number - 1) }
                },
                LinearLayout.LayoutParams(dp(38), dp(38))
            )
        }

        if (number < 114) {
            top.addView(
                text("›", 26f, Color.WHITE).apply {
                    gravity = Gravity.CENTER
                    setOnClickListener { showSurah(number + 1) }
                },
                LinearLayout.LayoutParams(dp(38), dp(38))
            )
        }

        header.addView(top)

        header.addView(
            text("$number. ${surahs[number - 1]}", 26f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
            }
        )

        header.addView(
            text(surahMeta[number - 1], 12f, 0xffd8ebe4.toInt()).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            }
        )

        body.addView(
            header,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            }
        )

        val loading = text("Ayələr yüklənir…", 14f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(18))
        }
        body.addView(loading)

        var renderedFromCache = false

        loadSurahJson(
            number,
            cached = { cached ->
                renderedFromCache = true
                renderSurah(number, cached, focusAyah, fromCache = true)
            },
            fresh = { fresh ->
                if (fresh != null) {
                    renderSurah(number, fresh, focusAyah, fromCache = false)
                } else if (!renderedFromCache) {
                    body.removeAllViews()
                    body.addView(header)
                    message("Quran ayələri hazırda yüklənmədi və telefonda keş yoxdur.")
                    button("Yenidən yoxla") { showSurah(number, focusAyah) }
                    button("‹  Surələr") { surahList() }
                }
            }
        )
    }

    private fun renderSurah(
        number: Int,
        json: JSONObject,
        focusAyah: Int,
        fromCache: Boolean
    ) {
        val verses = json.optJSONArray("ayahs") ?: return

        body.removeAllViews()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(16), dp(17), dp(16))
            background = shape(green, 19)

            addView(
                LinearLayout(this@NativeContentActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL

                    addView(
                        text("‹  Surələr", 13f, Color.WHITE, true).apply {
                            setOnClickListener { surahList() }
                        },
                        LinearLayout.LayoutParams(0, dp(38), 1f)
                    )

                    if (number > 1) {
                        addView(
                            text("‹", 26f, Color.WHITE).apply {
                                gravity = Gravity.CENTER
                                setOnClickListener { showSurah(number - 1) }
                            },
                            LinearLayout.LayoutParams(dp(38), dp(38))
                        )
                    }

                    if (number < 114) {
                        addView(
                            text("›", 26f, Color.WHITE).apply {
                                gravity = Gravity.CENTER
                                setOnClickListener { showSurah(number + 1) }
                            },
                            LinearLayout.LayoutParams(dp(38), dp(38))
                        )
                    }
                }
            )

            addView(
                text("$number. ${surahs[number - 1]}", 26f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(6), 0, 0)
                }
            )

            addView(
                text(
                    "${surahMeta[number - 1]} • ${verses.length()} ayə",
                    12f,
                    0xffd8ebe4.toInt()
                ).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(4), 0, 0)
                }
            )
        }

        body.addView(
            header,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(9)
            }
        )

        if (fromCache) {
            body.addView(
                text("Keşdən açıldı • internet varsa arxa planda yenilənir", 10f, muted).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(2), 0, dp(8))
                }
            )
        }

        val lastAyah = favorites.getInt("last_ayah_$number", 1)

        favorites.edit()
            .putInt("last_surah", number)
            .apply()

        quranPlayer.bind(
            number,
            verses,
            lastAyah.coerceAtLeast(1)
        ) { surah, ayah ->
            favorites.edit()
                .putInt("last_surah", surah)
                .putInt("last_ayah_$surah", ayah)
                .apply()
        }

        var focusView: View? = null

        for (i in 0 until verses.length()) {
            val ayah = verses.optJSONObject(i) ?: continue
            val ayahNo = ayah.optInt("number")

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = shape(Color.WHITE, 17, line)
            }

            val meta = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
            }

            val ayahBadge = TextView(this).apply {
                text = "$number:$ayahNo"
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(green)
                background = shape(softGreen, 10, 0xffd5e8df.toInt())
            }

            meta.addView(
                ayahBadge,
                LinearLayout.LayoutParams(dp(58), dp(31))
            )

            meta.addView(
                View(this),
                LinearLayout.LayoutParams(0, 1, 1f)
            )

            val play = text("▶  Dinlə", 12f, green, true).apply {
                setPadding(dp(8), dp(7), dp(8), dp(7))
                setOnClickListener {
                    favorites.edit()
                        .putInt("last_surah", number)
                        .putInt("last_ayah_$number", ayahNo)
                        .apply()
                    quranPlayer.playAt(i)
                }
            }
            meta.addView(play)

            val favId = "$number:$ayahNo"
            val fav = text(
                if (isSaved("quran", favId)) "♥" else "♡",
                23f,
                gold
            ).apply {
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(4), dp(6), dp(4))
                setOnClickListener {
                    toggleSaved(
                        "quran",
                        favId,
                        "${surahs[number - 1]} $number:$ayahNo"
                    )
                    text = if (isSaved("quran", favId)) "♥" else "♡"
                }
            }
            meta.addView(fav, LinearLayout.LayoutParams(dp(40), dp(40)))

            card.addView(meta)

            val arabic = text(
                ayah.optString("text"),
                28f,
                0xff153d34.toInt()
            ).apply {
                gravity = Gravity.RIGHT
                textDirection = View.TEXT_DIRECTION_RTL
                typeface = Typeface.create("serif", Typeface.NORMAL)
                setTextIsSelectable(true)
                setPadding(0, dp(15), 0, dp(15))
                setLineSpacing(dp(8).toFloat(), 1.28f)
            }
            card.addView(arabic)

            View(this).apply {
                setBackgroundColor(0xffe8efeb.toInt())
            }.also {
                card.addView(it, LinearLayout.LayoutParams(-1, dp(1)))
            }

            val translation = text(
                ayah.optString("translation"),
                15f,
                0xff49635b.toInt()
            ).apply {
                setTextIsSelectable(true)
                setPadding(0, dp(13), 0, dp(2))
                setLineSpacing(dp(5).toFloat(), 1.08f)
            }
            card.addView(translation)

            body.addView(
                card,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(10)
                }
            )

            if (focusAyah == ayahNo) focusView = card
        }

        if (focusView != null) {
            scroll.postDelayed({
                scroll.smoothScrollTo(
                    0,
                    (focusView!!.top - dp(16)).coerceAtLeast(0)
                )
            }, 180)
        }
    }

    // ---------------------------------------------------------------------
    // AUDIO / PDF / SAVED
    // ---------------------------------------------------------------------

    private fun safeMedia(raw: String): String? {
        val url = try {
            URL(URL("https://alieba.ge"), raw)
        } catch (_: Exception) {
            return null
        }

        return if (
            url.protocol == "https" &&
            url.host in setOf(
                "alieba.ge",
                "cdn.islamic.network",
                "api.alquran.cloud",
                "audio.alquran.cloud",
                "everyayah.com"
            )
        ) url.toString() else null
    }

    private fun stopAudio() {
        audio?.run {
            try {
                if (isPlaying) stop()
            } catch (_: Exception) {
            }
            release()
        }
        audio = null
    }

    private fun playAudio(source: String) {
        val url = safeMedia(source) ?: run {
            Toast.makeText(this, "Audio ünvanı etibarsızdır", Toast.LENGTH_LONG).show()
            return
        }

        if (audio != null) {
            stopAudio()
            return
        }

        Toast.makeText(this, "Audio hazırlanır…", Toast.LENGTH_SHORT).show()

        val player = MediaPlayer()
        audio = player

        try {
            player.setDataSource(url)
            player.setOnPreparedListener {
                if (audio === it) it.start()
            }
            player.setOnCompletionListener { stopAudio() }
            player.setOnErrorListener { _, _, _ ->
                stopAudio()
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            stopAudio()
            Toast.makeText(this, "Audio açıla bilmədi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadPdf(url: String, title: String) {
        val target = safeMedia(url) ?: run {
            Toast.makeText(this, "PDF ünvanı etibarsızdır", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val name = title
                .replace(Regex("[^\\p{L}\\p{N}._ -]"), "")
                .take(60)
                .ifBlank { "Alieba" } + ".pdf"

            val request = DownloadManager.Request(Uri.parse(target))
                .setTitle(name)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    name
                )

            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager)
                .enqueue(request)

            Toast.makeText(
                this,
                "PDF yükləməyə göndərildi",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            Toast.makeText(this, "PDF yüklənmədi", Toast.LENGTH_LONG).show()
        }
    }

    private fun savedArray(): JSONArray = try {
        JSONArray(favorites.getString("items", "[]"))
    } catch (_: Exception) {
        JSONArray()
    }

    private fun isSaved(section: String, id: String): Boolean {
        val items = savedArray()
        return (0 until items.length()).any {
            val item = items.optJSONObject(it)
            item != null &&
                item.optString("section") == section &&
                item.optString("id") == id
        }
    }

    private fun toggleSaved(section: String, id: String, title: String) {
        val items = savedArray()
        val output = JSONArray()
        var found = false

        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            if (
                item.optString("section") == section &&
                item.optString("id") == id
            ) {
                found = true
            } else {
                output.put(item)
            }
        }

        if (!found) {
            output.put(
                JSONObject()
                    .put("section", section)
                    .put("id", id)
                    .put("title", title)
            )
        }

        favorites.edit()
            .putString("items", output.toString())
            .apply()

        Toast.makeText(
            this,
            if (found) "Yadda saxlanandan silindi" else "Yadda saxlandı",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showSaved() {
        body.removeAllViews()
        heading("Yadda saxlananlar")

        val all = savedArray()

        if (all.length() == 0) {
            message("Hələ məzmun saxlamamısınız.")
            return
        }

        for (i in 0 until all.length()) {
            val item = all.optJSONObject(i) ?: continue

            button(
                "♥  ${item.optString("title")}  ·  ${titleFor(item.optString("section"))}"
            ) {
                val destination = item.optString("section")

                if (destination == "quran") {
                    section = "quran"
                    val bits = item.optString("id").split(":")
                    val surah = bits.getOrNull(0)?.toIntOrNull() ?: 1
                    val ayah = bits.getOrNull(1)?.toIntOrNull() ?: 0
                    showSurah(surah, ayah)
                } else {
                    section = destination
                    categoryId = 0
                    query = ""
                    loadContent()
                }
            }
        }
    }

    companion object {
        val surahs = arrayOf(
            "Fatihə","Bəqərə","Ali-İmran","Nisa","Maidə","Ənam","Əraf","Ənfal",
            "Tövbə","Yunus","Hud","Yusuf","Rəd","İbrahim","Hicr","Nəhl","İsra",
            "Kəhf","Məryəm","Taha","Ənbiya","Həcc","Muminun","Nur","Furqan",
            "Şuəra","Nəml","Qəsəs","Ənkəbut","Rum","Loğman","Səcdə","Əhzab",
            "Səba","Fatir","Yasin","Saffat","Sad","Zumər","Ğafir","Fussilət",
            "Şura","Zuxruf","Duxan","Casiyə","Əhqaf","Muhəmməd","Fəth","Hucurat",
            "Qaf","Zariyat","Tur","Nəcm","Qəmər","Rəhman","Vaqiə","Hədid",
            "Mucadilə","Həşr","Mumtəhənə","Saff","Cümə","Munafiqun","Təğabun",
            "Talaq","Təhrim","Mulk","Qələm","Haqqə","Məaric","Nuh","Cin",
            "Muzzəmmil","Muddəssir","Qiyamə","İnsan","Mursəlat","Nəbə","Naziat",
            "Əbəsə","Təkvir","İnfitar","Mutaffifin","İnşiqaq","Buruc","Tariq",
            "Əla","Ğaşiyə","Fəcr","Bələd","Şəms","Leyl","Duha","Şərh","Tin",
            "Ələq","Qədr","Bəyyinə","Zəlzələ","Adiyat","Qariə","Təkasur","Əsr",
            "Huməzə","Fil","Qureyş","Maun","Kövsər","Kafirun","Nəsr","Məsəd",
            "İxlas","Fələq","Nas"
        )

        // Compact native metadata. Exact verse counts are shown after each surah loads.
        val surahMeta = arrayOf(
            "Məkkə","Mədinə","Mədinə","Mədinə","Mədinə","Məkkə","Məkkə","Mədinə",
            "Mədinə","Məkkə","Məkkə","Məkkə","Mədinə","Məkkə","Məkkə","Məkkə",
            "Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Mədinə","Məkkə","Mədinə",
            "Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə",
            "Mədinə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə",
            "Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Mədinə","Mədinə",
            "Mədinə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Mədinə","Məkkə",
            "Mədinə","Mədinə","Mədinə","Mədinə","Mədinə","Mədinə","Mədinə",
            "Mədinə","Mədinə","Mədinə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə",
            "Məkkə","Məkkə","Məkkə","Məkkə","Mədinə","Məkkə","Məkkə","Məkkə",
            "Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə",
            "Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə",
            "Məkkə","Məkkə","Məkkə","Məkkə","Mədinə","Mədinə","Məkkə","Məkkə",
            "Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Mədinə","Məkkə",
            "Məkkə","Məkkə","Məkkə","Məkkə","Məkkə","Məkkə"
        )
    }
}
