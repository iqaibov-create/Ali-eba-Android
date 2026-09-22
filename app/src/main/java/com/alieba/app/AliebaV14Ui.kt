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
    private val p=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    override fun onDraw(canvas:Canvas){
        val w=width.toFloat();val h=height.toFloat()
        if(w<=0f||h<=0f)return
        // Use ALL of the supplied mosque photograph from the TOP. No opaque
        // placeholder sky hiding the roof and minaret as in V14.
        p.shader=null;p.colorFilter=null;p.color=Color.WHITE
        canvas.drawBitmap(bitmap,null,RectF(0f,0f,w,h),p)
        if(night){
            p.color=0xb00a1c34.toInt() // Real night-darkening of the entire photograph.
            canvas.drawRect(0f,0f,w,h,p)
            // Illuminated arches, minaret and garden posts, anchored to this photo.
            // These are Android Canvas lighting effects, not a second stock photo.
            fun lamp(x:Float,y:Float,r:Float,power:Int){
                p.shader=RadialGradient(w*x,h*y,w*r,
                    intArrayOf(power,0x36f4ba66,Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
                canvas.drawCircle(w*x,h*y,w*r,p);p.shader=null
            }
            lamp(.29f,.68f,.15f,0xb7ffe6a1.toInt())
            lamp(.37f,.67f,.14f,0x9dffd788.toInt())
            lamp(.48f,.67f,.15f,0x9affd991.toInt())
            lamp(.57f,.54f,.13f,0x7dffc774.toInt())
            lamp(.49f,.82f,.16f,0x7dffd991.toInt())
            lamp(.29f,.78f,.12f,0xc1ffe0a1.toInt())
            lamp(.59f,.78f,.12f,0xb5ffe6b0.toInt())
            lamp(.75f,.79f,.12f,0x92ffdda4.toInt())
            lamp(.57f,.19f,.10f,0x73f7b86a.toInt())
            // A restrained night moon and stars over the sky.
            p.shader=null;p.color=0xfff3e5bb.toInt()
            canvas.drawCircle(w*.13f,h*.13f,w*.023f,p)
            p.color=0xff12314a.toInt()
            canvas.drawCircle(w*.139f,h*.122f,w*.021f,p)
            p.color=0xb8fff4d4.toInt()
            for(pt in arrayOf(.25f to .07f,.36f to .12f,.73f to .08f,.81f to .16f,.17f to .28f)){
                canvas.drawCircle(w*pt.first,h*pt.second,w*.0025f,p)
            }
        } else {
            // Light text-contrast gradient only; do NOT cover half the picture.
            p.shader=LinearGradient(0f,0f,0f,h*.27f,
                0x4d0e3943,Color.TRANSPARENT,Shader.TileMode.CLAMP)
            canvas.drawRect(0f,0f,w,h*.27f,p);p.shader=null
        }
        p.color=Color.WHITE
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
        "quran"->0xff10a995.toInt();"mafatih"->0xffec8e59.toInt();"ahkam"->0xff0696bd.toInt()
        "news"->0xffd54b97.toInt();"hadis"->0xff269356.toInt();"mersiye"->0xffbc3c4c.toInt()
        "kitabxana"->0xff3b5aca.toInt();"saved"->0xff9b6fc4.toInt();"settings"->0xff2c896e.toInt()
        else->0xff255fd0.toInt()
    }
    fun make(activity:Activity,section:String):View {
        val c:Context=activity
        val accent=tintFor(section)
        fun open(to:String){
            when(to){
                "home"->{
                    if(activity is MainActivity) return
                    activity.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                    activity.finish()
                }
                "settings"->if(activity !is NativeSettingsActivity)activity.startActivity(Intent(c,NativeSettingsActivity::class.java))
                "ai"->AlertDialog.Builder(activity).setTitle("Alieba köməkçi")
                    .setMessage("Alieba köməkçisi hazırlanır. Hazırda namaz vaxtlarına və ayarlara keçə bilərsiniz.")
                    .setPositiveButton("Azan ayarları"){_,_->activity.startActivity(Intent(c,NativeSettingsActivity::class.java))}
                    .setNegativeButton("Bağla",null).show()
                else->if(!(activity is NativeContentActivity && section==to)){
                    activity.startActivity(Intent(c,NativeContentActivity::class.java).putExtra("section",to))
                }
            }
        }
        val frame=FrameLayout(c).apply {
            background=BottomWaveDrawable(Color.WHITE,dp(c,24).toFloat())
            elevation=dp(c,6).toFloat()
            clipChildren=false;clipToPadding=false
        }
        val line=LinearLayout(c).apply {
            orientation=LinearLayout.HORIZONTAL;gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            setPadding(dp(c,6),dp(c,22),dp(c,6),dp(c,6))
            clipChildren=false
        }
        fun cell(title:String,icon:Int,to:String){
            val selected=section==to
            val v=LinearLayout(c).apply {
                orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
                setOnClickListener{open(to)}
            }
            v.addView(ImageView(c).apply {
                setImageResource(icon)
                setColorFilter(if(selected)accent else 0xff546b69.toInt())
            },LinearLayout.LayoutParams(dp(c,22),dp(c,22)))
            v.addView(TextView(c).apply {
                text=title;textSize=9.5f;gravity=Gravity.CENTER
                setTextColor(if(selected)accent else 0xff506662.toInt())
                isSingleLine=true
                setPadding(0,dp(c,3),0,0)
            },LinearLayout.LayoutParams(-1,dp(c,20)))
            line.addView(v,LinearLayout.LayoutParams(0,dp(c,55),1f))
        }
        cell("Ana səhifə",R.drawable.ic_home,"home")
        cell("Yadda saxla",R.drawable.ic_heart,"saved")
        line.addView(View(c),LinearLayout.LayoutParams(dp(c,70),dp(c,48)))
        cell("Yeniliklər",R.drawable.ic_calendar,"news")
        cell("Ayarlar",R.drawable.ic_settings,"settings")
        frame.addView(line,FrameLayout.LayoutParams(-1,-1))
        val center=FrameLayout(c).apply {
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(accent,if(section=="home")0xff228dc5.toInt() else 0xff75b9a7.toInt())).apply {
                shape=GradientDrawable.OVAL
            }
            elevation=dp(c,5).toFloat()
            setOnClickListener{open("ai")}
        }
        center.addView(ImageView(c).apply {
            setImageResource(R.drawable.ic_alieba_ai)
            setColorFilter(Color.WHITE)
            setPadding(dp(c,14),dp(c,14),dp(c,14),dp(c,14))
        },FrameLayout.LayoutParams(-1,-1))
        frame.addView(center,FrameLayout.LayoutParams(dp(c,66),dp(c,66),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {topMargin=dp(c,1)})
        return frame
    }
}
