package com.alieba.app

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class HabibV222WaveDrawable(
    private val density: Float,
    private val fillColor: Int,
    private val lineColor: Int
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val base = 29f * density
        val cx = w / 2f
        val half = 88f * density
        val top = 1.5f * density

        val path = Path().apply {
            moveTo(0f, base)
            lineTo(cx - half, base)
            cubicTo(
                cx - 63f * density, base,
                cx - 55f * density, top,
                cx, top
            )
            cubicTo(
                cx + 55f * density, top,
                cx + 63f * density, base,
                cx + half, base
            )
            lineTo(w, base)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        paint.style = Paint.Style.FILL
        paint.color = fillColor
        canvas.drawPath(path, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * density
        paint.color = lineColor
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
