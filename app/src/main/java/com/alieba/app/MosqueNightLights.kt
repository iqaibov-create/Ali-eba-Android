package com.alieba.app

import android.content.Context
import android.graphics.*
import android.view.View

/** The original mosque photo is displayed FIT_CENTER above this canvas. Warm translucent
 * highlights are painted above its facade; no replacement photo is generated. */
class MosqueNightLights(c:Context): View(c){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 override fun onDraw(canvas:Canvas){
  val w=width.toFloat(); val h=height.toFloat()
  val imageH=kotlin.math.min(h,w*886f/1536f)
  val y=(h-imageH)/2f; val x=(w-imageH*1536f/886f)/2f
  val iw=imageH*1536f/886f
  p.color=0xffffd47e.toInt();p.alpha=158
  listOf(0.14f,0.31f,0.47f,0.65f,0.82f).forEach { xx ->
   val left=x+iw*xx
   canvas.drawRoundRect(left,y+imageH*.68f,left+iw*.021f,y+imageH*.79f,iw*.005f,iw*.005f,p)
  }
  p.color=0xffffd996.toInt();p.alpha=85
  canvas.drawCircle(x+iw*.74f,y+imageH*.41f,iw*.012f,p)
 }
}
