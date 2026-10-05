package com.alieba.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

object NativeUi {
    val bg = 0xfff7fbf8.toInt()
    val white = Color.WHITE
    val ink = 0xff173f36.toInt()
    val green = 0xff176454.toInt()
    val green2 = 0xff2d8068.toInt()
    val soft = 0xffedf7f2.toInt()
    val line = 0xffdce8e2.toInt()
    val muted = 0xff71847d.toInt()
    val gold = 0xffb79242.toInt()
    val red = 0xffb94e4e.toInt()

    fun dp(c: Context, n: Int) =
        (n * c.resources.displayMetrics.density).toInt()

    fun shape(
        c: Context,
        color: Int,
        radius: Int = 16,
        stroke: Int? = null,
        strokeWidth: Int = 1
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(c, radius).toFloat()
        if (stroke != null) setStroke(dp(c, strokeWidth), stroke)
    }

    fun text(
        c: Context,
        value: String,
        size: Float = 15f,
        color: Int = ink,
        bold: Boolean = false
    ) = TextView(c).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
        includeFontPadding = false
    }

    fun topBar(activity: Activity, title: String, onBack: (() -> Unit)? = null): View {
        val row = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(activity, 10), dp(activity, 7), dp(activity, 13), dp(activity, 7))
            setBackgroundColor(Color.WHITE)
        }

        val back = text(activity, if (onBack == null) "☪" else "‹", if (onBack == null) 24f else 34f,
            if (onBack == null) green else ink, true).apply {
            gravity = Gravity.CENTER
            if (onBack != null) setOnClickListener { onBack() }
        }
        row.addView(back, LinearLayout.LayoutParams(dp(activity, 44), dp(activity, 46)))

        row.addView(
            text(activity, title, 20f, ink, true).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(activity, 6), 0, 0, 0)
            },
            LinearLayout.LayoutParams(0, dp(activity, 48), 1f)
        )

        return row
    }

    fun card(c: Context, radius: Int = 17) =
        shape(c, Color.WHITE, radius, line)

    fun setBars(activity: Activity) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        activity.window.statusBarColor = bg
        activity.window.navigationBarColor = Color.WHITE
        activity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
}

class NativeWaveDrawable(private val density: Float) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(c: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val lift = 23f * density
        val cx = w / 2f
        val half = 58f * density

        val path = Path().apply {
            moveTo(0f, lift)
            lineTo(cx - half, lift)
            cubicTo(
                cx - 36f*density, lift,
                cx - 34f*density, 0f,
                cx, 0f
            )
            cubicTo(
                cx + 34f*density, 0f,
                cx + 36f*density, lift,
                cx + half, lift
            )
            lineTo(w, lift)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        c.drawPath(path, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f * density
        p.color = 0xffdce8e2.toInt()
        c.drawPath(path, p)
    }

    override fun setAlpha(alpha: Int) { p.alpha = alpha }
    override fun setColorFilter(filter: android.graphics.ColorFilter?) { p.colorFilter = filter }
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
