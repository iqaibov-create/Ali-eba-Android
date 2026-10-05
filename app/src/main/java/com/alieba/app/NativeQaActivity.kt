package com.alieba.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import coil.load
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

class NativeQaActivity : Activity() {
    private lateinit var body: LinearLayout
    private var mode = "feed"

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        NativeUi.setBars(this)
        mode = intent.getStringExtra("mode") ?: "feed"
        buildBase()
        load()
    }

    override fun onResume() {
        super.onResume()
        MobileApi.heartbeat(this)
    }

    private fun buildBase() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(NativeUi.bg)
        }

        root.addView(NativeUi.topBar(this, "Dini sual-cavab") { finish() })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeQaActivity, 13),
                NativeUi.dp(this@NativeQaActivity, 13),
                NativeUi.dp(this@NativeQaActivity, 13),
                NativeUi.dp(this@NativeQaActivity, 28)
            )
        }

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(
            NativeNav.make(this, "qa"),
            LinearLayout.LayoutParams(-1, NativeUi.dp(this, 87))
        )

        setContentView(root)
    }

    private fun load() {
        body.removeAllViews()

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeQaActivity, 17),
                NativeUi.dp(this@NativeQaActivity, 16),
                NativeUi.dp(this@NativeQaActivity, 17),
                NativeUi.dp(this@NativeQaActivity, 16)
            )
            background = NativeUi.shape(this@NativeQaActivity, NativeUi.green, 19)
        }
        hero.addView(NativeUi.text(this, "Dini sual-cavab", 25f, Color.WHITE, true))
        hero.addView(
            NativeUi.text(
                this,
                "Təsdiqli dini mütəxəssislərə sual ver, cavabları və reytinqi izlə.",
                12f,
                0xffd8ebe4.toInt()
            ).apply { setPadding(0, NativeUi.dp(this@NativeQaActivity, 6), 0, 0) }
        )
        body.addView(hero, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = NativeUi.dp(this@NativeQaActivity, 10)
        })

        val tabs = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        listOf(
            "feed" to "Cavablar",
            "experts" to "Mütəxəssislər",
            "my" to "Suallarım"
        ).forEach { (key, label) ->
            val selected = mode == key
            tabs.addView(
                NativeUi.text(
                    this,
                    label,
                    10f,
                    if (selected) Color.WHITE else NativeUi.ink,
                    selected
                ).apply {
                    gravity = Gravity.CENTER
                    background = NativeUi.shape(
                        this@NativeQaActivity,
                        if (selected) NativeUi.green else Color.WHITE,
                        14,
                        if (selected) null else NativeUi.line
                    )
                    setOnClickListener {
                        mode = key
                        load()
                    }
                },
                LinearLayout.LayoutParams(0, NativeUi.dp(this, 39), 1f).apply {
                    if (key != "feed") leftMargin = NativeUi.dp(this@NativeQaActivity, 5)
                }
            )
        }

        body.addView(tabs, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = NativeUi.dp(this@NativeQaActivity, 12)
        })

        when (mode) {
            "experts" -> loadExperts()
            "my" -> loadMine()
            else -> loadFeed()
        }
    }

    private fun loadFeed() {
        val status = NativeUi.text(this, "Cavablar yüklənir…", 12f, NativeUi.muted)
        body.addView(status)

        MobileApi.get(this, "qa", cacheKey = "qa_feed") { json, fromCache ->
            if (json.optBoolean("ok") != true) return@get
            val items = json.optJSONArray("items") ?: JSONArray()

            body.removeView(status)

            if (fromCache) {
                body.addView(
                    NativeUi.text(this, "Keşdən açıldı • arxa planda yenilənir", 9f, NativeUi.muted),
                    LinearLayout.LayoutParams(-1, -2).apply {
                        bottomMargin = NativeUi.dp(this@NativeQaActivity, 7)
                    }
                )
            }

            if (items.length() == 0) {
                body.addView(NativeUi.text(this, "Hələ cavab yoxdur.", 13f, NativeUi.muted))
                return@get
            }

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                body.addView(
                    qaCard(item),
                    LinearLayout.LayoutParams(-1, -2).apply {
                        bottomMargin = NativeUi.dp(this@NativeQaActivity, 11)
                    }
                )
            }
        }
    }

    private fun qaCard(item: JSONObject): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeQaActivity, 14),
                NativeUi.dp(this@NativeQaActivity, 14),
                NativeUi.dp(this@NativeQaActivity, 14),
                NativeUi.dp(this@NativeQaActivity, 14)
            )
            background = NativeUi.card(this@NativeQaActivity)
        }

        val expert = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener {
                startActivity(
                    Intent(this@NativeQaActivity, NativeExpertActivity::class.java)
                        .putExtra("expert_id", item.optInt("expert_id"))
                )
            }
        }

        val avatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = NativeUi.shape(this@NativeQaActivity, NativeUi.soft, 25)
            val url = item.optString("expert_avatar")
            if (url.isNotBlank()) {
                load(MobileApi.absolute(url)) {
                    crossfade(true)
                    diskCacheKey("expert_${item.optInt("expert_id")}")
                }
            } else {
                setImageResource(R.drawable.ic_profile)
                setPadding(
                    NativeUi.dp(this@NativeQaActivity, 12),
                    NativeUi.dp(this@NativeQaActivity, 12),
                    NativeUi.dp(this@NativeQaActivity, 12),
                    NativeUi.dp(this@NativeQaActivity, 12)
                )
            }
        }
        expert.addView(avatar, LinearLayout.LayoutParams(
            NativeUi.dp(this, 48), NativeUi.dp(this, 48)
        ))

        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(NativeUi.dp(this@NativeQaActivity, 10), 0, 0, 0)
        }

        copy.addView(
            NativeUi.text(
                this,
                item.optString("expert_name") + if (item.optBoolean("verified")) "  ✓" else "",
                14f,
                NativeUi.ink,
                true
            )
        )

        copy.addView(
            NativeUi.text(
                this,
                item.optString("religious_title") +
                    " • " + if (item.optBoolean("online")) "Onlayn" else "Offline",
                9.5f,
                if (item.optBoolean("online")) 0xff168052.toInt() else NativeUi.muted,
                true
            ).apply { setPadding(0, NativeUi.dp(this@NativeQaActivity, 3), 0, 0) }
        )

        expert.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(expert)

        card.addView(
            qaBlock("Sual", item.optString("question"), false),
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = NativeUi.dp(this@NativeQaActivity, 12)
            }
        )
        card.addView(
            qaBlock("Cavab", item.optString("answer"), true),
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = NativeUi.dp(this@NativeQaActivity, 9)
            }
        )

        val stats = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, NativeUi.dp(this@NativeQaActivity, 11), 0, 0)
        }
        stats.addView(NativeUi.text(this, "♡  ${item.optInt("likes")}", 10f, NativeUi.muted, true))
        stats.addView(NativeUi.text(this, "◉  ${item.optInt("views")}", 10f, NativeUi.muted, true).apply {
            setPadding(NativeUi.dp(this@NativeQaActivity, 14), 0, 0, 0)
        })
        stats.addView(LinearLayout(this), LinearLayout.LayoutParams(0, 1, 1f))

        val open = NativeUi.text(this, "Aç →", 10f, NativeUi.green, true).apply {
            setOnClickListener {
                startActivity(
                    Intent(this@NativeQaActivity, NativeExpertActivity::class.java)
                        .putExtra("expert_id", item.optInt("expert_id"))
                        .putExtra("question_id", item.optInt("id"))
                )
            }
        }
        stats.addView(open)
        card.addView(stats)

        return card
    }

    private fun qaBlock(label: String, value: String, answer: Boolean) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeQaActivity, 13),
                NativeUi.dp(this@NativeQaActivity, 12),
                NativeUi.dp(this@NativeQaActivity, 13),
                NativeUi.dp(this@NativeQaActivity, 12)
            )
            background = NativeUi.shape(
                this@NativeQaActivity,
                if (answer) 0xffeaf7f0.toInt() else 0xfff7f9f8.toInt(),
                13,
                if (answer) 0xffd2e8dc.toInt() else NativeUi.line
            )

            addView(NativeUi.text(this@NativeQaActivity, label, 9f,
                if (answer) NativeUi.green else NativeUi.muted, true))
            addView(
                NativeUi.text(this@NativeQaActivity, value, 14f, NativeUi.ink).apply {
                    setPadding(0, NativeUi.dp(this@NativeQaActivity, 7), 0, 0)
                }
            )
        }

    private fun loadExperts() {
        val status = NativeUi.text(this, "Mütəxəssislər yüklənir…", 12f, NativeUi.muted)
        body.addView(status)

        MobileApi.get(this, "experts", cacheKey = "experts") { json, _ ->
            if (json.optBoolean("ok") != true) return@get
            body.removeView(status)

            val items = json.optJSONArray("items") ?: JSONArray()
            if (items.length() == 0) {
                body.addView(NativeUi.text(this, "Hələ təsdiqli mütəxəssis yoxdur.", 13f, NativeUi.muted))
                return@get
            }

            for (i in 0 until items.length()) {
                val e = items.optJSONObject(i) ?: continue
                body.addView(
                    expertCard(e),
                    LinearLayout.LayoutParams(-1, -2).apply {
                        bottomMargin = NativeUi.dp(this@NativeQaActivity, 9)
                    }
                )
            }
        }
    }

    private fun expertCard(e: JSONObject): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                NativeUi.dp(this@NativeQaActivity, 12),
                NativeUi.dp(this@NativeQaActivity, 12),
                NativeUi.dp(this@NativeQaActivity, 12),
                NativeUi.dp(this@NativeQaActivity, 12)
            )
            background = NativeUi.card(this@NativeQaActivity)
            setOnClickListener {
                startActivity(
                    Intent(this@NativeQaActivity, NativeExpertActivity::class.java)
                        .putExtra("expert_id", e.optInt("id"))
                )
            }
        }

        val avatarWrap = FrameLayout(this)
        val avatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = NativeUi.shape(this@NativeQaActivity, NativeUi.soft, 28)
            val url = e.optString("avatar")
            if (url.isNotBlank()) load(MobileApi.absolute(url)) { crossfade(true) }
            else {
                setImageResource(R.drawable.ic_profile)
                setPadding(
                    NativeUi.dp(this@NativeQaActivity, 13),
                    NativeUi.dp(this@NativeQaActivity, 13),
                    NativeUi.dp(this@NativeQaActivity, 13),
                    NativeUi.dp(this@NativeQaActivity, 13)
                )
            }
        }
        avatarWrap.addView(avatar, FrameLayout.LayoutParams(-1, -1))

        val dot = TextView(this).apply {
            background = NativeUi.shape(
                this@NativeQaActivity,
                if (e.optBoolean("online")) 0xff22b36b.toInt() else 0xffaeb9b5.toInt(),
                10,
                Color.WHITE,
                2
            )
        }
        avatarWrap.addView(
            dot,
            FrameLayout.LayoutParams(
                NativeUi.dp(this, 13),
                NativeUi.dp(this, 13),
                Gravity.END or Gravity.BOTTOM
            )
        )

        row.addView(avatarWrap, LinearLayout.LayoutParams(
            NativeUi.dp(this, 56), NativeUi.dp(this, 56)
        ))

        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(NativeUi.dp(this@NativeQaActivity, 11), 0, 0, 0)
        }
        copy.addView(
            NativeUi.text(
                this,
                e.optString("name") + if (e.optBoolean("verified")) "  ✓" else "",
                15f,
                NativeUi.ink,
                true
            )
        )
        copy.addView(
            NativeUi.text(this, e.optString("religious_title"), 10f, NativeUi.muted).apply {
                setPadding(0, NativeUi.dp(this@NativeQaActivity, 3), 0, 0)
            }
        )
        copy.addView(
            NativeUi.text(
                this,
                "★ ${e.optString("rating")}  •  ${e.optInt("rating_count")} səs  •  ${e.optInt("answered")} cavab",
                9.5f,
                NativeUi.gold,
                true
            ).apply {
                setPadding(0, NativeUi.dp(this@NativeQaActivity, 5), 0, 0)
            }
        )
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(NativeUi.text(this, "›", 25f, NativeUi.green))

        return row
    }

    private fun loadMine() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            body.addView(
                NativeUi.text(this, "Suallarını görmək üçün Google hesabınla daxil ol.", 13f, NativeUi.muted)
            )
            body.addView(
                NativeUi.text(this, "Google ilə daxil ol", 13f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    background = NativeUi.shape(this@NativeQaActivity, NativeUi.green, 13)
                    setOnClickListener {
                        startActivity(Intent(this@NativeQaActivity, NativeProfileActivity::class.java))
                    }
                },
                LinearLayout.LayoutParams(-1, NativeUi.dp(this, 46)).apply {
                    topMargin = NativeUi.dp(this@NativeQaActivity, 10)
                }
            )
            return
        }

        val status = NativeUi.text(this, "Sualların yüklənir…", 12f, NativeUi.muted)
        body.addView(status)

        MobileApi.post(this, "my_questions", auth = true) { json ->
            body.removeView(status)

            if (json?.optBoolean("ok") != true) {
                body.addView(NativeUi.text(this, "Suallar yüklənmədi.", 13f, NativeUi.muted))
                return@post
            }

            val items = json.optJSONArray("items") ?: JSONArray()
            if (items.length() == 0) {
                body.addView(NativeUi.text(this, "Hələ sual göndərməmisən.", 13f, NativeUi.muted))
                return@post
            }

            for (i in 0 until items.length()) {
                val q = items.optJSONObject(i) ?: continue

                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(
                        NativeUi.dp(this@NativeQaActivity, 14),
                        NativeUi.dp(this@NativeQaActivity, 14),
                        NativeUi.dp(this@NativeQaActivity, 14),
                        NativeUi.dp(this@NativeQaActivity, 14)
                    )
                    background = NativeUi.card(this@NativeQaActivity)
                }

                card.addView(
                    NativeUi.text(
                        this,
                        q.optString("expert_name") + " • " + q.optString("status_label"),
                        11f,
                        if (q.optString("status") == "answered") NativeUi.green else NativeUi.gold,
                        true
                    )
                )

                card.addView(qaBlock("Sualın", q.optString("question"), false),
                    LinearLayout.LayoutParams(-1, -2).apply {
                        topMargin = NativeUi.dp(this@NativeQaActivity, 9)
                    })

                if (q.optString("answer").isNotBlank()) {
                    card.addView(qaBlock("Cavab", q.optString("answer"), true),
                        LinearLayout.LayoutParams(-1, -2).apply {
                            topMargin = NativeUi.dp(this@NativeQaActivity, 8)
                        })
                }

                body.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = NativeUi.dp(this@NativeQaActivity, 9)
                })
            }
        }
    }
}
