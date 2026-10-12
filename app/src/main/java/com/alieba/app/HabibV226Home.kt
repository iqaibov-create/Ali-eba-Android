package com.alieba.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HabibV226Home {
    private fun dp(a: Activity, n: Int) = (n * a.resources.displayMetrics.density).toInt()

    private val ink = 0xff1e3440.toInt()
    private val softText = 0xffdce8ee.toInt()
    private val gold = 0xffd3a839.toInt()

    private fun label(
        a: Activity,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) = TextView(a).apply {
        text = value
        textSize = size
        setTextColor(color)
        includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun round(a: Activity, color: Int, radius: Int, stroke: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(a, radius).toFloat()
            if (stroke != null) setStroke(dp(a, 1), stroke)
        }

    private fun open(a: Activity, target: String) {
        when (target) {
            "quran" -> a.startActivity(Intent(a, HabibV221QuranActivity::class.java))
            "news" -> a.startActivity(Intent(a, NativeNewsActivity::class.java))
            "qa" -> a.startActivity(Intent(a, NativeQaActivity::class.java))
            "experts" -> a.startActivity(
                Intent(a, NativeQaActivity::class.java).putExtra("mode", "experts")
            )
            "profile" -> a.startActivity(Intent(a, HabibV222ProfileActivity::class.java))
            "settings" -> a.startActivity(Intent(a, NativeSettingsActivity::class.java))
            "qibla" -> a.startActivity(Intent(a, QiblaActivity::class.java))
            "tasbeh" -> a.startActivity(Intent(a, ZikrActivity::class.java))
            else -> a.startActivity(
                Intent(a, NativeContentActivity::class.java).putExtra("section", target)
            )
        }
    }

    fun build(a: Activity): View {
        val prayer = PrayerClock.times(a).ifEmpty { PrayerClock.displayTimes(a) }
        val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val night = now < (prayer["sunrise"] ?: "06:00") ||
            now >= (prayer["sunset"] ?: "19:00")

        val root = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        val scroll = ScrollView(a).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            setBackgroundColor(Color.WHITE)
        }
        val page = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val heroHeight = dp(a, 300)
        val hero = FrameLayout(a).apply {
            clipChildren = true
        }
        hero.addView(
            ImageView(a).apply {
                setImageResource(if (night) R.drawable.mosque_night else R.drawable.mosque_day)
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = "Alieba məscid"
            },
            FrameLayout.LayoutParams(-1, -1)
        )
        hero.addView(
            View(a).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        0x22000000,
                        0x12000000,
                        0x28000000,
                        0x90000000.toInt()
                    )
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        val header = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(a, 14), dp(a, 16), dp(a, 14), 0)
        }
        val logoBox = FrameLayout(a).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff4f8fff.toInt(), 0xff2e61e5.toInt())
            ).apply {
                cornerRadius = dp(a, 12).toFloat()
                setStroke(dp(a, 1), 0x66ffffff)
            }
        }
        logoBox.addView(ImageView(a).apply {
            setImageResource(R.drawable.ic_notification_mosque)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(a, 10), dp(a, 10), dp(a, 10), dp(a, 10))
        }, FrameLayout.LayoutParams(-1, -1))
        header.addView(logoBox, LinearLayout.LayoutParams(dp(a, 48), dp(a, 48)))

        val brand = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(a, 10), 0, 0, 0)
        }
        brand.addView(label(a, "Ali-eba", 18f, Color.WHITE, true).apply { gravity = Gravity.START })
        brand.addView(label(a, "İmanla yaşa...", 10f, softText).apply { gravity = Gravity.START })
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(ImageView(a).apply {
            setImageResource(R.drawable.ic_profile)
            setColorFilter(Color.WHITE)
            background = round(a, 0x44000000, 22, 0x66ffffff)
            setPadding(dp(a, 10), dp(a, 10), dp(a, 10), dp(a, 10))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setOnClickListener { open(a, "profile") }
        }, LinearLayout.LayoutParams(dp(a, 44), dp(a, 44)))
        hero.addView(header, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))

        val titleWrap = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(a, 24), 0, dp(a, 24), 0)
        }
        titleWrap.addView(label(
            a,
            if (night) "Həyatını\nAllahın rəngi ilə boya" else "Hər gün\nAllaha daha yaxın",
            19f,
            0xff102534.toInt(),
            true
        ).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.create("serif", Typeface.NORMAL)
        })
        titleWrap.addView(label(
            a,
            if (night) "“Həqiqətən, Allah qəlbləri zikrlə rahatladır.”" else "“Allah zikr edənləri sevər.”",
            10.5f,
            0xff203848.toInt()
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(a, 8), 0, 0)
        })
        hero.addView(titleWrap, FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply {
            topMargin = dp(a, 62)
        })

        val dateCard = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(a, 12), dp(a, 10), dp(a, 12), dp(a, 10))
            background = round(a, 0xaa1f2328.toInt(), 14)
            elevation = dp(a, 4).toFloat()
        }
        val dateText = try {
            SimpleDateFormat("dd MMMM, EEEE", Locale("az")).format(Date())
        } catch (_: Exception) {
            SimpleDateFormat("dd MMMM, EEEE", Locale.getDefault()).format(Date())
        }
        dateCard.addView(label(a, dateText, 10.5f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
        })
        dateCard.addView(label(a, "Bu gün", 10f, 0xffd8e1e8.toInt()).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(a, 4), 0, 0)
        })
        hero.addView(dateCard, FrameLayout.LayoutParams(dp(a, 124), -2, Gravity.END or Gravity.BOTTOM).apply {
            rightMargin = dp(a, 12)
            bottomMargin = dp(a, 78)
        })

        hero.addView(prayerStrip(a, prayer), FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            bottomMargin = dp(a, 12)
        })
        page.addView(hero, LinearLayout.LayoutParams(-1, heroHeight))

        val sheet = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            background = round(a, Color.WHITE, 24)
            elevation = dp(a, 4).toFloat()
            setPadding(dp(a, 14), dp(a, 14), dp(a, 14), dp(a, 18))
        }
        sheet.addView(label(a, "Bölmələr", 15f, ink, true).apply {
            gravity = Gravity.START
            setPadding(dp(a, 6), 0, 0, dp(a, 6))
        })

        data class Item(val title: String, val icon: Int, val c1: Int, val c2: Int, val target: String)
        val items = listOf(
            Item("Quran", R.drawable.ic_quran, 0xff18c5aa.toInt(), 0xff35d7ba.toInt(), "quran"),
            Item("Məfatih", R.drawable.ic_dua, 0xffff8a64.toInt(), 0xffffa479.toInt(), "mafatih"),
            Item("Əhkam", R.drawable.ic_rules, 0xff1cb7e6.toInt(), 0xff33c7ef.toInt(), "ahkam"),
            Item("Görüş", R.drawable.ic_profile, 0xff7d53e8.toInt(), 0xff9b72ef.toInt(), "experts"),
            Item("Təqvim", R.drawable.ic_calendar, 0xffff58a0.toInt(), 0xffff76b3.toInt(), "news"),
            Item("Hüseyniyyə", R.drawable.ic_audio, 0xffe0333a.toInt(), 0xfff04d57.toInt(), "mersiye"),
            Item("Hədislər", R.drawable.ic_hadith, 0xff2bb84b.toInt(), 0xff49cf69.toInt(), "hadis"),
            Item("Məsləhət", R.drawable.ic_chat, 0xffffc126.toInt(), 0xffffd54d.toInt(), "qa"),
            Item("Kitabxana", R.drawable.ic_library, 0xff3550dc.toInt(), 0xff5870ea.toInt(), "kitabxana"),
            Item("Kompas", R.drawable.ic_qibla, 0xff15997f.toInt(), 0xff29b295.toInt(), "qibla"),
            Item("Təsbeh", R.drawable.ic_tasbeh, 0xff8e62cf.toInt(), 0xffac7be0.toInt(), "tasbeh")
        )

        val grid = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
        }
        val rows = (items.size + 2) / 3
        for (r in 0 until rows) {
            val row = LinearLayout(a).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (c in 0 until 3) {
                val idx = r * 3 + c
                if (idx >= items.size) {
                    row.addView(View(a), LinearLayout.LayoutParams(0, dp(a, 92), 1f))
                    continue
                }
                val item = items[idx]
                val cell = LinearLayout(a).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(a, 2), dp(a, 4), dp(a, 2), dp(a, 6))
                    setOnClickListener { open(a, item.target) }
                }
                val iconBox = FrameLayout(a).apply {
                    background = GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(item.c1, item.c2)
                    ).apply {
                        cornerRadius = dp(a, 14).toFloat()
                    }
                    elevation = dp(a, 2).toFloat()
                }
                iconBox.addView(ImageView(a).apply {
                    setImageResource(item.icon)
                    setColorFilter(Color.WHITE)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(a, 11), dp(a, 11), dp(a, 11), dp(a, 11))
                }, FrameLayout.LayoutParams(-1, -1))
                cell.addView(iconBox, LinearLayout.LayoutParams(dp(a, 46), dp(a, 46)))
                cell.addView(label(a, item.title, 10.5f, ink).apply {
                    gravity = Gravity.CENTER
                    maxLines = 1
                    setPadding(0, dp(a, 5), 0, 0)
                })
                row.addView(cell, LinearLayout.LayoutParams(0, dp(a, 82), 1f))
            }
            grid.addView(row, LinearLayout.LayoutParams(-1, -2))
        }

        sheet.addView(grid, LinearLayout.LayoutParams(-1, -2))
        val wrap = FrameLayout(a).apply {
            setPadding(0, dp(a, 8), 0, dp(a, 14))
            addView(sheet, FrameLayout.LayoutParams(-1, -2))
        }
        page.addView(wrap, LinearLayout.LayoutParams(-1, -2))

        scroll.addView(page, FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(AliebaBottomNav.make(a, "home"), LinearLayout.LayoutParams(-1, dp(a, 88)))
        return root
    }

    private fun prayerStrip(a: Activity, prayer: Map<String, String>): View {
        val selected = PrayerClock.activeKey(a)
        val icons = listOf("☾", "☀", "☀", "☀", "☾", "☾")

        val row = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(a, 10), 0, dp(a, 10), 0)
        }
        for (i in PrayerClock.keys.indices) {
            val key = PrayerClock.keys[i]
            val active = key == selected
            val card = LinearLayout(a).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(a, 8), dp(a, 7), dp(a, 8), dp(a, 7))
                background = round(
                    a,
                    if (active) 0xccd6a93e.toInt() else 0xaa2a2629.toInt(),
                    12,
                    if (active) 0xfff0d37a.toInt() else 0x22ffffff
                )
                setOnClickListener { open(a, "settings") }
            }
            card.addView(label(
                a,
                PrayerClock.displayNames.getOrElse(i) { "" },
                10f,
                Color.WHITE,
                active
            ).apply { gravity = Gravity.CENTER })
            card.addView(label(
                a,
                icons.getOrElse(i) { "☾" },
                10f,
                if (active) 0xfffff1b8.toInt() else 0xfff1f5f9.toInt()
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(a, 2), 0, 0)
            })
            card.addView(label(
                a,
                prayer[key] ?: "--:--",
                13f,
                Color.WHITE,
                true
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(a, 2), 0, 0)
            })
            row.addView(card, LinearLayout.LayoutParams(dp(a, 72), dp(a, 74)).apply {
                rightMargin = dp(a, 6)
            })
        }

        return HorizontalScrollView(a).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(row, FrameLayout.LayoutParams(-2, -2))
        }
    }
}
