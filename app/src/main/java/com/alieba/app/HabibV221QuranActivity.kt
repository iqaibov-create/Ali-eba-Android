package com.alieba.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*

class HabibV221QuranActivity : Activity() {

    private val teal = 0xff22cbb7.toInt()
    private val tealDark = 0xff08ad9b.toInt()
    private val ink = 0xff222428.toInt()
    private val muted = 0xffa0a2a5.toInt()
    private val pageBg = 0xfff6f6f6.toInt()

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    private fun rounded(
        color: Int,
        radius: Int,
        stroke: Int? = null,
        strokeWidth: Int = 1
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(strokeWidth), stroke)
    }

    private fun text(
        value: String,
        size: Float,
        color: Int = ink,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private val arabicNames = arrayOf(
        "الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال",
        "التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف",
        "مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل",
        "القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس",
        "الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية",
        "الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر",
        "الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة",
        "المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج",
        "نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات",
        "عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى",
        "الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق",
        "القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة",
        "الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"
    )

    private val ayahCounts = intArrayOf(
        7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,
        135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,
        54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,
        11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,
        22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = 0xffeffcf9.toInt()
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        setContentView(buildScreen())
    }

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBg)
            clipChildren = false
        }

        val top = FrameLayout(this).apply {
            background = AliebaPatternDrawable(resources.displayMetrics.density)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(6))
        }

        val back = text("‹", 43f, Color.BLACK).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 18)
            elevation = dp(2).toFloat()
            setOnClickListener { finish() }
        }
        headerRow.addView(back, LinearLayout.LayoutParams(dp(58), dp(58)))

        headerRow.addView(
            text("Quran", 27f, 0xff15171a.toInt(), true).apply {
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(0, dp(58), 1f)
        )

        val searchButton = text("⌕", 34f, Color.BLACK).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 18)
            elevation = dp(2).toFloat()
        }
        headerRow.addView(
            searchButton,
            LinearLayout.LayoutParams(dp(58), dp(58)).apply {
                rightMargin = dp(8)
            }
        )

        val history = text("↶", 31f, Color.BLACK).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 18)
            elevation = dp(2).toFloat()
            setOnClickListener {
                Toast.makeText(
                    this@HabibV221QuranActivity,
                    "Son oxunan surə aşağıdakı siyahıda saxlanılır.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        headerRow.addView(history, LinearLayout.LayoutParams(dp(58), dp(58)))

        top.addView(
            headerRow,
            FrameLayout.LayoutParams(-1, dp(76), Gravity.TOP)
        )

        root.addView(top, LinearLayout.LayoutParams(-1, dp(88)))

        val tabsWrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = rounded(0xffe9e9e9.toInt(), 24)
        }

        fun tab(title: String, selected: Boolean): View =
            text(title, 15f, if (selected) ink else 0xff8f9092.toInt(), selected).apply {
                gravity = Gravity.CENTER
                background = if (selected) rounded(Color.WHITE, 19) else null
                if (!selected) {
                    setOnClickListener {
                        Toast.makeText(
                            this@HabibV221QuranActivity,
                            "$title bölməsi növbəti mərhələdə aktiv ediləcək.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        listOf("Surə","Cüz","Səhifə","Təfsir").forEachIndexed { index, title ->
            tabsWrap.addView(
                tab(title, index == 0),
                LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                    if (index > 0) leftMargin = dp(2)
                }
            )
        }

        root.addView(
            tabsWrap,
            LinearLayout.LayoutParams(-1, dp(62)).apply {
                leftMargin = dp(18)
                rightMargin = dp(18)
                bottomMargin = dp(12)
            }
        )

        val finder = EditText(this).apply {
            hint = "Surə adına görə axtar..."
            hintTextColor = 0xff9ea0a3.toInt()
            setTextColor(ink)
            textSize = 17f
            setSingleLine(true)
            setPadding(dp(24), 0, dp(18), 0)
            background = rounded(0xfffafafa.toInt(), 17, 0xffbfc1c3.toInt())
        }

        val searchFrame = FrameLayout(this)
        searchFrame.addView(
            finder,
            FrameLayout.LayoutParams(-1, dp(62))
        )
        searchFrame.addView(
            text("⌕", 30f, Color.BLACK).apply {
                gravity = Gravity.CENTER
            },
            FrameLayout.LayoutParams(dp(58), dp(62), Gravity.START)
        )
        finder.setPadding(dp(66), 0, dp(18), 0)

        root.addView(
            searchFrame,
            LinearLayout.LayoutParams(-1, dp(62)).apply {
                leftMargin = dp(18)
                rightMargin = dp(18)
                bottomMargin = dp(12)
            }
        )

        searchButton.setOnClickListener {
            finder.requestFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(finder, InputMethodManager.SHOW_IMPLICIT)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
        }

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(2), dp(18), dp(18))
        }

        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bottomNav(), LinearLayout.LayoutParams(-1, dp(94)))

        fun render(filter: String) {
            list.removeAllViews()
            val az = NativeContentActivity.surahs
            val meta = NativeContentActivity.surahMeta

            az.forEachIndexed { index, name ->
                val number = index + 1
                val arabic = arabicNames[index]

                if (
                    filter.isNotBlank() &&
                    !name.contains(filter, true) &&
                    !arabic.contains(filter, true) &&
                    !number.toString().contains(filter)
                ) return@forEachIndexed

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(13), dp(9), dp(13), dp(9))
                    background = rounded(Color.WHITE, 19)
                    elevation = dp(1).toFloat()
                    setOnClickListener { openSurah(number) }
                }

                val badge = FrameLayout(this).apply {
                    rotation = 45f
                    background = rounded(teal, 12, 0xff78ddd2.toInt())
                }
                badge.addView(
                    text(number.toString(), 12f, Color.WHITE, true).apply {
                        gravity = Gravity.CENTER
                        rotation = -45f
                    },
                    FrameLayout.LayoutParams(-1, -1)
                )
                row.addView(badge, LinearLayout.LayoutParams(dp(48), dp(48)))

                val labels = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(17), 0, dp(8), 0)
                }

                labels.addView(
                    text(name, 18f, ink, true)
                )
                labels.addView(
                    text(
                        "${meta[index]} | ${ayahCounts[index]} Ayələr",
                        12.5f,
                        0xffb0b2b4.toInt()
                    ).apply {
                        setPadding(0, dp(2), 0, 0)
                    }
                )

                row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))

                row.addView(
                    text(arabic, 23f, 0xff252629.toInt()).apply {
                        gravity = Gravity.CENTER or Gravity.END
                        textDirection = View.TEXT_DIRECTION_RTL
                        typeface = Typeface.create("serif", Typeface.NORMAL)
                    },
                    LinearLayout.LayoutParams(dp(128), dp(56))
                )

                list.addView(
                    row,
                    LinearLayout.LayoutParams(-1, dp(82)).apply {
                        bottomMargin = dp(10)
                    }
                )
            }
        }

        finder.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(s?.toString()?.trim() ?: "")
            }
        })

        render("")
        return root
    }

    private fun openSurah(number: Int) {
        startActivity(
            Intent(this, NativeContentActivity::class.java)
                .putExtra("section", "quran")
                .putExtra("surah_number", number)
        )
    }

    private fun bottomNav(): View {
        val frame = FrameLayout(this).apply {
            background = HabibV221WaveDrawable(
                resources.displayMetrics.density,
                Color.WHITE,
                0xffdce6e4.toInt()
            )
            elevation = dp(12).toFloat()
            clipChildren = false
            clipToPadding = false
            setPadding(0, dp(18), 0, 0)
        }

        fun nav(title: String, icon: Int, active: Boolean, action: () -> Unit): View {
            val color = if (active) tealDark else 0xff3f4144.toInt()
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener { action() }

                addView(
                    ImageView(this@HabibV221QuranActivity).apply {
                        setImageResource(icon)
                        setColorFilter(color)
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                    },
                    LinearLayout.LayoutParams(dp(24), dp(24))
                )

                addView(
                    text(title, 8.5f, color, active).apply {
                        gravity = Gravity.CENTER
                        setPadding(0, dp(4), 0, 0)
                    },
                    LinearLayout.LayoutParams(-1, dp(21))
                )
            }
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        row.addView(
            nav("Ana səhifə", R.drawable.ic_home, true) { finish() },
            LinearLayout.LayoutParams(0, -1, 1f)
        )
        row.addView(
            nav("Xətm", R.drawable.ic_library, false) {
                Toast.makeText(this@HabibV221QuranActivity, "Xətm bölməsi hazırlanır.", Toast.LENGTH_SHORT).show()
            },
            LinearLayout.LayoutParams(0, -1, 1f)
        )
        row.addView(View(this), LinearLayout.LayoutParams(0, -1, 1.05f))
        row.addView(
            nav("Haqqında", R.drawable.ic_notification_mosque, false) {
                Toast.makeText(this@HabibV221QuranActivity, "Qurani-Kərim • Alieba", Toast.LENGTH_SHORT).show()
            },
            LinearLayout.LayoutParams(0, -1, 1f)
        )
        row.addView(
            nav("Seçilmişlər", R.drawable.ic_heart, false) {
                startActivity(
                    Intent(this@HabibV221QuranActivity, NativeContentActivity::class.java)
                        .putExtra("section", "saved")
                )
            },
            LinearLayout.LayoutParams(0, -1, 1f)
        )

        frame.addView(
            row,
            FrameLayout.LayoutParams(-1, dp(70), Gravity.BOTTOM)
        )

        val center = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff12d2b7.toInt(), 0xff20c1c4.toInt())
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(5), Color.WHITE)
            }
            elevation = dp(11).toFloat()
            setOnClickListener {
                scrollToTop()
            }
        }

        center.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.ic_quran)
                setColorFilter(Color.WHITE)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(16), dp(16), dp(16), dp(16))
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        frame.addView(
            center,
            FrameLayout.LayoutParams(dp(70), dp(70), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        )

        return frame
    }

    private fun scrollToTop() {
        val decor = window.decorView
        decor.post {
            val scroll = findScrollView(decor)
            scroll?.smoothScrollTo(0, 0)
        }
    }

    private fun findScrollView(view: View): ScrollView? {
        if (view is ScrollView) return view
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findScrollView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }
}
