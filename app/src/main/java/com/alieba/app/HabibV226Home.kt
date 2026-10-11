package com.alieba.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * V22.10: A portrait mosque photo must not be FIT_CENTER'ed into a full-width banner.
 * Portrait photo is on the right, text has its own space on the left.
 * Prayer times are below (never over) the photo. Bottom nav stays outside the scroll.
 */
object HabibV226Home {
    private fun dp(a: Activity, n: Int) = (n * a.resources.displayMetrics.density).toInt()

    private val ink = 0xff203a39.toInt()
    private val green = 0xff0e695f.toInt()

    private fun label(a: Activity, value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(a).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
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
            "experts" -> a.startActivity(Intent(a, NativeQaActivity::class.java).putExtra("mode", "experts"))
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
        val screenDp = a.resources.configuration.screenHeightDp
        val widthDp = a.resources.configuration.screenWidthDp
        val heroDp = (screenDp * 0.33f).toInt().coerceIn(218, 265)

        // Full portrait mosque photo remains readable. No artificial blue side-bars.
        val hero = FrameLayout(a).apply {
            setBackgroundColor(if (night) 0xff183948.toInt() else 0xff1d5d6a.toInt())
            clipChildren = true
        }
        val imageWidthDp = (heroDp * 341f / 512f).toInt()
        hero.addView(
            ImageView(a).apply {
                setImageResource(if (night) R.drawable.mosque_night else R.drawable.mosque_day)
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = "Alieba məscid"
            },
            FrameLayout.LayoutParams(dp(a, imageWidthDp), -1, Gravity.RIGHT)
        )
        hero.addView(
            View(a).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(
                        0xff123842.toInt(), 0xff194f5b.toInt(),
                        0x80255863.toInt(), 0x00173948
                    )
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        val header = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(a, 16), dp(a, 12), dp(a, 16), 0)
        }
        val logo = FrameLayout(a).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff19b9a0.toInt(), 0xff21b7d3.toInt())
            ).apply {
                cornerRadius = dp(a, 13).toFloat()
                setStroke(dp(a, 1), 0x88ffffff.toInt())
            }
        }
        logo.addView(ImageView(a).apply {
            setImageResource(R.drawable.ic_notification_mosque)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(a, 10), dp(a, 10), dp(a, 10), dp(a, 10))
        }, FrameLayout.LayoutParams(-1, -1))
        header.addView(logo, LinearLayout.LayoutParams(dp(a, 43), dp(a, 43)))
        val brand = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(a, 9), 0, 0, 0)
        }
        brand.addView(label(a, "Alieba", 18f, Color.WHITE, true).apply {
            gravity = Gravity.START
        })
        brand.addView(label(a, "İmanla yaşa", 10f, 0xffe1f0ef.toInt()).apply {
            gravity = Gravity.START
        })
        header.addView(brand, LinearLayout.LayoutParams(0, dp(a, 45), 1f))
        header.addView(ImageView(a).apply {
            setImageResource(R.drawable.ic_profile)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(a, 10), dp(a, 10), dp(a, 10), dp(a, 10))
            background = round(a, 0xaa133745.toInt(), 25, 0xaaffffff.toInt())
            setOnClickListener { open(a, "profile") }
            contentDescription = "Profil"
        }, LinearLayout.LayoutParams(dp(a, 43), dp(a, 43)))
        hero.addView(header, FrameLayout.LayoutParams(-1, dp(a, 66), Gravity.TOP))

        val slogan = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(a, 2), 0, dp(a, 1), 0)
        }
        slogan.addView(label(a,
            if (night) "Qəlbin nurla dolsun" else "Hər gün Allaha\nbir addım yaxın",
            18f, Color.WHITE, true
        ).apply {
            gravity = Gravity.START
            maxLines = 3
            typeface = Typeface.create("serif", Typeface.BOLD)
        })
        slogan.addView(label(a, "Dua ilə başla,\nimanla davam et.", 11f,
            0xffd4e7e6.toInt()).apply {
            gravity = Gravity.START
            setPadding(0, dp(a, 9), 0, 0)
        })
        hero.addView(slogan, FrameLayout.LayoutParams(
            dp(a, (widthDp * 0.46f).toInt().coerceAtMost(195)), dp(a, 115),
            Gravity.LEFT or Gravity.CENTER_VERTICAL
        ).apply { leftMargin = dp(a, 18); topMargin = dp(a, 35) })
        page.addView(hero, LinearLayout.LayoutParams(-1, dp(a, heroDp)))

        // Prayer information is outside of hero so the mosque remains visible.
        val prayerArea = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(a, 12), dp(a, 12), dp(a, 12), dp(a, 12))
            setBackgroundColor(0xffedf7f4.toInt())
        }
        prayerArea.addView(label(a, "Namaz vaxtları", 13.5f, green, true).apply {
            gravity = Gravity.START
            setPadding(dp(a, 4), 0, 0, dp(a, 5))
        })
        prayerArea.addView(prayerGrid(a, prayer), LinearLayout.LayoutParams(-1, -2))
        page.addView(prayerArea, LinearLayout.LayoutParams(-1, -2))

        val sheet = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(a, 13), dp(a, 14), dp(a, 13), dp(a, 20))
            setBackgroundColor(Color.WHITE)
        }
        sheet.addView(label(a, "Bölmələr", 14f, ink, true).apply {
            gravity = Gravity.START
            setPadding(dp(a, 6), 0, 0, dp(a, 8))
        })

        data class Item(val title: String, val icon: Int, val c1: Int, val c2: Int, val target: String)
        val items = listOf(
            Item("Quran", R.drawable.ic_quran, 0xff0bc7a3.toInt(), 0xff26d4b5.toInt(), "quran"),
            Item("Məfatih", R.drawable.ic_dua, 0xffff8260.toInt(), 0xffff9d71.toInt(), "mafatih"),
            Item("Əhkam", R.drawable.ic_rules, 0xff10b1df.toInt(), 0xff2bc5e9.toInt(), "ahkam"),
            Item("Yeniliklər", R.drawable.ic_calendar, 0xff6c4ee8.toInt(), 0xff8c62ef.toInt(), "news"),
            Item("Dini sual", R.drawable.ic_chat, 0xffff4f7b.toInt(), 0xffff7899.toInt(), "qa"),
            Item("Mərsiyələr", R.drawable.ic_audio, 0xffc72f38.toInt(), 0xffe14a53.toInt(), "mersiye"),
            Item("Hədislər", R.drawable.ic_hadith, 0xff24b949.toInt(), 0xff42cf63.toInt(), "hadis"),
            Item("Mütəxəssis", R.drawable.ic_profile, 0xffffb51f.toInt(), 0xffffc950.toInt(), "experts"),
            Item("Kitabxana", R.drawable.ic_library, 0xff4259dd.toInt(), 0xff6375e9.toInt(), "kitabxana"),
            Item("Kompas", R.drawable.ic_qibla, 0xff168a76.toInt(), 0xff2aa992.toInt(), "qibla"),
            Item("Təsbeh", R.drawable.ic_tasbeh, 0xff8d60cc.toInt(), 0xffb77bdc.toInt(), "tasbeh")
        )
        val grid = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        for (r in 0 until (items.size + 2) / 3) {
            val row = LinearLayout(a).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            for (c in 0 until 3) {
                val idx = r * 3 + c
                if (idx >= items.size) {
                    row.addView(View(a), LinearLayout.LayoutParams(0, dp(a, 80), 1f))
                    continue
                }
                val item = items[idx]
                val cell = LinearLayout(a).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(a, 2), dp(a, 4), dp(a, 2), dp(a, 2))
                    setOnClickListener { open(a, item.target) }
                    contentDescription = item.title
                }
                val iconBox = FrameLayout(a).apply {
                    background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        intArrayOf(item.c1, item.c2)).apply {
                        cornerRadius = dp(a, 14).toFloat()
                    }
                }
                iconBox.addView(ImageView(a).apply {
                    setImageResource(item.icon)
                    setColorFilter(Color.WHITE)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(a, 11), dp(a, 11), dp(a, 11), dp(a, 11))
                }, FrameLayout.LayoutParams(-1, -1))
                cell.addView(iconBox, LinearLayout.LayoutParams(dp(a, 48), dp(a, 48)))
                cell.addView(label(a, item.title, 11.5f, ink).apply {
                    maxLines = 2
                    setPadding(0, dp(a, 5), 0, 0)
                }, LinearLayout.LayoutParams(-1, dp(a, 26)))
                row.addView(cell, LinearLayout.LayoutParams(0, dp(a, 90), 1f))
            }
            grid.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        sheet.addView(grid, LinearLayout.LayoutParams(-1, -2))
        page.addView(sheet, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(page, FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(AliebaBottomNav.make(a, "home"), LinearLayout.LayoutParams(-1, dp(a, 88)))
        return root
    }

    private fun prayerGrid(a: Activity, prayer: Map<String, String>): View {
        val selectedKey = PrayerClock.activeKey(a)
        val symbols = listOf("☾", "☀", "☀", "☀", "☾", "☾")
        val outer = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        for (r in 0 until (PrayerClock.keys.size + 2) / 3) {
            val row = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0 until 3) {
                val i = r * 3 + c
                if (i >= PrayerClock.keys.size) {
                    row.addView(View(a), LinearLayout.LayoutParams(0, dp(a, 65), 1f))
                    continue
                }
                val key = PrayerClock.keys[i]
                val value = prayer[key] ?: "--:--"
                val selected = key == selectedKey && value != "--:--"
                val card = LinearLayout(a).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(a, 3), dp(a, 2), dp(a, 3), dp(a, 2))
                    background = round(a,
                        if (selected) 0xfffff0c5.toInt() else Color.WHITE,
                        12, if (selected) 0xffdca72b.toInt() else 0xffd3e8e2.toInt())
                    setOnClickListener { open(a, "settings") }
                }
                card.addView(label(a, symbols.getOrElse(i) { "☾" } + "  " +
                    PrayerClock.displayNames.getOrElse(i) { "" },
                    10f, if (selected) 0xff76521c.toInt() else green, selected).apply {
                    maxLines = 2
                }, LinearLayout.LayoutParams(-1, dp(a, 25)))
                card.addView(label(a, value, 15f,
                    if (selected) 0xff8b6015.toInt() else ink, true),
                    LinearLayout.LayoutParams(-1, dp(a, 24)))
                row.addView(card, LinearLayout.LayoutParams(0, dp(a, 62), 1f).apply {
                    setMargins(dp(a, 3), dp(a, 3), dp(a, 3), dp(a, 3))
                })
            }
            outer.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        return outer
    }
}
