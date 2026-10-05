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

class NativeExpertActivity : Activity() {
    private lateinit var body: LinearLayout
    private var expertId = 0
    private var questionFocus = 0

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        NativeUi.setBars(this)
        expertId = intent.getIntExtra("expert_id", 0)
        questionFocus = intent.getIntExtra("question_id", 0)
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
            background = AliebaPatternDrawable(resources.displayMetrics.density)
        }
        root.addView(NativeUi.topBar(this, "Mütəxəssis") { finish() })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeExpertActivity, 13),
                NativeUi.dp(this@NativeExpertActivity, 13),
                NativeUi.dp(this@NativeExpertActivity, 13),
                NativeUi.dp(this@NativeExpertActivity, 28)
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
        if (expertId <= 0) {
            body.addView(NativeUi.text(this, "Mütəxəssis tapılmadı.", 14f, NativeUi.muted))
            return
        }

        body.removeAllViews()
        body.addView(NativeUi.text(this, "Profil yüklənir…", 13f, NativeUi.muted))

        MobileApi.get(
            this,
            "expert",
            mapOf("id" to expertId.toString()),
            cacheKey = "expert_$expertId"
        ) { json, _ ->
            if (json.optBoolean("ok") != true) return@get
            draw(json)
        }
    }

    private fun draw(json: JSONObject) {
        body.removeAllViews()
        val e = json.optJSONObject("expert") ?: return

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                NativeUi.dp(this@NativeExpertActivity, 18),
                NativeUi.dp(this@NativeExpertActivity, 20),
                NativeUi.dp(this@NativeExpertActivity, 18),
                NativeUi.dp(this@NativeExpertActivity, 17)
            )
            background = NativeUi.shape(this@NativeExpertActivity, NativeUi.green, 20)
        }

        val avatarWrap = FrameLayout(this)
        val avatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = NativeUi.shape(this@NativeExpertActivity, 0xffdbeae4.toInt(), 60)
            val url = e.optString("avatar")
            if (url.isNotBlank()) load(MobileApi.absolute(url)) { crossfade(true) }
            else {
                setImageResource(R.drawable.ic_profile)
                setPadding(
                    NativeUi.dp(this@NativeExpertActivity, 23),
                    NativeUi.dp(this@NativeExpertActivity, 23),
                    NativeUi.dp(this@NativeExpertActivity, 23),
                    NativeUi.dp(this@NativeExpertActivity, 23)
                )
            }
        }
        avatarWrap.addView(avatar, FrameLayout.LayoutParams(-1, -1))

        val dot = TextView(this).apply {
            background = NativeUi.shape(
                this@NativeExpertActivity,
                if (e.optBoolean("online")) 0xff22b36b.toInt() else 0xffaeb9b5.toInt(),
                20,
                Color.WHITE,
                3
            )
        }
        avatarWrap.addView(
            dot,
            FrameLayout.LayoutParams(
                NativeUi.dp(this, 17),
                NativeUi.dp(this, 17),
                Gravity.END or Gravity.BOTTOM
            )
        )

        hero.addView(avatarWrap, LinearLayout.LayoutParams(
            NativeUi.dp(this, 105), NativeUi.dp(this, 105)
        ))

        hero.addView(
            NativeUi.text(
                this,
                e.optString("name") + if (e.optBoolean("verified")) "  ✓" else "",
                27f,
                Color.WHITE,
                true
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, NativeUi.dp(this@NativeExpertActivity, 12), 0, 0)
            }
        )

        hero.addView(
            NativeUi.text(this, e.optString("religious_title"), 11f, 0xffe3c877.toInt(), true).apply {
                gravity = Gravity.CENTER
                setPadding(0, NativeUi.dp(this@NativeExpertActivity, 5), 0, 0)
            }
        )

        hero.addView(
            NativeUi.text(
                this,
                if (e.optBoolean("online")) "● Onlayn" else "● Offline",
                9f,
                if (e.optBoolean("online")) 0xffaaf0c6.toInt() else 0xffc8d3cf.toInt(),
                true
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, NativeUi.dp(this@NativeExpertActivity, 5), 0, 0)
            }
        )

        val stats = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, NativeUi.dp(this@NativeExpertActivity, 14), 0, 0)
        }

        listOf(
            "${e.optInt("answered")}\nCavab",
            "★ ${e.optString("rating")}\n${e.optInt("rating_count")} səs",
            "${e.optInt("likes")}\nBəyənmə"
        ).forEach {
            stats.addView(
                NativeUi.text(this, it, 11f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    background = NativeUi.shape(this@NativeExpertActivity, 0x22ffffff, 13)
                },
                LinearLayout.LayoutParams(0, NativeUi.dp(this, 60), 1f).apply {
                    leftMargin = NativeUi.dp(this@NativeExpertActivity, 3)
                    rightMargin = NativeUi.dp(this@NativeExpertActivity, 3)
                }
            )
        }

        hero.addView(stats, LinearLayout.LayoutParams(-1, -2))
        body.addView(hero, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = NativeUi.dp(this@NativeExpertActivity, 11)
        })

        ratingBox(e)
        askBox(e)

        body.addView(
            NativeUi.text(this, "Cavablandırılmış suallar", 22f, NativeUi.ink, true),
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = NativeUi.dp(this@NativeExpertActivity, 10)
                bottomMargin = NativeUi.dp(this@NativeExpertActivity, 9)
            }
        )

        val answers = json.optJSONArray("answers") ?: JSONArray()
        for (i in 0 until answers.length()) {
            val q = answers.optJSONObject(i) ?: continue
            body.addView(
                answerCard(q),
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = NativeUi.dp(this@NativeExpertActivity, 10)
                }
            )
        }
    }

    private fun ratingBox(e: JSONObject) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeExpertActivity, 14),
                NativeUi.dp(this@NativeExpertActivity, 13),
                NativeUi.dp(this@NativeExpertActivity, 14),
                NativeUi.dp(this@NativeExpertActivity, 13)
            )
            background = NativeUi.card(this@NativeExpertActivity)
        }

        box.addView(NativeUi.text(this, "★  Mütəxəssisə reytinq ver", 14f, NativeUi.ink, true))
        box.addView(
            NativeUi.text(this, "1–5 ulduz seç. Sonradan səsini dəyişə bilərsən.", 10f, NativeUi.muted).apply {
                setPadding(0, NativeUi.dp(this@NativeExpertActivity, 4), 0, NativeUi.dp(this@NativeExpertActivity, 8))
            }
        )

        val line = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        for (rating in 1..5) {
            line.addView(
                NativeUi.text(this, "★", 26f, NativeUi.gold).apply {
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        if (FirebaseAuth.getInstance().currentUser == null) {
                            startActivity(Intent(this@NativeExpertActivity, NativeProfileActivity::class.java))
                        } else {
                            MobileApi.post(
                                this@NativeExpertActivity,
                                "rate",
                                mapOf(
                                    "expert_id" to expertId.toString(),
                                    "rating" to rating.toString()
                                ),
                                auth = true
                            ) { load() }
                        }
                    }
                },
                LinearLayout.LayoutParams(NativeUi.dp(this, 42), NativeUi.dp(this, 42))
            )
        }
        box.addView(line)

        body.addView(box, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = NativeUi.dp(this@NativeExpertActivity, 10)
        })
    }

    private fun askBox(e: JSONObject) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeExpertActivity, 14),
                NativeUi.dp(this@NativeExpertActivity, 13),
                NativeUi.dp(this@NativeExpertActivity, 14),
                NativeUi.dp(this@NativeExpertActivity, 13)
            )
            background = NativeUi.card(this@NativeExpertActivity)
        }

        box.addView(NativeUi.text(this, "?  Sual ver", 15f, NativeUi.ink, true))
        box.addView(
            NativeUi.text(this, "Sual birbaşa bu mütəxəssisə gedəcək.", 10f, NativeUi.muted).apply {
                setPadding(0, NativeUi.dp(this@NativeExpertActivity, 4), 0, NativeUi.dp(this@NativeExpertActivity, 8))
            }
        )

        if (FirebaseAuth.getInstance().currentUser == null) {
            box.addView(
                NativeUi.text(this, "Sual üçün Google hesabınla daxil ol", 12f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    background = NativeUi.shape(this@NativeExpertActivity, NativeUi.green, 12)
                    setOnClickListener {
                        startActivity(Intent(this@NativeExpertActivity, NativeProfileActivity::class.java))
                    }
                },
                LinearLayout.LayoutParams(-1, NativeUi.dp(this, 44))
            )
        } else {
            val field = EditText(this).apply {
                hint = "Dini sualını aydın yaz…"
                minLines = 3
                maxLines = 7
                gravity = Gravity.TOP
                setPadding(
                    NativeUi.dp(this@NativeExpertActivity, 12),
                    NativeUi.dp(this@NativeExpertActivity, 11),
                    NativeUi.dp(this@NativeExpertActivity, 12),
                    NativeUi.dp(this@NativeExpertActivity, 11)
                )
                background = NativeUi.shape(this@NativeExpertActivity, NativeUi.bg, 12, NativeUi.line)
            }
            box.addView(field, LinearLayout.LayoutParams(-1, -2))

            box.addView(
                NativeUi.text(this, "Sualı göndər", 12f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    background = NativeUi.shape(this@NativeExpertActivity, NativeUi.green, 12)
                    setOnClickListener {
                        val q = field.text.toString().trim()
                        if (q.length !in 12..2000) {
                            field.error = "Sual 12–2000 simvol olmalıdır"
                            return@setOnClickListener
                        }

                        MobileApi.post(
                            this@NativeExpertActivity,
                            "ask",
                            mapOf(
                                "expert_id" to expertId.toString(),
                                "question" to q
                            ),
                            auth = true
                        ) { result ->
                            if (result?.optBoolean("ok") == true) {
                                field.setText("")
                                Toast.makeText(
                                    this@NativeExpertActivity,
                                    "Sual göndərildi",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@NativeExpertActivity,
                                    "Sual göndərilmədi",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                },
                LinearLayout.LayoutParams(-1, NativeUi.dp(this, 44)).apply {
                    topMargin = NativeUi.dp(this@NativeExpertActivity, 8)
                }
            )
        }

        body.addView(box, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = NativeUi.dp(this@NativeExpertActivity, 10)
        })
    }

    private fun answerCard(q: JSONObject): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeExpertActivity, 14),
                NativeUi.dp(this@NativeExpertActivity, 14),
                NativeUi.dp(this@NativeExpertActivity, 14),
                NativeUi.dp(this@NativeExpertActivity, 14)
            )
            background = NativeUi.card(this@NativeExpertActivity)
        }

        card.addView(
            NativeUi.text(this, q.optString("asker_name") + " • " + q.optString("date"), 9.5f, NativeUi.muted)
        )

        card.addView(
            block("Sual", q.optString("question"), false),
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = NativeUi.dp(this@NativeExpertActivity, 9)
            }
        )

        card.addView(
            block("Cavab", q.optString("answer"), true),
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = NativeUi.dp(this@NativeExpertActivity, 8)
            }
        )

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, NativeUi.dp(this@NativeExpertActivity, 10), 0, 0)
        }

        bottom.addView(
            NativeUi.text(this, "♡  ${q.optInt("likes")}", 10f, NativeUi.muted, true).apply {
                setOnClickListener {
                    MobileApi.post(
                        this@NativeExpertActivity,
                        "like",
                        mapOf("question_id" to q.optInt("id").toString()),
                        auth = true
                    ) { load() }
                }
            }
        )
        bottom.addView(
            NativeUi.text(this, "◉  ${q.optInt("views")}", 10f, NativeUi.muted, true).apply {
                setPadding(NativeUi.dp(this@NativeExpertActivity, 14), 0, 0, 0)
            }
        )

        card.addView(bottom)
        return card
    }

    private fun block(label: String, value: String, answer: Boolean) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeExpertActivity, 13),
                NativeUi.dp(this@NativeExpertActivity, 12),
                NativeUi.dp(this@NativeExpertActivity, 13),
                NativeUi.dp(this@NativeExpertActivity, 12)
            )
            background = NativeUi.shape(
                this@NativeExpertActivity,
                if (answer) 0xffeaf7f0.toInt() else 0xfff7f9f8.toInt(),
                13,
                if (answer) 0xffd2e8dc.toInt() else NativeUi.line
            )
            addView(NativeUi.text(this@NativeExpertActivity, label, 9f,
                if (answer) NativeUi.green else NativeUi.muted, true))
            addView(
                NativeUi.text(this@NativeExpertActivity, value, 14f, NativeUi.ink).apply {
                    setPadding(0, NativeUi.dp(this@NativeExpertActivity, 7), 0, 0)
                }
            )
        }
}
