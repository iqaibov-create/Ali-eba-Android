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
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * V22.6 stable home.
 *
 * This screen intentionally does NOT use ScrollView/custom scene rendering for the
 * main home body. The previous white-screen bug left only the bottom navigation
 * visible on some Samsung devices. Here every visible block is placed directly
 * into a weighted LinearLayout with a normal ImageView hero.
 */
object HabibV226Home {

    private fun dp(a: Activity, v: Int): Int =
        (v * a.resources.displayMetrics.density).toInt()

    private fun text(
        a: Activity,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) = TextView(a).apply {
        this.text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
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
            "tasbeh", "zikr" -> a.startActivity(Intent(a, ZikrActivity::class.java))
            else -> a.startActivity(
                Intent(a, NativeContentActivity::class.java).putExtra("section", target)
            )
        }
    }

    fun build(a: Activity): View {
        val white = Color.WHITE
        val prayer = PrayerClock.times(a).ifEmpty { PrayerClock.displayTimes(a) }
        val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val sunrise = prayer["sunrise"] ?: "06:00"
        val sunset = prayer["sunset"] ?: "19:00"
        val night = now < sunrise || now >= sunset

        val root = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(white)
            clipChildren = false
            clipToPadding = false
        }

        val page = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(white)
            clipChildren = false
            clipToPadding = false
        }

        val screenDp = a.resources.configuration.screenHeightDp
        val heroDp = (screenDp * 0.40f).toInt().coerceIn(286, 336)

        val hero = FrameLayout(a).apply {
            clipChildren = false
            clipToPadding = false
            setBackgroundColor(if (night) 0xff14345e.toInt() else 0xff9bcfee.toInt())
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
                        0x43041b2b,
                        0x06041b2b,
                        0x14000000,
                        0x8a000000.toInt()
                    )
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        val top = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(a, 17), dp(a, 11), dp(a, 17), 0)
        }

        val logo = FrameLayout(a).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff16c6ad.toInt(), 0xff22b7d9.toInt())
            ).apply {
                cornerRadius = dp(a, 15).toFloat()
                setStroke(dp(a, 1), 0xccffffff.toInt())
            }
        }
        logo.addView(
            ImageView(a).apply {
                setImageResource(R.drawable.ic_notification_mosque)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(a, 10), dp(a, 10), dp(a, 10), dp(a, 10))
            },
            FrameLayout.LayoutParams(-1, -1)
        )
        top.addView(logo, LinearLayout.LayoutParams(dp(a, 48), dp(a, 48)))

        val brand = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(a, 10), 0, 0, 0)
        }
        brand.addView(text(a, "Alieba", 18f, Color.WHITE, true).apply { gravity = Gravity.START })
        brand.addView(text(a, "İmanla yaşa", 10f, 0xffeef7f5.toInt()).apply {
            gravity = Gravity.START
            setPadding(0, dp(a, 2), 0, 0)
        })
        top.addView(brand, LinearLayout.LayoutParams(0, dp(a, 50), 1f))

        top.addView(
            ImageView(a).apply {
                setImageResource(R.drawable.ic_profile)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(a, 10), dp(a, 10), dp(a, 10), dp(a, 10))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0x45132432)
                    setStroke(dp(a, 1), 0xaaffffff.toInt())
                }
                setOnClickListener { open(a, "profile") }
            },
            LinearLayout.LayoutParams(dp(a, 46), dp(a, 46))
        )
        hero.addView(top, FrameLayout.LayoutParams(-1, dp(a, 68), Gravity.TOP))

        val middle = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(a, 28), 0, dp(a, 28), 0)
        }
        middle.addView(
            text(
                a,
                if (night) "Həyatını Allahın rəngi ilə boya" else "Hər gün Allaha daha yaxın",
                22f,
                Color.WHITE,
                true
            ).apply {
                typeface = Typeface.create("serif", Typeface.BOLD)
                maxLines = 2
                setShadowLayer(dp(a, 2).toFloat(), 0f, dp(a, 1).toFloat(), 0xaa00151d.toInt())
            }
        )
        middle.addView(
            View(a).apply { setBackgroundColor(0xddffffff.toInt()) },
            LinearLayout.LayoutParams(dp(a, 64), dp(a, 1)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(a, 8)
                bottomMargin = dp(a, 8)
            }
        )
        middle.addView(
            text(
                a,
                if (night) "Qəlblər Allahı zikr etməklə rahatlıq tapır. • Rəd, 28"
                else "Dua ilə başla, imanla yaşa, xeyirlə davam et.",
                9.5f,
                0xfff6f9f8.toInt()
            ).apply {
                maxLines = 2
                setShadowLayer(dp(a, 2).toFloat(), 0f, dp(a, 1).toFloat(), 0xaa00151d.toInt())
            }
        )
        hero.addView(
            middle,
            FrameLayout.LayoutParams(-1, dp(a, 116), Gravity.TOP).apply {
                topMargin = dp(a, 74)
            }
        )

        hero.addView(
            prayerStrip(a, prayer),
            FrameLayout.LayoutParams(-1, dp(a, 66), Gravity.BOTTOM).apply {
                leftMargin = dp(a, 9)
                rightMargin = dp(a, 9)
                bottomMargin = dp(a, 16)
            }
        )

        page.addView(hero, LinearLayout.LayoutParams(-1, dp(a, heroDp)))

        val sheet = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(a, 13), dp(a, 9), dp(a, 13), dp(a, 6))
            setBackgroundColor(Color.TRANSPARENT)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                setCornerRadii(
                    floatArrayOf(
                        dp(a, 31).toFloat(), dp(a, 31).toFloat(),
                        dp(a, 31).toFloat(), dp(a, 31).toFloat(),
                        0f, 0f, 0f, 0f
                    )
                )
            }
            elevation = dp(a, 7).toFloat()
            translationY = -dp(a, 13).toFloat()
            clipChildren = false
            clipToPadding = false
        }

        sheet.addView(
            View(a).apply {
                background = GradientDrawable().apply {
                    setColor(0xffd7dddf.toInt())
                    cornerRadius = dp(a, 3).toFloat()
                }
            },
            LinearLayout.LayoutParams(dp(a, 38), dp(a, 4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(a, 7)
            }
        )

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
            Item("Təsbeh", R.drawable.ic_tasbeh, 0xff8d60cc.toInt(), 0xffb77bdc.toInt(), "tasbeh"),
            Item("Zikr", R.drawable.ic_dua, 0xffbd8a35.toInt(), 0xffdfa94d.toInt(), "zikr")
        )

        val grid = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        for (r in 0 until 4) {
            val row = LinearLayout(a).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (c in 0 until 3) {
                val item = items[r * 3 + c]
                val cell = LinearLayout(a).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(a, 3), dp(a, 2), dp(a, 3), dp(a, 2))
                    setOnClickListener { open(a, item.target) }
                }
                val iconBox = FrameLayout(a).apply {
                    background = GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(item.c1, item.c2)
                    ).apply {
                        cornerRadius = dp(a, 16).toFloat()
                        setStroke(dp(a, 1), 0x55ffffff)
                    }
                    elevation = dp(a, 2).toFloat()
                }
                iconBox.addView(
                    ImageView(a).apply {
                        setImageResource(item.icon)
                        setColorFilter(Color.WHITE)
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        setPadding(dp(a, 11), dp(a, 11), dp(a, 11), dp(a, 11))
                    },
                    FrameLayout.LayoutParams(-1, -1)
                )
                cell.addView(iconBox, LinearLayout.LayoutParams(dp(a, 45), dp(a, 45)))
                cell.addView(
                    text(a, item.title, 8.8f, 0xff2e3236.toInt()).apply {
                        maxLines = 2
                        setPadding(0, dp(a, 5), 0, 0)
                    },
                    LinearLayout.LayoutParams(-1, dp(a, 20))
                )
                row.addView(cell, LinearLayout.LayoutParams(0, -1, 1f))
            }
            grid.addView(row, LinearLayout.LayoutParams(-1, 0, 1f))
        }

        sheet.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(sheet, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(page, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(AliebaBottomNav.make(a, "home"), LinearLayout.LayoutParams(-1, dp(a, 88)))
        return root
    }

    private fun prayerStrip(a: Activity, prayer: Map<String, String>): View {
        val active = PrayerClock.activeKey(a)
        val symbols = listOf("☾", "☀", "☀", "☀", "☾", "☾")
        val row = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        for (i in PrayerClock.keys.indices) {
            val key = PrayerClock.keys[i]
            val name = PrayerClock.displayNames[i]
            val value = prayer[key] ?: "--:--"
            val selected = active == key && value != "--:--"
            val card = LinearLayout(a).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(a, 1), 0, dp(a, 1))
                background = GradientDrawable().apply {
                    cornerRadius = dp(a, 13).toFloat()
                    setColor(if (selected) 0xddbd8a00.toInt() else 0x99111c28.toInt())
                    setStroke(
                        dp(a, if (selected) 2 else 1),
                        if (selected) 0xffffd662.toInt() else 0x55ffffff
                    )
                }
                setOnClickListener { open(a, "settings") }
            }
            card.addView(
                text(a, symbols[i], 10f, if (selected) 0xffffe28a.toInt() else Color.WHITE, true),
                LinearLayout.LayoutParams(-1, dp(a, 15))
            )
            card.addView(
                text(a, name, 6.2f, Color.WHITE, selected).apply { maxLines = 2 },
                LinearLayout.LayoutParams(-1, dp(a, 20))
            )
            card.addView(
                text(a, value, 9.5f, Color.WHITE, true),
                LinearLayout.LayoutParams(-1, dp(a, 18))
            )
            row.addView(
                card,
                LinearLayout.LayoutParams(0, dp(a, 54), 1f).apply {
                    setMargins(dp(a, 2), 0, dp(a, 2), 0)
                }
            )
        }
        return row
    }
}
