package com.alieba.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** Subtle warm window highlights on original mosque photo, not a generated replacement image. */
class MosqueNightLights(c:Context):View(c) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas:Canvas){
        val w=width.toFloat();val h=height.toFloat()
        // A small moon and window lights that scale with the hero layout.
        p.color=0xfff8e7ba.toInt();p.alpha=225
        canvas.drawCircle(w*.82f,h*.19f,w*.025f,p)
        p.color=0xff061a35.toInt();canvas.drawCircle(w*.83f,h*.18f,w*.025f,p)
        p.color=0xffffc878.toInt();p.alpha=120
        for(x in listOf(.315f,.41f,.505f,.615f)) {
            canvas.drawRoundRect(w*x-w*.023f,h*.52f,w*x+w*.023f,h*.61f,w*.01f,w*.01f,p)
        }
    }
}
