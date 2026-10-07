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

/**
 * Alieba V21.9
 * HabibApp-style ana səhifə.
 *
 * Vacib:
 * - Alieba-nın öz mosque_day.jpg / mosque_night.jpg şəkillərindən istifadə edir.
 * - Tarix bloku yoxdur.
 * - Namaz vaxtları hero üstündədir.
 * - 3x3 modul ikon paneli var.
 */
object HabibV219Home {

    private fun dp(a: Activity, v: Int) =
        (v * a.resources.displayMetrics.density).toInt()

    private fun text(
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

    private fun blend(a: Int, b: Int, f: Float): Int = Color.rgb(
        (Color.red(a) * (1f - f) + Color.red(b) * f).toInt(),
        (Color.green(a) * (1f - f) + Color.green(b) * f).toInt(),
        (Color.blue(a) * (1f - f) + Color.blue(b) * f).toInt()
    )

    private fun open(activity: Activity, target: String) {
        when (target) {
            "news" -> activity.startActivity(Intent(activity, NativeNewsActivity::class.java))
            "qa" -> activity.startActivity(Intent(activity, NativeQaActivity::class.java))
            "experts" -> activity.startActivity(
                Intent(activity, NativeQaActivity::class.java).putExtra("mode", "experts")
            )
            "profile" -> activity.startActivity(Intent(activity, NativeProfileActivity::class.java))
            "settings" -> activity.startActivity(Intent(activity, NativeSettingsActivity::class.java))
            "donate" -> activity.startActivity(Intent(activity, NativeDonateActivity::class.java))
            "saved" -> activity.startActivity(
                Intent(activity, NativeContentActivity::class.java).putExtra("section", "saved")
            )
            else -> activity.startActivity(
                Intent(activity, NativeContentActivity::class.java).putExtra("section", target)
            )
        }
    }

    fun build(activity: Activity): View {
        val prayer = PrayerClock.times(activity).ifEmpty {
            PrayerClock.displayTimes(activity)
        }

        val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val sunrise = prayer["sunrise"] ?: "06:00"
        val sunset = prayer["sunset"] ?: "19:00"
        val night = now < sunrise || now >= sunset

        val root = FrameLayout(activity).apply {
            setBackgroundColor(0xfff7fbfa.toInt())
        }

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(activity, 92))
        }

        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xfff7fbfa.toInt())
        }

        val heroHeight = (activity.resources.displayMetrics.heightPixels * .405f)
            .toInt()
            .coerceIn(dp(activity, 345), dp(activity, 445))

        val hero = FrameLayout(activity).apply {
            clipChildren = false
            clipToPadding = false
        }

        // Mövcud APK-dakı öz məscid şəkilləri.
        hero.addView(
            MosqueSceneView(activity, night),
            FrameLayout.LayoutParams(-1, -1)
        )

        hero.addView(
            View(activity).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        0x50041b2b,
                        0x10041b2b,
                        0x22000000,
                        0x93000000.toInt()
                    )
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        val top = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(activity, 18), dp(activity, 15), dp(activity, 18), 0)
        }

        val logo = FrameLayout(activity).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff15c9af.toInt(), 0xff25aee3.toInt())
            ).apply {
                cornerRadius = dp(activity, 16).toFloat()
                setStroke(dp(activity, 1), 0xccffffff.toInt())
            }
            elevation = dp(activity, 4).toFloat()
        }
        logo.addView(
            ImageView(activity).apply {
                setImageResource(R.drawable.ic_notification_mosque)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(
                    dp(activity, 11),
                    dp(activity, 11),
                    dp(activity, 11),
                    dp(activity, 11)
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )
        top.addView(logo, LinearLayout.LayoutParams(dp(activity, 54), dp(activity, 54)))

        val brand = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(activity, 10), 0, 0, 0)
        }
        brand.addView(
            text(activity, "Alieba", 20f, Color.WHITE, true).apply {
                gravity = Gravity.START
            }
        )
        brand.addView(
            text(activity, "İmanla yaşa", 10.5f, 0xffeef7f5.toInt()).apply {
                gravity = Gravity.START
                setPadding(0, dp(activity, 3), 0, 0)
            }
        )
        top.addView(brand, LinearLayout.LayoutParams(0, dp(activity, 56), 1f))

        top.addView(
            ImageView(activity).apply {
                setImageResource(R.drawable.ic_profile)
                setColorFilter(Color.WHITE)
                setPadding(
                    dp(activity, 12),
                    dp(activity, 12),
                    dp(activity, 12),
                    dp(activity, 12)
                )
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0x55142637)
                    setStroke(dp(activity, 1), 0x99ffffff.toInt())
                }
                setOnClickListener { open(activity, "profile") }
            },
            LinearLayout.LayoutParams(dp(activity, 50), dp(activity, 50))
        )

        hero.addView(top, FrameLayout.LayoutParams(-1, dp(activity, 78), Gravity.TOP))

        val center = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(activity, 32), 0, dp(activity, 32), 0)
        }

        center.addView(
            text(
                activity,
                if (night) "Həyatını Allahın rəngi ilə boya"
                else "Hər gün Allaha daha yaxın",
                26f,
                Color.WHITE
            ).apply {
                typeface = Typeface.create("serif", Typeface.BOLD)
                setLines(2)
                setShadowLayer(
                    dp(activity, 3).toFloat(),
                    0f,
                    dp(activity, 1).toFloat(),
                    0xaa00151d.toInt()
                )
            }
        )

        center.addView(
            View(activity).apply {
                setBackgroundColor(0xd8ffffff.toInt())
            },
            LinearLayout.LayoutParams(dp(activity, 75), dp(activity, 1)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(activity, 11)
                bottomMargin = dp(activity, 10)
            }
        )

        center.addView(
            text(
                activity,
                if (night)
                    "Qəlblər Allahı zikr etməklə rahatlıq tapır. • Rəd, 28"
                else
                    "Dua ilə başla, imanla yaşa, xeyirlə davam et.",
                10.5f,
                0xfff5f7f7.toInt()
            ).apply {
                setLines(2)
                setShadowLayer(
                    dp(activity, 2).toFloat(),
                    0f,
                    dp(activity, 1).toFloat(),
                    0xaa00151d.toInt()
                )
            }
        )

        hero.addView(
            center,
            FrameLayout.LayoutParams(-1, dp(activity, 138), Gravity.CENTER).apply {
                bottomMargin = dp(activity, 62)
            }
        )

        hero.addView(
            prayerStrip(activity, prayer),
            FrameLayout.LayoutParams(-1, dp(activity, 83), Gravity.BOTTOM).apply {
                leftMargin = dp(activity, 12)
                rightMargin = dp(activity, 12)
                bottomMargin = dp(activity, 24)
            }
        )

        body.addView(hero, LinearLayout.LayoutParams(-1, heroHeight))

        val sheet = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(activity, 17),
                dp(activity, 14),
                dp(activity, 17),
                dp(activity, 25)
            )
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                setCornerRadii(
                    floatArrayOf(
                        dp(activity, 35).toFloat(), dp(activity, 35).toFloat(),
                        dp(activity, 35).toFloat(), dp(activity, 35).toFloat(),
                        0f, 0f, 0f, 0f
                    )
                )
            }
            elevation = dp(activity, 8).toFloat()
        }

        sheet.addView(
            View(activity).apply {
                background = GradientDrawable().apply {
                    setColor(0xffdfe4e6.toInt())
                    cornerRadius = dp(activity, 4).toFloat()
                }
            },
            LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 5)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(activity, 18)
            }
        )

        data class Item(
            val title: String,
            val icon: Int,
            val c1: Int,
            val c2: Int,
            val target: String
        )

        val items = listOf(
            Item("Quran", R.drawable.ic_quran, 0xff08c59e.toInt(), 0xff19d0af.toInt(), "quran"),
            Item("Məfatih", R.drawable.ic_dua, 0xffff825b.toInt(), 0xffffa06f.toInt(), "mafatih"),
            Item("Əhkam", R.drawable.ic_rules, 0xff0dafe1.toInt(), 0xff2bc2e9.toInt(), "ahkam"),
            Item("Yeniliklər", R.drawable.ic_calendar, 0xff6750e8.toInt(), 0xff895bef.toInt(), "news"),
            Item("Dini sual", R.drawable.ic_chat, 0xffff4e7b.toInt(), 0xffff7497.toInt(), "qa"),
            Item("Mərsiyələr", R.drawable.ic_audio, 0xffc52c34.toInt(), 0xffe54850.toInt(), "mersiye"),
            Item("Hədislər", R.drawable.ic_hadith, 0xff22b749.toInt(), 0xff43cc61.toInt(), "hadis"),
            Item("Mütəxəssis", R.drawable.ic_profile, 0xffffb21d.toInt(), 0xffffc44f.toInt(), "experts"),
            Item("Kitabxana", R.drawable.ic_library, 0xff4055da.toInt(), 0xff6273e8.toInt(), "kitabxana")
        )

        val grid = android.widget.GridLayout(activity).apply {
            columnCount = 3
            rowCount = 3
            alignmentMode = android.widget.GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }

        items.forEachIndexed { index, item ->
            val cell = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                setPadding(
                    dp(activity, 5),
                    dp(activity, 5),
                    dp(activity, 5),
                    dp(activity, 8)
                )
                setOnClickListener { open(activity, item.target) }
            }

            val box = FrameLayout(activity).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(item.c1, item.c2)
                ).apply {
                    cornerRadius = dp(activity, 19).toFloat()
                    setStroke(
                        dp(activity, 2),
                        blend(item.c2, Color.WHITE, .34f)
                    )
                }
                elevation = dp(activity, 3).toFloat()
            }

            box.addView(
                ImageView(activity).apply {
                    setImageResource(item.icon)
                    setColorFilter(Color.WHITE)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(
                        dp(activity, 15),
                        dp(activity, 15),
                        dp(activity, 15),
                        dp(activity, 15)
                    )
                },
                FrameLayout.LayoutParams(-1, -1)
            )

            cell.addView(
                box,
                LinearLayout.LayoutParams(dp(activity, 68), dp(activity, 68))
            )

            cell.addView(
                text(activity, item.title, 11.3f, 0xff292b30.toInt()).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    setPadding(0, dp(activity, 7), 0, 0)
                    setLines(2)
                },
                LinearLayout.LayoutParams(-1, dp(activity, 38))
            )

            grid.addView(
                cell,
                android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(activity, 118)
                    columnSpec = android.widget.GridLayout.spec(index % 3, 1f)
                    rowSpec = android.widget.GridLayout.spec(index / 3)
                    setMargins(
                        dp(activity, 4),
                        dp(activity, 4),
                        dp(activity, 4),
                        dp(activity, 5)
                    )
                }
            )
        }

        sheet.addView(grid, LinearLayout.LayoutParams(-1, -2))
        body.addView(
            sheet,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = -dp(activity, 24)
            }
        )

        scroll.addView(body)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        root.addView(
            bottomNav(activity),
            FrameLayout.LayoutParams(-1, dp(activity, 88), Gravity.BOTTOM)
        )

        return root
    }

    private fun prayerStrip(
        activity: Activity,
        prayer: Map<String, String>
    ): View {
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
                setPadding(0, dp(activity, 4), 0, dp(activity, 4))
                background = GradientDrawable().apply {
                    cornerRadius = dp(activity, 15).toFloat()
                    setColor(
                        if (selected) 0xddb48000.toInt()
                        else 0x99131f2c.toInt()
                    )
                    setStroke(
                        dp(activity, if (selected) 2 else 1),
                        if (selected) 0xffffd75f.toInt()
                        else 0x44ffffff
                    )
                }
                setOnClickListener { open(activity, "settings") }
            }

            card.addView(
                text(
                    activity,
                    symbols[i],
                    12.5f,
                    if (selected) 0xffffe38c.toInt() else Color.WHITE,
                    true
                ),
                LinearLayout.LayoutParams(-1, dp(activity, 21))
            )

            card.addView(
                text(
                    activity,
                    name,
                    7.2f,
                    Color.WHITE,
                    selected
                ).apply { setLines(2) },
                LinearLayout.LayoutParams(-1, dp(activity, 24))
            )

            card.addView(
                text(
                    activity,
                    value ?: "...",
                    10.8f,
                    Color.WHITE,
                    true
                ),
                LinearLayout.LayoutParams(-1, dp(activity, 21))
            )

            row.addView(
                card,
                LinearLayout.LayoutParams(0, dp(activity, 72), 1f).apply {
                    setMargins(
                        dp(activity, 2),
                        0,
                        dp(activity, 2),
                        0
                    )
                }
            )
        }

        return row
    }

    private fun bottomNav(activity: Activity): View {
        val frame = FrameLayout(activity).apply {
            setBackgroundColor(Color.WHITE)
            elevation = dp(activity, 10).toFloat()
            clipChildren = false
            clipToPadding = false
        }

        val line = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(activity, 8), 0, dp(activity, 5))
        }

        fun cell(
            title: String,
            icon: Int,
            target: String,
            selected: Boolean = false
        ): View {
            return LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener {
                    if (target != "home") open(activity, target)
                }

                addView(
                    ImageView(activity).apply {
                        setImageResource(icon)
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        setColorFilter(
                            if (selected) 0xff19bfa9.toInt()
                            else 0xff34383b.toInt()
                        )
                    },
                    LinearLayout.LayoutParams(
                        dp(activity, 24),
                        dp(activity, 24)
                    )
                )

                addView(
                    text(
                        activity,
                        title,
                        9f,
                        if (selected) 0xff19bfa9.toInt()
                        else 0xff34383b.toInt(),
                        selected
                    ).apply {
                        setPadding(0, dp(activity, 5), 0, 0)
                    },
                    LinearLayout.LayoutParams(-1, dp(activity, 23))
                )
            }
        }

        line.addView(
            cell("Ana səhifə", R.drawable.ic_home, "home", true),
            LinearLayout.LayoutParams(0, dp(activity, 64), 1f)
        )
        line.addView(
            cell("Kömək et", R.drawable.ic_heart, "donate"),
            LinearLayout.LayoutParams(0, dp(activity, 64), 1f)
        )
        line.addView(
            View(activity),
            LinearLayout.LayoutParams(0, dp(activity, 64), 1f)
        )
        line.addView(
            cell("Sevimlilər", R.drawable.ic_heart, "saved"),
            LinearLayout.LayoutParams(0, dp(activity, 64), 1f)
        )
        line.addView(
            cell("Daha çox", R.drawable.ic_settings, "settings"),
            LinearLayout.LayoutParams(0, dp(activity, 64), 1f)
        )

        frame.addView(line, FrameLayout.LayoutParams(-1, -1))

        val center = FrameLayout(activity).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xffff8d63.toInt(), 0xffff744f.toInt())
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(activity, 5), Color.WHITE)
            }
            elevation = dp(activity, 12).toFloat()
            setOnClickListener { open(activity, "settings") }
        }

        center.addView(
            ImageView(activity).apply {
                setImageResource(R.drawable.ic_alieba_ai)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(
                    dp(activity, 17),
                    dp(activity, 17),
                    dp(activity, 17),
                    dp(activity, 17)
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        frame.addView(
            center,
            FrameLayout.LayoutParams(
                dp(activity, 78),
                dp(activity, 78),
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                topMargin = -dp(activity, 20)
            }
        )

        return frame
    }
}
