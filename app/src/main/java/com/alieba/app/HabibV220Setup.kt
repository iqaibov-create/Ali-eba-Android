package com.alieba.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

object HabibV220Setup {

    private fun dp(a: Activity, v: Int) =
        (v * a.resources.displayMetrics.density).toInt()

    private fun txt(
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

    private fun box(a: Activity, color: Int, radius: Int, stroke: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(a, radius).toFloat()
            if (stroke != null) setStroke(dp(a, 1), stroke)
        }

    private fun dots(a: Activity, active: Int): View {
        val row = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        repeat(3) { i ->
            row.addView(
                View(a).apply {
                    background = GradientDrawable().apply {
                        setColor(if (i == active) 0xff292b30.toInt() else 0xffc8cacc.toInt())
                        cornerRadius = dp(a, 7).toFloat()
                    }
                },
                LinearLayout.LayoutParams(
                    dp(a, if (i == active) 30 else 16),
                    dp(a, 9)
                ).apply { setMargins(dp(a, 4), 0, dp(a, 4), 0) }
            )
        }
        return row
    }

    private fun base(a: Activity, active: Int): LinearLayout =
        LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(a, 22), dp(a, 20), dp(a, 22), dp(a, 22))
            setBackgroundColor(0xfffbfcfd.toInt())
            addView(
                dots(a, active),
                LinearLayout.LayoutParams(-1, dp(a, 24)).apply {
                    bottomMargin = dp(a, 24)
                }
            )
        }

    private fun illustration(a: Activity, icon: Int, c1: Int, c2: Int): View {
        val frame = FrameLayout(a)
        val circle = FrameLayout(a).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(c1, c2)
            ).apply { shape = GradientDrawable.OVAL }
        }
        circle.addView(
            ImageView(a).apply {
                setImageResource(icon)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(a, 35), dp(a, 35), dp(a, 35), dp(a, 35))
            },
            FrameLayout.LayoutParams(-1, -1)
        )
        frame.addView(
            circle,
            FrameLayout.LayoutParams(dp(a, 150), dp(a, 150), Gravity.CENTER)
        )
        return frame
    }

    fun language(
        a: Activity,
        selected: String?,
        onSelect: (String) -> Unit
    ): View {
        val root = base(a, 0)

        root.addView(
            illustration(a, R.drawable.ic_library, 0xff29b8e8.toInt(), 0xff9358ed.toInt()),
            LinearLayout.LayoutParams(-1, dp(a, 185))
        )

        root.addView(
            txt(a, "Dilinizi seçin", 23f, 0xff292b30.toInt(), true),
            LinearLayout.LayoutParams(-1, dp(a, 40))
        )
        root.addView(
            txt(a, "Alieba tətbiqinin istifadə dilini seçin", 13.5f, 0xff8a8d91.toInt()),
            LinearLayout.LayoutParams(-1, dp(a, 48))
        )

        val languages = listOf(
            "Azərbaycan" to "az",
            "Türkçe" to "tr",
            "Русский" to "ru",
            "ქართული" to "ka"
        )

        languages.forEach { (name, code) ->
            val chosen = selected == code
            root.addView(
                txt(
                    a,
                    name,
                    16f,
                    if (chosen) Color.WHITE else 0xff383a3d.toInt(),
                    chosen
                ).apply {
                    background = box(
                        a,
                        if (chosen) 0xff292b30.toInt() else Color.WHITE,
                        22,
                        0xffe0e2e4.toInt()
                    )
                    setOnClickListener { onSelect(code) }
                },
                LinearLayout.LayoutParams(-1, dp(a, 56)).apply {
                    bottomMargin = dp(a, 10)
                }
            )
        }

        return root
    }

    fun location(
        a: Activity,
        ready: Boolean,
        onAuto: () -> Unit,
        onManual: (String) -> Unit,
        onNext: () -> Unit
    ): View {
        val root = base(a, 1)

        root.addView(
            illustration(a, R.drawable.ic_profile, 0xff35bde6.toInt(), 0xffe465c9.toInt()),
            LinearLayout.LayoutParams(-1, dp(a, 190))
        )

        root.addView(
            txt(a, "Məkanınızı seçin", 23f, 0xff292b30.toInt(), true),
            LinearLayout.LayoutParams(-1, dp(a, 40))
        )
        root.addView(
            txt(
                a,
                "Azan və təqvim funksiyalarının düzgün işləməsi üçün məkan lazımdır.",
                13.5f,
                0xff8a8d91.toInt()
            ),
            LinearLayout.LayoutParams(-1, dp(a, 62))
        )

        val city = EditText(a).apply {
            hint = "Şəhər — məsələn: Marneuli"
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(a, 18), 0, dp(a, 18), 0)
            background = box(a, Color.WHITE, 18, 0xffd9dcdf.toInt())
        }

        root.addView(
            city,
            LinearLayout.LayoutParams(-1, dp(a, 58)).apply {
                topMargin = dp(a, 8)
                bottomMargin = dp(a, 10)
            }
        )

        root.addView(
            txt(a, "Şəhəri yadda saxla", 15f, 0xff292b30.toInt(), true).apply {
                background = box(a, 0xfff1f2f3.toInt(), 18)
                setOnClickListener {
                    val value = city.text.toString().trim()
                    if (value.isEmpty()) city.error = "Şəhəri yazın"
                    else onManual(value)
                }
            },
            LinearLayout.LayoutParams(-1, dp(a, 55)).apply {
                bottomMargin = dp(a, 10)
            }
        )

        root.addView(
            txt(
                a,
                if (ready) "✓ Məkan seçildi" else "◎  Avtomatik yer müəyyənləşdirmə",
                15.5f,
                Color.WHITE,
                true
            ).apply {
                background = box(a, 0xff292b30.toInt(), 18)
                setOnClickListener { onAuto() }
            },
            LinearLayout.LayoutParams(-1, dp(a, 58)).apply {
                bottomMargin = dp(a, 10)
            }
        )

        root.addView(
            txt(a, "Sonrakı  →", 16f, Color.WHITE, true).apply {
                background = box(
                    a,
                    if (ready) 0xff292b30.toInt() else 0xffaeb2b5.toInt(),
                    18
                )
                setOnClickListener {
                    if (ready) onNext()
                    else Toast.makeText(a, "Əvvəl məkanınızı seçin.", Toast.LENGTH_SHORT).show()
                }
            },
            LinearLayout.LayoutParams(-1, dp(a, 58))
        )

        return root
    }

    fun notifications(
        a: Activity,
        permissionGranted: Boolean,
        onPermission: () -> Unit,
        onFinish: (Boolean) -> Unit,
        onBack: () -> Unit
    ): View {
        val root = base(a, 2)

        root.addView(
            illustration(
                a,
                R.drawable.ic_notification_mosque,
                0xff26bfc1.toInt(),
                0xff4a83e6.toInt()
            ),
            LinearLayout.LayoutParams(-1, dp(a, 190))
        )

        root.addView(
            txt(a, "Azan Ayarları", 23f, 0xff292b30.toInt(), true),
            LinearLayout.LayoutParams(-1, dp(a, 40))
        )
        root.addView(
            txt(
                a,
                "Namaz vaxtlarında bildiriş və azan səsi üçün icazələri seçin.",
                13.5f,
                0xff8a8d91.toInt()
            ),
            LinearLayout.LayoutParams(-1, dp(a, 62))
        )

        val enabled = Switch(a).apply {
            text = "Namaz bildirişləri və azan"
            textSize = 16f
            isChecked = true
            setTextColor(0xff292b30.toInt())
            setPadding(dp(a, 18), 0, dp(a, 18), 0)
            background = box(a, Color.WHITE, 18, 0xffdfe2e4.toInt())
        }

        root.addView(
            enabled,
            LinearLayout.LayoutParams(-1, dp(a, 62)).apply {
                bottomMargin = dp(a, 10)
            }
        )

        if (!permissionGranted) {
            root.addView(
                txt(a, "Bildiriş icazəsini ver", 15f, 0xff292b30.toInt(), true).apply {
                    background = box(a, 0xfff0f1f2.toInt(), 18)
                    setOnClickListener { onPermission() }
                },
                LinearLayout.LayoutParams(-1, dp(a, 55)).apply {
                    bottomMargin = dp(a, 10)
                }
            )
        }

        root.addView(
            txt(a, "Alieba-ya başla  →", 16f, Color.WHITE, true).apply {
                background = box(a, 0xff292b30.toInt(), 18)
                setOnClickListener { onFinish(enabled.isChecked) }
            },
            LinearLayout.LayoutParams(-1, dp(a, 58)).apply {
                bottomMargin = dp(a, 8)
            }
        )

        root.addView(
            txt(a, "‹ Məkan seçiminə qayıt", 13.5f, 0xff6f7478.toInt()).apply {
                setOnClickListener { onBack() }
            },
            LinearLayout.LayoutParams(-1, dp(a, 44))
        )

        return root
    }
}
