package com.alieba.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * Offline Gregorian/Hijri (Umm al-Qura) month calendar.
 * Hijri dates are an estimate, not a religious ruling on moonsighting.
 */
class NativeHijriCalendarActivity : Activity() {
    private val bg = 0xfff4f8f7.toInt()
    private val white = Color.WHITE
    private val ink = 0xff183a36.toInt()
    private val green = 0xff0d6257.toInt()
    private val gold = 0xffc39b50.toInt()
    private val muted = 0xff667d78.toInt()
    private val months = arrayOf(
        "Məhərrəm", "Səfər", "Rəbiüləvvəl", "Rəbiülaxır",
        "Cəmadiyələvvəl", "Cəmadiyəlaxır", "Rəcəb", "Şəban",
        "Ramazan", "Şəvval", "Zilqədə", "Zilhiccə"
    )
    private val weekDays = arrayOf("B.e", "Ç.a", "Ç.", "C.a", "C.", "Ş.", "B.")
    private val az = Locale("az")
    private val gregFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", az)
    private var year = 0
    private var month = 0
    private var selectedDay = 1
    private lateinit var body: LinearLayout

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun shape(color: Int, radius: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (border != null) setStroke(dp(1), border)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NativeUi.setBars(this)
        val today = HijrahDate.from(LocalDate.now())
        year = today.get(ChronoField.YEAR_OF_ERA)
        month = today.get(ChronoField.MONTH_OF_YEAR)
        selectedDay = today.get(ChronoField.DAY_OF_MONTH)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = shape(green, 0)
            setPadding(dp(12), dp(9), dp(14), dp(9))
        }
        toolbar.addView(text("‹", 34f, white).apply {
            setOnClickListener { finish() }
            contentDescription = "Geri"
        }, LinearLayout.LayoutParams(dp(46), dp(48)))
        toolbar.addView(text("Hicri-qəməri təqvim", 19f, white, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        toolbar.addView(text("☾", 24f, 0xfff6d89f.toInt()),
            LinearLayout.LayoutParams(dp(38), dp(45)))
        root.addView(toolbar)

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = false
        }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(15), dp(12), dp(30))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        render()
    }

    private fun event(month: Int, day: Int): String? = when (month to day) {
        1 to 1 -> "Hicri ilin başlanğıcı"
        1 to 10 -> "Aşura günü"
        2 to 20 -> "Ərbəin"
        9 to 1 -> "Ramazan ayının başlanğıcı"
        9 to 21 -> "Qədr gecələrindən biri"
        10 to 1 -> "Fitr bayramı"
        12 to 10 -> "Qurban bayramı"
        12 to 18 -> "Qədir-Xum bayramı"
        else -> null
    }

    private fun shiftMonth(amount: Int) {
        val ordinal = year * 12 + month - 1 + amount
        year = Math.floorDiv(ordinal, 12)
        month = Math.floorMod(ordinal, 12) + 1
        val max = HijrahDate.of(year, month, 1).lengthOfMonth()
        selectedDay = selectedDay.coerceIn(1, max)
        render()
    }

    private fun render() {
        body.removeAllViews()
        val first = HijrahDate.of(year, month, 1)
        val monthDays = first.lengthOfMonth()
        selectedDay = selectedDay.coerceIn(1, monthDays)
        val firstGregorian = LocalDate.from(first)
        val lastGregorian = LocalDate.from(HijrahDate.of(year, month, monthDays))
        val selected = HijrahDate.of(year, month, selectedDay)
        val selectedGregorian = LocalDate.from(selected)
        val today = HijrahDate.from(LocalDate.now())

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(22), dp(18), dp(22))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff12594f.toInt(), 0xff228477.toInt())
            ).apply { cornerRadius = dp(22).toFloat() }
        }
        heading.addView(text("☾  Hicri  $year", 16f, 0xffffdfa6.toInt(), true))
        heading.addView(text(months[month - 1], 27f, white, true).apply {
            typeface = Typeface.create("serif", Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(7))
        })
        heading.addView(text("${firstGregorian.format(DateTimeFormatter.ofPattern("d MMM", az))} – ${lastGregorian.format(gregFormat)}", 12f, 0xffe4f1ee.toInt()))
        body.addView(heading, LinearLayout.LayoutParams(-1, -2))

        val monthControls = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(16), 0, dp(10))
        }
        monthControls.addView(text("‹", 34f, green).apply {
            background = shape(white, 13, 0xffe1e9e6.toInt())
            setOnClickListener { shiftMonth(-1) }
            contentDescription = "Əvvəlki ay"
        }, LinearLayout.LayoutParams(dp(45), dp(44)))
        monthControls.addView(text("${months[month - 1]} $year", 17f, ink, true),
            LinearLayout.LayoutParams(0, dp(44), 1f))
        monthControls.addView(text("›", 34f, green).apply {
            background = shape(white, 13, 0xffe1e9e6.toInt())
            setOnClickListener { shiftMonth(1) }
            contentDescription = "Növbəti ay"
        }, LinearLayout.LayoutParams(dp(45), dp(44)))
        body.addView(monthControls)

        val calendar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(12), dp(6), dp(14))
            background = shape(white, 20, 0xffe2ebe7.toInt())
        }
        val dayTitles = LinearLayout(this)
        for (weekDay in weekDays) {
            dayTitles.addView(text(weekDay, 10.5f, muted, true),
                LinearLayout.LayoutParams(0, dp(34), 1f))
        }
        calendar.addView(dayTitles)
        val offset = firstGregorian.dayOfWeek.value - 1 // Monday-first
        val rows = (offset + monthDays + 6) / 7
        for (r in 0 until rows) {
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            for (c in 0 until 7) {
                val day = r * 7 + c - offset + 1
                if (day < 1 || day > monthDays) {
                    row.addView(View(this), LinearLayout.LayoutParams(0, dp(65), 1f))
                    continue
                }
                val hijri = HijrahDate.of(year, month, day)
                val gregorian = LocalDate.from(hijri)
                val isSelected = day == selectedDay
                val isToday = hijri == today
                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = shape(
                        when {
                            isSelected -> green
                            isToday -> 0xffe9f5ee.toInt()
                            else -> white
                        }, 10,
                        if (isToday && !isSelected) 0xffa4c7ad.toInt() else null
                    )
                    setOnClickListener {
                        selectedDay = day
                        render()
                    }
                    contentDescription = "$day ${months[month - 1]}, ${gregorian.format(gregFormat)}"
                }
                tile.addView(text(day.toString(), 17f,
                    if (isSelected) white else ink, true))
                tile.addView(text(gregorian.dayOfMonth.toString(), 9f,
                    if (isSelected) 0xffdff0e9.toInt() else muted).apply {
                    setPadding(0, dp(3), 0, 0)
                })
                if (event(month, day) != null) {
                    tile.addView(text("•", 10f,
                        if (isSelected) 0xffffdfa6.toInt() else gold))
                }
                row.addView(tile, LinearLayout.LayoutParams(0, dp(65), 1f).apply {
                    setMargins(dp(1), 0, dp(1), 0)
                })
            }
            calendar.addView(row)
        }
        body.addView(calendar, LinearLayout.LayoutParams(-1, -2))

        val dateInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = shape(white, 17, 0xffe2ebe7.toInt())
        }
        dateInfo.addView(text("Seçilmiş gün", 12f, muted).apply { gravity = Gravity.START })
        dateInfo.addView(text("$selectedDay ${months[month - 1]} $year", 20f, green, true).apply {
            gravity = Gravity.START
            setPadding(0, dp(6), 0, 0)
        })
        dateInfo.addView(text(selectedGregorian.format(gregFormat), 14f, ink).apply {
            gravity = Gravity.START
            setPadding(0, dp(5), 0, 0)
        })
        event(month, selectedDay)?.let { item ->
            dateInfo.addView(text("☾  $item", 14f, 0xff9a6c1c.toInt(), true).apply {
                gravity = Gravity.START
                setPadding(0, dp(12), 0, 0)
            })
        }
        body.addView(dateInfo, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        val events = (1..monthDays).mapNotNull { day ->
            event(month, day)?.let { day to it }
        }
        if (events.isNotEmpty()) {
            body.addView(text("Bu ayın dini günləri", 16f, ink, true).apply {
                gravity = Gravity.START
                setPadding(dp(4), dp(19), 0, dp(10))
            })
            for ((day, name) in events) {
                val row = LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    background = shape(white, 14)
                    setPadding(dp(12), dp(9), dp(12), dp(9))
                    setOnClickListener {
                        selectedDay = day
                        render()
                    }
                }
                row.addView(text(day.toString(), 16f, green, true).apply {
                    background = shape(0xffeaf5f1.toInt(), 10)
                }, LinearLayout.LayoutParams(dp(45), dp(43)))
                row.addView(text(name, 14f, ink).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), 0, 0, 0)
                }, LinearLayout.LayoutParams(0, -2, 1f))
                body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
            }
        }

        body.addView(text(
            "Hicri-qəməri tarixlər hesablamaya əsaslanır. Ayın görünməsinə və yerli dini təqvimə görə 1–2 gün fərqlənə bilər.",
            11f,
            muted
        ).apply {
            gravity = Gravity.START
            setPadding(dp(8), dp(17), dp(8), 0)
        })
    }
}
