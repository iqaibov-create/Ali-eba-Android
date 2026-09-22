package com.alieba.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import kotlin.math.min

/** Canvas-only decor: original user-provided photograph, no newly generated images. */
class MosqueSceneView(c:Context,private val night:Boolean):View(c){
    private val bitmap=BitmapFactory.decodeResource(resources,R.drawable.mosque_bg)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    override fun onDraw(canvas:Canvas){
        val w=width.toFloat();val h=height.toFloat()
        if(w<=0||h<=0)return
        paint.shader=LinearGradient(0f,0f,0f,h,
            if(night)0xff061827.toInt() else 0xff3d96b9.toInt(),
            if(night)0xff16403e.toInt() else 0xffa9d8e6.toInt(),Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,w,h,paint);paint.shader=null
        val imageH=w*bitmap.height/bitmap.width.toFloat()
        val top=h-imageH
        paint.colorFilter=if(night)ColorMatrixColorFilter(floatArrayOf(
            .27f,0f,0f,0f,0f, 0f,.35f,0f,0f,0f, 0f,0f,.54f,0f,0f, 0f,0f,0f,1f,0f
        )) else null
        canvas.drawBitmap(bitmap,null,RectF(0f,top,w,h),paint)
        paint.colorFilter=null
        // Blend the top edge of the landscape photograph into the extended sky.
        paint.shader=LinearGradient(0f,top,0f,top+min(70f,imageH*.35f),
            if(night)0xff122f38.toInt() else 0xff86c0d3.toInt(),Color.TRANSPARENT,Shader.TileMode.CLAMP)
        canvas.drawRect(0f,top,w,top+min(70f,imageH*.35f),paint);paint.shader=null
        if(night){
            // Discreet warm facade lights; highlights never replace the supplied photo.
            paint.shader=RadialGradient(w*.52f,h*.86f,w*.4f,
                intArrayOf(0x6ff3c76a,0x18f8dba6,Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
            canvas.drawRect(0f,top,w,h,paint);paint.shader=null
        }
        paint.shader=LinearGradient(0f,h*.45f,0f,h,
            0x0005161b,if(night)0x94030e18.toInt() else 0x43061a1d,Shader.TileMode.CLAMP)
        canvas.drawRect(0f,h*.45f,w,h,paint);paint.shader=null
    }
}

/** Nearly white mint with fine translucent geometric motifs; avoids a heavy green screen. */
class AliebaPatternDrawable(private val density:Float):Drawable(){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat()
        p.color=0xfff7faf7.toInt();p.style=Paint.Style.FILL
        c.drawRect(0f,0f,w,h,p)
        val step=92f*density
        p.style=Paint.Style.STROKE;p.strokeWidth=.65f*density;p.color=0x12a2bbaa
        var y=-step
        while(y<h+step){
            var x=-step
            while(x<w+step){
                val r=step*.28f
                c.drawLine(x,y-r,x+r,y,p);c.drawLine(x+r,y,x,y+r,p)
                c.drawLine(x,y+r,x-r,y,p);c.drawLine(x-r,y,x,y-r,p)
                c.drawCircle(x,y,r*.34f,p)
                x+=step
            }
            y+=step
        }
        p.style=Paint.Style.FILL
    }
    override fun setAlpha(alpha:Int){p.alpha=alpha}
    override fun setColorFilter(filter:android.graphics.ColorFilter?){p.colorFilter=filter}
    override fun getOpacity()=android.graphics.PixelFormat.OPAQUE
}

object AliebaBottomNav {
    private fun dp(c:Context,x:Int)=(x*c.resources.displayMetrics.density).toInt()
    private fun tintFor(section:String)=when(section){
        "quran"->0xff12bfa6.toInt();"mafatih"->0xffec8e59.toInt();"ahkam"->0xff06a4c8.toInt()
        "news"->0xffd54b97.toInt();"hadis"->0xff34a15b.toInt();"mersiye"->0xffbc3c4c.toInt()
        "kitabxana"->0xff3b5aca.toInt();"saved"->0xff9b6fc4.toInt();"settings"->0xff2c896e.toInt()
        else->0xff215cbf.toInt()
    }
    fun make(activity:Activity,section:String):View {
        val c:Context=activity
        val active=tintFor(section)
        val bar=LinearLayout(c).apply {
            gravity=Gravity.CENTER_VERTICAL
            orientation=LinearLayout.HORIZONTAL
            background=BottomWaveDrawable(Color.WHITE,dp(c,27).toFloat())
            elevation=dp(c,8).toFloat()
            setPadding(dp(c,3),dp(c,19),dp(c,3),dp(c,3))
        }
        fun sectionIntent(to:String){
            when(to){
                "home"->{activity.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP));if(activity !is MainActivity)activity.finish()}
                "settings"->if(activity !is NativeSettingsActivity)activity.startActivity(Intent(c,NativeSettingsActivity::class.java))
                "ai"->AlertDialog.Builder(activity).setTitle("Alieba köməkçi")
                    .setMessage("AI köməkçi hazırlıq mərhələsindədir. Hələlik namaz vaxtlarına və ayarlara keçə bilərsiniz.")
                    .setPositiveButton("Azan ayarları"){_,_->activity.startActivity(Intent(c,NativeSettingsActivity::class.java))}
                    .setNegativeButton("Bağla",null).show()
                else-> if(!(activity is NativeContentActivity && section==to))activity.startActivity(Intent(c,NativeContentActivity::class.java).putExtra("section",to))
            }
        }
        fun cell(title:String,icon:Int,destination:String){
            val selected=section==destination
            val v=LinearLayout(c).apply {gravity=Gravity.CENTER;orientation=LinearLayout.VERTICAL;setOnClickListener{sectionIntent(destination)}}
            v.addView(ImageView(c).apply {setImageResource(icon);setColorFilter(if(selected)active else 0xff586c68.toInt())},LinearLayout.LayoutParams(dp(c,24),dp(c,24)))
            v.addView(TextView(c).apply {text=title;textSize=9.3f;gravity=Gravity.CENTER;setTextColor(if(selected)active else 0xff66756f.toInt());maxLines=1},LinearLayout.LayoutParams(-1,dp(c,18)))
            bar.addView(v,LinearLayout.LayoutParams(0,-1,1f))
        }
        cell("Ana səhifə",R.drawable.ic_home,"home")
        cell("Yadda saxla",R.drawable.ic_heart,"saved")
        val center=FrameLayout(c).apply {
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(active,0xff4fbfba.toInt())).apply{cornerRadius=dp(c,38).toFloat()}
            elevation=dp(c,6).toFloat()
            setOnClickListener{sectionIntent("ai")}
        }
        center.addView(ImageView(c).apply{setImageResource(R.drawable.ic_alieba_ai);setColorFilter(Color.WHITE);setPadding(dp(c,11),dp(c,11),dp(c,11),dp(c,11))},FrameLayout.LayoutParams(-1,-1))
        bar.addView(center,LinearLayout.LayoutParams(dp(c,62),dp(c,62)).apply {bottomMargin=dp(c,8)})
        cell("Yeniliklər",R.drawable.ic_calendar,"news")
        cell("Ayarlar",R.drawable.ic_settings,"settings")
        return bar
    }
}
