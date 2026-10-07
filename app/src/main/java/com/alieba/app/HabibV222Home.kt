package com.alieba.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HabibV222Home {

    private fun dp(a: Activity, v: Int) =
        (v * a.resources.displayMetrics.density).toInt()

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
        gravity = Gravity.CENTER
        includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun open(activity: Activity, target: String) {
        when (target) {
            "news" -> activity.startActivity(Intent(activity, NativeNewsActivity::class.java))
            "qa" -> activity.startActivity(Intent(activity, NativeQaActivity::class.java))
            "experts" -> activity.startActivity(
                Intent(activity, NativeQaActivity::class.java).putExtra("mode", "experts")
            )
            "profile" -> activity.startActivity(Intent(activity, HabibV222ProfileActivity::class.java))
            "settings" -> activity.startActivity(Intent(activity, NativeSettingsActivity::class.java))
            "donate" -> activity.startActivity(Intent(activity, NativeDonateActivity::class.java))
            "quran" -> activity.startActivity(Intent(activity, HabibV221QuranActivity::class.java))
            "ai" -> activity.startActivity(Intent(activity, NativeAiActivity::class.java))
            else -> activity.startActivity(
                Intent(activity, NativeContentActivity::class.java).putExtra("section", target)
            )
        }
    }

    fun build(activity: Activity): View {
        val prayer = PrayerClock.times(activity).ifEmpty { PrayerClock.displayTimes(activity) }
        val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val sunrise = prayer["sunrise"] ?: "06:00"
        val sunset = prayer["sunset"] ?: "19:00"
        val night = now < sunrise || now >= sunset

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            clipChildren = false
            clipToPadding = false
        }

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            isSaveEnabled = false
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(Color.WHITE)
        }

        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            isSaveEnabled = false
            setBackgroundColor(Color.WHITE)
            clipChildren = false
            clipToPadding = false
        }

        val hero = FrameLayout(activity).apply {
            clipChildren = false
            clipToPadding = false
        }

        // V22.5: ana səhifədə məscid şəkli birbaşa ImageView ilə çəkilir.
        // Bu, bəzi telefonlarda custom View-in boş/ağ qalması problemini aradan qaldırır.
        hero.addView(
            ImageView(activity).apply {
                setImageResource(if (night) R.drawable.mosque_night else R.drawable.mosque_day)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(if (night) 0xff14345e.toInt() else 0xff9bcfee.toInt())
                contentDescription = "Alieba məscid"
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        hero.addView(
            View(activity).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        0x46041b2b,
                        0x05041b2b,
                        0x12000000,
                        0x82000000.toInt()
                    )
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        val top = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(activity, 18), dp(activity, 12), dp(activity, 18), 0)
        }

        val logo = FrameLayout(activity).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff16c6ad.toInt(), 0xff22b7d9.toInt())
            ).apply {
                cornerRadius = dp(activity, 16).toFloat()
                setStroke(dp(activity, 1), 0xccffffff.toInt())
            }
            elevation = dp(activity, 3).toFloat()
        }
        logo.addView(
            ImageView(activity).apply {
                setImageResource(R.drawable.ic_notification_mosque)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10))
            },
            FrameLayout.LayoutParams(-1, -1)
        )
        top.addView(logo, LinearLayout.LayoutParams(dp(activity, 50), dp(activity, 50)))

        val brand = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(activity, 10), 0, 0, 0)
        }
        brand.addView(label(activity, "Alieba", 19f, Color.WHITE, true).apply { gravity = Gravity.START })
        brand.addView(label(activity, "İmanla yaşa", 10f, 0xffedf7f5.toInt()).apply {
            gravity = Gravity.START
            setPadding(0, dp(activity, 2), 0, 0)
        })
        top.addView(brand, LinearLayout.LayoutParams(0, dp(activity, 52), 1f))

        top.addView(
            ImageView(activity).apply {
                setImageResource(R.drawable.ic_profile)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(activity, 11), dp(activity, 11), dp(activity, 11), dp(activity, 11))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0x44132432)
                    setStroke(dp(activity, 1), 0x99ffffff.toInt())
                }
                setOnClickListener { open(activity, "profile") }
            },
            LinearLayout.LayoutParams(dp(activity, 47), dp(activity, 47))
        )
        hero.addView(top, FrameLayout.LayoutParams(-1, dp(activity, 70), Gravity.TOP))

        val titleBox = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(activity, 34), 0, dp(activity, 34), 0)
        }
        titleBox.addView(
            label(
                activity,
                if (night) "Həyatını Allahın rəngi ilə boya" else "Hər gün Allaha daha yaxın",
                23f,
                Color.WHITE
            ).apply {
                typeface = Typeface.create("serif", Typeface.BOLD)
                setLines(2)
                setShadowLayer(dp(activity, 2).toFloat(), 0f, dp(activity, 1).toFloat(), 0xaa00151d.toInt())
            }
        )
        titleBox.addView(
            View(activity).apply { setBackgroundColor(0xd8ffffff.toInt()) },
            LinearLayout.LayoutParams(dp(activity, 68), dp(activity, 1)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(activity, 8)
                bottomMargin = dp(activity, 8)
            }
        )
        titleBox.addView(
            label(
                activity,
                if (night) "Qəlblər Allahı zikr etməklə rahatlıq tapır. • Rəd, 28"
                else "Dua ilə başla, imanla yaşa, xeyirlə davam et.",
                10f,
                0xfff5f8f7.toInt()
            ).apply {
                setLines(2)
                setShadowLayer(dp(activity, 2).toFloat(), 0f, dp(activity, 1).toFloat(), 0xaa00151d.toInt())
            }
        )
        hero.addView(
            titleBox,
            FrameLayout.LayoutParams(-1, dp(activity, 120), Gravity.TOP).apply {
                topMargin = dp(activity, 82)
            }
        )

        hero.addView(
            prayerStrip(activity, prayer),
            FrameLayout.LayoutParams(-1, dp(activity, 70), Gravity.BOTTOM).apply {
                leftMargin = dp(activity, 11)
                rightMargin = dp(activity, 11)
                bottomMargin = dp(activity, 20)
            }
        )
        body.addView(hero, LinearLayout.LayoutParams(-1, dp(activity, 374)))

        // Panel hero-nun üstünə azca gəlir: yuxarı yumru künclərin arxasında məscid şəkli görünür.
        val sheet = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 22))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                setCornerRadii(
                    floatArrayOf(
                        dp(activity, 34).toFloat(), dp(activity, 34).toFloat(),
                        dp(activity, 34).toFloat(), dp(activity, 34).toFloat(),
                        0f, 0f, 0f, 0f
                    )
                )
            }
            elevation = dp(activity, 7).toFloat()
        }
        sheet.addView(
            View(activity).apply {
                background = GradientDrawable().apply {
                    setColor(0xffd8dee0.toInt())
                    cornerRadius = dp(activity, 3).toFloat()
                }
            },
            LinearLayout.LayoutParams(dp(activity, 38), dp(activity, 4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(activity, 15)
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
            Item("Kitabxana", R.drawable.ic_library, 0xff4259dd.toInt(), 0xff6375e9.toInt(), "kitabxana")
        )

        for (rowIndex in 0 until 3) {
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
            }
            for (column in 0 until 3) {
                val item = items[rowIndex * 3 + column]
                val cell = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    setPadding(dp(activity, 5), dp(activity, 5), dp(activity, 5), dp(activity, 5))
                    setOnClickListener { open(activity, item.target) }
                }
                val iconBox = FrameLayout(activity).apply {
                    background = GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(item.c1, item.c2)
                    ).apply {
                        cornerRadius = dp(activity, 18).toFloat()
                        setStroke(dp(activity, 2), 0x55ffffff)
                    }
                    elevation = dp(activity, 2).toFloat()
                }
                iconBox.addView(
                    ImageView(activity).apply {
                        setImageResource(item.icon)
                        setColorFilter(Color.WHITE)
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        setPadding(dp(activity, 13), dp(activity, 13), dp(activity, 13), dp(activity, 13))
                    },
                    FrameLayout.LayoutParams(-1, -1)
                )
                cell.addView(iconBox, LinearLayout.LayoutParams(dp(activity, 58), dp(activity, 58)))
                cell.addView(
                    label(activity, item.title, 10.5f, 0xff2c3034.toInt()).apply {
                        setLines(2)
                        setPadding(0, dp(activity, 7), 0, 0)
                    },
                    LinearLayout.LayoutParams(-1, dp(activity, 33))
                )
                row.addView(
                    cell,
                    LinearLayout.LayoutParams(0, dp(activity, 101), 1f).apply {
                        setMargins(dp(activity, 3), 0, dp(activity, 3), dp(activity, 6))
                    }
                )
            }
            sheet.addView(row, LinearLayout.LayoutParams(-1, dp(activity, 107)))
        }

        body.addView(
            sheet,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = -dp(activity, 14)
            }
        )
        body.addView(View(activity).apply { setBackgroundColor(Color.WHITE) }, LinearLayout.LayoutParams(-1, dp(activity, 12)))

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bottomBar(activity), LinearLayout.LayoutParams(-1, dp(activity, 92)))
        return root
    }

    private fun prayerStrip(activity: Activity, prayer: Map<String, String>): View {
        val active = PrayerClock.activeKey(activity)
        val symbols = listOf("☾", "☀", "☀", "☀", "☾", "☾")
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        for (i in PrayerClock.keys.indices) {
            val key = PrayerClock.keys[i]
            val name = PrayerClock.displayNames[i]
            val value = prayer[key]
            val selected = active == key && value != null
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(activity, 2), 0, dp(activity, 2))
                background = GradientDrawable().apply {
                    cornerRadius = dp(activity, 14).toFloat()
                    setColor(if (selected) 0xddbd8a00.toInt() else 0x99111c28.toInt())
                    setStroke(dp(activity, if (selected) 2 else 1), if (selected) 0xffffd662.toInt() else 0x44ffffff)
                }
                setOnClickListener { open(activity, "settings") }
            }
            card.addView(label(activity, symbols[i], 11f, if (selected) 0xffffe28a.toInt() else Color.WHITE, true), LinearLayout.LayoutParams(-1, dp(activity, 17)))
            card.addView(label(activity, name, 6.6f, Color.WHITE, selected).apply { setLines(2) }, LinearLayout.LayoutParams(-1, dp(activity, 21)))
            card.addView(label(activity, value ?: "...", 10f, Color.WHITE, true), LinearLayout.LayoutParams(-1, dp(activity, 19)))
            row.addView(card, LinearLayout.LayoutParams(0, dp(activity, 62), 1f).apply { setMargins(dp(activity, 2), 0, dp(activity, 2), 0) })
        }
        return row
    }

    private fun bottomBar(activity: Activity): View {
        val frame = FrameLayout(activity).apply {
            background = HabibV222WaveDrawable(
                activity.resources.displayMetrics.density,
                Color.WHITE,
                0xffdbe8e5.toInt()
            )
            elevation = dp(activity, 12).toFloat()
            clipChildren = false
            clipToPadding = false
            setPadding(0, dp(activity, 24), 0, 0)
        }

        fun nav(title: String, icon: Int, color: Int, action: () -> Unit): View =
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener { action() }
                addView(
                    ImageView(activity).apply {
                        setImageResource(icon)
                        setColorFilter(color)
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                    },
                    LinearLayout.LayoutParams(dp(activity, 24), dp(activity, 24))
                )
                addView(
                    label(activity, title, 8.7f, color, title == "Ana səhifə").apply { setPadding(0, dp(activity, 4), 0, 0) },
                    LinearLayout.LayoutParams(-1, dp(activity, 21))
                )
            }

        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(nav("Ana səhifə", R.drawable.ic_home, 0xff18bfa9.toInt()) {}, LinearLayout.LayoutParams(0, -1, 1f))
        row.addView(nav("Kömək et", R.drawable.ic_heart, 0xff34383b.toInt()) { open(activity, "donate") }, LinearLayout.LayoutParams(0, -1, 1f))
        row.addView(View(activity), LinearLayout.LayoutParams(0, -1, 1.10f))
        row.addView(nav("Sevimlilər", R.drawable.ic_heart, 0xff34383b.toInt()) {
            activity.startActivity(Intent(activity, NativeContentActivity::class.java).putExtra("section", "saved"))
        }, LinearLayout.LayoutParams(0, -1, 1f))
        row.addView(nav("Daha çox", R.drawable.ic_settings, 0xff34383b.toInt()) { open(activity, "settings") }, LinearLayout.LayoutParams(0, -1, 1f))

        frame.addView(row, FrameLayout.LayoutParams(-1, dp(activity, 66), Gravity.BOTTOM))

        val center = FrameLayout(activity).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xffff8b60.toInt(), 0xffff7651.toInt())
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(activity, 6), Color.WHITE)
            }
            elevation = dp(activity, 12).toFloat()
            setOnClickListener { open(activity, "ai") }
        }
        center.addView(
            ImageView(activity).apply {
                setImageResource(R.drawable.ic_alieba_ai)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(activity, 15), dp(activity, 15), dp(activity, 15), dp(activity, 15))
            },
            FrameLayout.LayoutParams(-1, -1)
        )
        frame.addView(
            center,
            FrameLayout.LayoutParams(dp(activity, 72), dp(activity, 72), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = 0
            }
        )
        return frame
    }
}
