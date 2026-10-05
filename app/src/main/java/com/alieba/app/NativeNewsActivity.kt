package com.alieba.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import coil.load
import org.json.JSONArray
import org.json.JSONObject

class NativeNewsActivity : Activity() {
    private lateinit var body: LinearLayout
    private var currentNewsId = 0
    private var lastItems = JSONArray()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        NativeUi.setBars(this)
        buildBase()

        currentNewsId = intent.getIntExtra("news_id", 0)
        if (currentNewsId > 0) openNews(currentNewsId)
        else loadNews()
    }

    private fun buildBase() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(NativeUi.bg)
        }

        root.addView(NativeUi.topBar(this, "Yeniliklər") { finish() })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeNewsActivity, 13),
                NativeUi.dp(this@NativeNewsActivity, 13),
                NativeUi.dp(this@NativeNewsActivity, 13),
                NativeUi.dp(this@NativeNewsActivity, 28)
            )
        }

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(
            NativeNav.make(this, "news"),
            LinearLayout.LayoutParams(-1, NativeUi.dp(this, 87))
        )

        setContentView(root)
    }

    private fun loadNews() {
        body.removeAllViews()
        body.addView(NativeUi.text(this, "Son yeniliklər", 25f, NativeUi.ink, true))
        body.addView(NativeUi.text(this, "Saytda yayımlanan xəbərlər APK-da native görünür.", 12f, NativeUi.muted).apply {
            setPadding(0, NativeUi.dp(this@NativeNewsActivity, 5), 0, NativeUi.dp(this@NativeNewsActivity, 12))
        })

        val loading = NativeUi.text(this, "Yüklənir…", 13f, NativeUi.muted)
        body.addView(loading)

        MobileApi.get(
            this,
            "news",
            cacheKey = "news_list"
        ) { json, fromCache ->
            if (json.optBoolean("ok") != true) return@get
            val items = json.optJSONArray("items") ?: JSONArray()
            lastItems = items
            drawNews(items, fromCache)
        }
    }

    private fun drawNews(items: JSONArray, fromCache: Boolean) {
        body.removeAllViews()
        body.addView(NativeUi.text(this, "Son yeniliklər", 25f, NativeUi.ink, true))

        if (fromCache) {
            body.addView(
                NativeUi.text(this, "Keşdən dərhal açıldı • arxa planda yenilənir", 9.5f, NativeUi.muted).apply {
                    setPadding(0, NativeUi.dp(this@NativeNewsActivity, 5), 0, NativeUi.dp(this@NativeNewsActivity, 10))
                }
            )
        } else {
            body.addView(
                NativeUi.text(this, "Saytdakı son xəbərlər", 11f, NativeUi.muted).apply {
                    setPadding(0, NativeUi.dp(this@NativeNewsActivity, 5), 0, NativeUi.dp(this@NativeNewsActivity, 10))
                }
            )
        }

        if (items.length() == 0) {
            body.addView(NativeUi.text(this, "Hələ xəbər yoxdur.", 14f, NativeUi.muted))
            return
        }

        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            body.addView(
                newsCard(item),
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = NativeUi.dp(this@NativeNewsActivity, 12)
                }
            )
        }
    }

    private fun newsCard(item: JSONObject): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = NativeUi.card(this@NativeNewsActivity)
            setOnClickListener { openNews(item.optInt("id")) }
        }

        val image = item.optString("image")
        if (image.isNotBlank()) {
            card.addView(
                ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    load(MobileApi.absolute(image)) {
                        crossfade(true)
                        memoryCacheKey("news_${item.optInt("id")}")
                        diskCacheKey("news_${item.optInt("id")}")
                    }
                },
                LinearLayout.LayoutParams(-1, NativeUi.dp(this, 205))
            )
        }

        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeNewsActivity, 15),
                NativeUi.dp(this@NativeNewsActivity, 14),
                NativeUi.dp(this@NativeNewsActivity, 15),
                NativeUi.dp(this@NativeNewsActivity, 14)
            )
        }

        copy.addView(NativeUi.text(this, item.optString("title"), 20f, NativeUi.ink, true))
        copy.addView(
            NativeUi.text(this, item.optString("date"), 10f, NativeUi.gold, true).apply {
                setPadding(0, NativeUi.dp(this@NativeNewsActivity, 6), 0, NativeUi.dp(this@NativeNewsActivity, 9))
            }
        )

        val stats = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        stats.addView(stat("◉", item.optInt("views").toString()))
        stats.addView(stat("♡", item.optInt("likes").toString()))
        stats.addView(stat("♧", item.optInt("dislikes").toString()))
        stats.addView(LinearLayout(this), LinearLayout.LayoutParams(0, 1, 1f))
        stats.addView(NativeUi.text(this, "Oxu →", 11f, NativeUi.green, true))
        copy.addView(stats)

        card.addView(copy)
        return card
    }

    private fun stat(icon: String, value: String) =
        NativeUi.text(this, "$icon  $value", 11f, NativeUi.muted, true).apply {
            setPadding(0, 0, NativeUi.dp(this@NativeNewsActivity, 13), 0)
        }

    private fun openNews(id: Int) {
        if (id <= 0) return
        currentNewsId = id

        body.removeAllViews()
        body.addView(NativeUi.text(this, "Xəbər yüklənir…", 13f, NativeUi.muted))

        MobileApi.get(
            this,
            "news_detail",
            mapOf("id" to id.toString()),
            cacheKey = "news_$id"
        ) { json, _ ->
            if (json.optBoolean("ok") != true) return@get
            val item = json.optJSONObject("item") ?: return@get
            drawDetail(item)
        }
    }

    private fun drawDetail(item: JSONObject) {
        body.removeAllViews()

        val back = NativeUi.text(this, "‹  Yeniliklər", 12f, NativeUi.green, true).apply {
            setOnClickListener { loadNews() }
        }
        body.addView(back, LinearLayout.LayoutParams(-1, NativeUi.dp(this, 38)))

        val image = item.optString("image")
        if (image.isNotBlank()) {
            body.addView(
                ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = NativeUi.shape(this@NativeNewsActivity, 0xffedf3f0.toInt(), 16)
                    load(MobileApi.absolute(image)) {
                        crossfade(true)
                        memoryCacheKey("news_detail_${item.optInt("id")}")
                        diskCacheKey("news_detail_${item.optInt("id")}")
                    }
                },
                LinearLayout.LayoutParams(-1, NativeUi.dp(this, 235)).apply {
                    bottomMargin = NativeUi.dp(this@NativeNewsActivity, 14)
                }
            )
        }

        body.addView(NativeUi.text(this, item.optString("title"), 28f, NativeUi.ink, true))
        body.addView(
            NativeUi.text(this, item.optString("date"), 10f, NativeUi.gold, true).apply {
                setPadding(0, NativeUi.dp(this@NativeNewsActivity, 7), 0, NativeUi.dp(this@NativeNewsActivity, 13))
            }
        )

        body.addView(
            NativeUi.text(this, item.optString("body"), 16f, NativeUi.ink).apply {
                setLineSpacing(NativeUi.dp(this@NativeNewsActivity, 6).toFloat(), 1.05f)
                setTextIsSelectable(true)
            }
        )

        val actions = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, NativeUi.dp(this@NativeNewsActivity, 18), 0, 0)
        }

        val like = actionButton("♡  ${item.optInt("likes")}") {
            react(item.optInt("id"), 1)
        }
        actions.addView(like, LinearLayout.LayoutParams(0, NativeUi.dp(this, 43), 1f).apply {
            rightMargin = NativeUi.dp(this@NativeNewsActivity, 5)
        })

        val dislike = actionButton("♧  ${item.optInt("dislikes")}") {
            react(item.optInt("id"), -1)
        }
        actions.addView(dislike, LinearLayout.LayoutParams(0, NativeUi.dp(this, 43), 1f).apply {
            leftMargin = NativeUi.dp(this@NativeNewsActivity, 5)
        })

        body.addView(actions)

        body.addView(
            actionButton("Paylaş") {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        item.optString("title") + "\nhttps://alieba.ge/yenilikler/xeber.php?id=" + item.optInt("id")
                    )
                }
                startActivity(Intent.createChooser(share, "Xəbəri paylaş"))
            },
            LinearLayout.LayoutParams(-1, NativeUi.dp(this, 43)).apply {
                topMargin = NativeUi.dp(this@NativeNewsActivity, 8)
            }
        )

        body.addView(
            NativeUi.text(
                this,
                "◉  ${item.optInt("views")} baxış",
                11f,
                NativeUi.muted,
                true
            ).apply {
                setPadding(0, NativeUi.dp(this@NativeNewsActivity, 12), 0, 0)
            }
        )
    }

    private fun actionButton(label: String, action: () -> Unit) =
        NativeUi.text(this, label, 12f, NativeUi.green, true).apply {
            gravity = Gravity.CENTER
            background = NativeUi.shape(this@NativeNewsActivity, NativeUi.soft, 12, 0xffd6e9e0.toInt())
            setOnClickListener { action() }
        }

    private fun react(id: Int, reaction: Int) {
        MobileApi.post(
            this,
            "news_react",
            mapOf("id" to id.toString(), "reaction" to reaction.toString())
        ) {
            openNews(id)
        }
    }
}
