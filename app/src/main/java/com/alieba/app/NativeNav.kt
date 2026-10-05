package com.alieba.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*

object NativeNav {
    private fun dp(c: Context, n: Int) = NativeUi.dp(c, n)

    fun make(activity: Activity, section: String): View {
        val frame = FrameLayout(activity).apply {
            background = NativeWaveDrawable(resources.displayMetrics.density)
            elevation = dp(activity, 7).toFloat()
            clipChildren = false
            clipToPadding = false
        }

        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(0, dp(activity, 26), 0, dp(activity, 5))
        }

        fun open(to: String) {
            when (to) {
                "home" -> if (activity !is NativeHomeActivity) {
                    activity.startActivity(
                        Intent(activity, NativeHomeActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                    activity.finish()
                }

                "saved" -> activity.startActivity(
                    Intent(activity, NativeContentActivity::class.java)
                        .putExtra("section", "saved")
                )

                "news" -> activity.startActivity(
                    Intent(activity, NativeNewsActivity::class.java)
                )

                "settings" -> activity.startActivity(
                    Intent(activity, NativeSettingsActivity::class.java)
                )

                "ai" -> activity.startActivity(
                    Intent(activity, NativeAdviceActivity::class.java)
                )
            }
        }

        fun cell(label: String, icon: Int, target: String): View {
            val selected = section == target

            return LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener { open(target) }

                addView(
                    ImageView(activity).apply {
                        setImageResource(icon)
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        setColorFilter(
                            if (selected) NativeUi.green
                            else 0xff81918b.toInt()
                        )
                    },
                    LinearLayout.LayoutParams(dp(activity, 22), dp(activity, 22))
                )

                addView(
                    NativeUi.text(
                        activity,
                        label,
                        9.5f,
                        if (selected) NativeUi.green else 0xff6f807a.toInt(),
                        selected
                    ).apply {
                        gravity = Gravity.CENTER
                        setPadding(0, dp(activity, 4), 0, 0)
                    },
                    LinearLayout.LayoutParams(-1, dp(activity, 22))
                )
            }
        }

        row.addView(cell("Ana səhifə", R.drawable.ic_home, "home"),
            LinearLayout.LayoutParams(0, dp(activity, 58), 1f))
        row.addView(cell("Yadda saxla", R.drawable.ic_heart, "saved"),
            LinearLayout.LayoutParams(0, dp(activity, 58), 1f))
        row.addView(View(activity), LinearLayout.LayoutParams(0, dp(activity, 58), 1f))
        row.addView(cell("Yeniliklər", R.drawable.ic_calendar, "news"),
            LinearLayout.LayoutParams(0, dp(activity, 58), 1f))
        row.addView(cell("Ayarlar", R.drawable.ic_settings, "settings"),
            LinearLayout.LayoutParams(0, dp(activity, 58), 1f))

        frame.addView(row, FrameLayout.LayoutParams(-1, -1))

        val ai = FrameLayout(activity).apply {
            background = NativeUi.shape(activity, 0xfff4fbf7.toInt(), 44, NativeUi.line)
            elevation = dp(activity, 6).toFloat()
            setOnClickListener { open("ai") }
        }

        ai.addView(
            ImageView(activity).apply {
                setImageResource(R.drawable.alieba_gold_ai)
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = "AI köməkçi"
                setPadding(dp(activity, 7), dp(activity, 7), dp(activity, 7), dp(activity, 7))
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        frame.addView(
            ai,
            FrameLayout.LayoutParams(
                dp(activity, 70),
                dp(activity, 70),
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                topMargin = dp(activity, 1)
            }
        )

        return frame
    }
}
