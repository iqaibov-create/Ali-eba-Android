package com.alieba.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class NativeHomeActivity : Activity() {
    private lateinit var body: LinearLayout
    private var setupLaunched = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        NativeUi.setBars(this)

        if (!setupReady()) {
            setupLaunched = true
            startActivity(Intent(this, MainActivity::class.java))
            return
        }

        build()
        routeIntent(intent)

        FirebaseMessaging.getInstance().subscribeToTopic("alieba_news")
        FirebaseMessaging.getInstance().subscribeToTopic("alieba_content")
        MobileApi.heartbeat(this)
        MobileApi.registerDevice(this)

        PrayerClock.fetchAndSchedule(this) {
            if (!isFinishing && !isDestroyed) refreshPrayerBox()
        }
    }

    override fun onResume() {
        super.onResume()

        if (setupLaunched && setupReady()) {
            setupLaunched = false
            build()
        }

        if (setupReady()) {
            PrayerClock.scheduleToday(this)
            MobileApi.heartbeat(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeIntent(intent)
    }

    private fun setupReady() =
        getSharedPreferences("alieba_setup", MODE_PRIVATE)
            .getBoolean("completed", false)

    private fun routeIntent(intent: Intent?) {
        when (intent?.getStringExtra("open_type")) {
            "news" -> startActivity(
                Intent(this, NativeNewsActivity::class.java)
                    .putExtra("news_id", intent.getIntExtra("item_id", 0))
            )

            "qa", "qa_answer" -> startActivity(
                Intent(this, NativeQaActivity::class.java)
                    .putExtra("mode", "my")
            )

            "content" -> startActivity(
                Intent(this, NativeContentActivity::class.java)
                    .putExtra("section", intent.getStringExtra("section") ?: "hadis")
            )
        }
    }

    private fun build() {
        NativeUi.setBars(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(NativeUi.bg)
        }

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                NativeUi.dp(this@NativeHomeActivity, 18),
                NativeUi.dp(this@NativeHomeActivity, 13),
                NativeUi.dp(this@NativeHomeActivity, 15),
                NativeUi.dp(this@NativeHomeActivity, 11)
            )
            setBackgroundColor(Color.WHITE)
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        brand.addView(NativeUi.text(this, "☪  Alieba", 25f, NativeUi.green, true))
        brand.addView(
            NativeUi.text(this, "İmanla yaşa", 11f, NativeUi.muted).apply {
                setPadding(0, NativeUi.dp(this@NativeHomeActivity, 2), 0, 0)
            }
        )
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))

        val user = FirebaseAuth.getInstance().currentUser
        val profile = NativeUi.text(
            this,
            if (user == null) "Daxil ol" else "Profilim",
            13f,
            NativeUi.green,
            true
        ).apply {
            gravity = Gravity.CENTER
            background = NativeUi.shape(
                this@NativeHomeActivity,
                0xfff6fbf8.toInt(),
                18,
                NativeUi.line
            )
            setOnClickListener {
                startActivity(Intent(this@NativeHomeActivity, NativeProfileActivity::class.java))
            }
        }
        top.addView(
            profile,
            LinearLayout.LayoutParams(
                NativeUi.dp(this, 90),
                NativeUi.dp(this, 42)
            )
        )

        root.addView(top)
        root.addView(View(this).apply { setBackgroundColor(NativeUi.line) },
            LinearLayout.LayoutParams(-1, NativeUi.dp(this, 1)))

        val scroll = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
        }

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeHomeActivity, 14),
                NativeUi.dp(this@NativeHomeActivity, 14),
                NativeUi.dp(this@NativeHomeActivity, 14),
                NativeUi.dp(this@NativeHomeActivity, 28)
            )
        }

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(
            NativeNav.make(this, "home"),
            LinearLayout.LayoutParams(-1, NativeUi.dp(this, 87))
        )

        setContentView(root)
        renderHome()
    }

    private fun renderHome() {
        body.removeAllViews()

        val intro = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeHomeActivity, 18),
                NativeUi.dp(this@NativeHomeActivity, 17),
                NativeUi.dp(this@NativeHomeActivity, 18),
                NativeUi.dp(this@NativeHomeActivity, 17)
            )
            background = NativeUi.shape(this@NativeHomeActivity, Color.WHITE, 20, NativeUi.line)
        }

        intro.addView(
            NativeUi.text(this, "Qəlbini zikrlə işıqlandır.", 25f, NativeUi.ink, true)
        )
        intro.addView(
            NativeUi.text(
                this,
                "Quran, dini sual-cavab, xəbərlər və gündəlik ibadət köməkçisi.",
                13f,
                NativeUi.muted
            ).apply {
                setPadding(0, NativeUi.dp(this@NativeHomeActivity, 7), 0, 0)
            }
        )

        body.addView(
            intro,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = NativeUi.dp(this@NativeHomeActivity, 12)
            }
        )

        body.addView(prayerBox(), LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = NativeUi.dp(this@NativeHomeActivity, 15)
        })

        body.addView(
            NativeUi.text(this, "Bölmələr", 21f, NativeUi.ink, true),
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = NativeUi.dp(this@NativeHomeActivity, 10)
            }
        )

        val grid = GridLayout(this).apply {
            columnCount = 2
            useDefaultMargins = false
        }

        val items = listOf(
            Triple("Qurani-Kərim", "Ərəb mətn, tərcümə, qiraət", "quran"),
            Triple("Yeniliklər", "Saytdakı xəbərlər və bildirişlər", "news"),
            Triple("Dini sual-cavab", "Təsdiqli mütəxəssislər", "qa"),
            Triple("Mütəxəssislər", "Online, verify, reytinq", "experts"),
            Triple("Məfatih", "Dua və ziyarətlər", "mafatih"),
            Triple("Hədislər", "Hədis və rəvayətlər", "hadis"),
            Triple("Əhkam", "Dini hökmlər", "ahkam"),
            Triple("Kitabxana", "Kitab və PDF", "kitabxana"),
            Triple("Mərsiyələr", "Dini mətnlər", "mersiye"),
            Triple("Zikr və təsbeh", "Gündəlik zikr", "zikr")
        )

        items.forEachIndexed { index, item ->
            grid.addView(
                sectionCard(item.first, item.second, item.third),
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = NativeUi.dp(this@NativeHomeActivity, 116)
                    columnSpec = GridLayout.spec(index % 2, 1f)
                    setMargins(
                        if (index % 2 == 0) 0 else NativeUi.dp(this@NativeHomeActivity, 5),
                        0,
                        if (index % 2 == 0) NativeUi.dp(this@NativeHomeActivity, 5) else 0,
                        NativeUi.dp(this@NativeHomeActivity, 10)
                    )
                }
            )
        }

        body.addView(grid, LinearLayout.LayoutParams(-1, -2))
    }

    private fun prayerBox(): View {
        val outer = LinearLayout(this).apply {
            tag = "prayer_box"
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeHomeActivity, 15),
                NativeUi.dp(this@NativeHomeActivity, 14),
                NativeUi.dp(this@NativeHomeActivity, 15),
                NativeUi.dp(this@NativeHomeActivity, 14)
            )
            background = NativeUi.shape(
                this@NativeHomeActivity,
                0xfff0f8f4.toInt(),
                18,
                0xffd6e9e0.toInt()
            )
        }

        val title = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        title.addView(
            NativeUi.text(this, "Namaz vaxtları", 18f, NativeUi.ink, true),
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        title.addView(
            NativeUi.text(this, "Şiə / Cəfəri", 10f, NativeUi.gold, true)
        )
        outer.addView(title)

        val times = PrayerClock.displayTimes(this)
        val names = listOf("Fəcr","Günəş","Zöhr","Günəş batımı","Məğrib","Gecə yarısı")
        val keys = PrayerClock.keys

        val grid = GridLayout(this).apply {
            columnCount = 3
            setPadding(0, NativeUi.dp(this@NativeHomeActivity, 10), 0, 0)
        }

        for (i in keys.indices) {
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(
                    NativeUi.dp(this@NativeHomeActivity, 5),
                    NativeUi.dp(this@NativeHomeActivity, 8),
                    NativeUi.dp(this@NativeHomeActivity, 5),
                    NativeUi.dp(this@NativeHomeActivity, 8)
                )
                background = NativeUi.shape(
                    this@NativeHomeActivity,
                    Color.WHITE,
                    12,
                    NativeUi.line
                )
            }

            cell.addView(NativeUi.text(this, names[i], 9f, NativeUi.muted).apply {
                gravity = Gravity.CENTER
            })
            cell.addView(NativeUi.text(this, times[keys[i]] ?: "--:--", 15f, NativeUi.ink, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, NativeUi.dp(this@NativeHomeActivity, 3), 0, 0)
            })

            grid.addView(
                cell,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = NativeUi.dp(this@NativeHomeActivity, 68)
                    columnSpec = GridLayout.spec(i % 3, 1f)
                    setMargins(
                        if (i % 3 == 0) 0 else NativeUi.dp(this@NativeHomeActivity, 3),
                        if (i >= 3) NativeUi.dp(this@NativeHomeActivity, 5) else 0,
                        if (i % 3 == 2) 0 else NativeUi.dp(this@NativeHomeActivity, 3),
                        0
                    )
                }
            )
        }

        outer.addView(grid)
        return outer
    }

    private fun refreshPrayerBox() {
        if (!::body.isInitialized) return
        renderHome()
    }

    private fun sectionCard(title: String, subtitle: String, target: String): View {
        val icon = when (target) {
            "quran" -> "۞"
            "news" -> "▤"
            "qa" -> "?"
            "experts" -> "✓"
            "mafatih" -> "☾"
            "hadis" -> "❝"
            "ahkam" -> "⚖"
            "kitabxana" -> "▣"
            "mersiye" -> "♪"
            "zikr" -> "◉"
            else -> "•"
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeHomeActivity, 13),
                NativeUi.dp(this@NativeHomeActivity, 12),
                NativeUi.dp(this@NativeHomeActivity, 13),
                NativeUi.dp(this@NativeHomeActivity, 12)
            )
            background = NativeUi.card(this@NativeHomeActivity)
            setOnClickListener {
                when (target) {
                    "news" -> startActivity(Intent(this@NativeHomeActivity, NativeNewsActivity::class.java))
                    "qa" -> startActivity(Intent(this@NativeHomeActivity, NativeQaActivity::class.java))
                    "experts" -> startActivity(
                        Intent(this@NativeHomeActivity, NativeQaActivity::class.java)
                            .putExtra("mode", "experts")
                    )
                    "zikr" -> startActivity(Intent(this@NativeHomeActivity, ZikrActivity::class.java))
                    else -> startActivity(
                        Intent(this@NativeHomeActivity, NativeContentActivity::class.java)
                            .putExtra("section", target)
                    )
                }
            }

            addView(NativeUi.text(this@NativeHomeActivity, icon, 22f, NativeUi.green, true))
            addView(
                NativeUi.text(this@NativeHomeActivity, title, 14f, NativeUi.ink, true).apply {
                    setPadding(0, NativeUi.dp(this@NativeHomeActivity, 8), 0, 0)
                }
            )
            addView(
                NativeUi.text(this@NativeHomeActivity, subtitle, 10f, NativeUi.muted).apply {
                    setPadding(0, NativeUi.dp(this@NativeHomeActivity, 4), 0, 0)
                }
            )
        }
    }
}
