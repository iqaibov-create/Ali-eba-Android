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

/** V17: preserve the aspect of the ORIGINAL photo, extend only sky and animate stars on Canvas. */
class MosqueSceneView(c:Context,private val night:Boolean):View(c){
    private val photo=BitmapFactory.decodeResource(resources,if(night)R.drawable.mosque_night else R.drawable.mosque_day)
    private val p=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val stars=(0 until 31).map { i ->
        val x=((i*73+19)%97+1)/100f
        val y=((i*43+7)%71+2)/100f
        Triple(x,y,i*1.1f)
    }
    override fun onDraw(canvas:Canvas){
        val w=width.toFloat();val h=height.toFloat()
        if(w<=0f||h<=0f||photo.width<=0)return
        val imageHeight=w*photo.height.toFloat()/photo.width.toFloat()
        val imageTop=(h-imageHeight).coerceAtLeast(0f)
        // A portrait screen cannot contain a landscape photograph at full width and
        // full height without cropping or stretching. Keep the building undistorted;
        // fill the extra space with a sky that matches the photo, not a dark band.
        p.color=Color.WHITE;p.colorFilter=null
        p.shader=LinearGradient(0f,0f,0f,h,
            if(night)0xff102e55.toInt() else 0xff4b9dc7.toInt(),
            if(night)0xff244b79.toInt() else 0xffb8dfed.toInt(),Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,w,h,p);p.shader=null
        if(imageTop>1f){
            val skyPixels=(photo.height*.16f).toInt().coerceAtLeast(1)
            p.alpha=215
            canvas.drawBitmap(photo,Rect(0,0,photo.width,skyPixels),RectF(0f,0f,w,imageTop+2f),p)
            p.alpha=255
        }
        // Draw photo once at its aspect-correct size. Never stretch the minaret.
        val left=if(imageTop>0f)0f else (w-h*photo.width/photo.height)/2f
        val dest=if(imageTop>0f)RectF(0f,imageTop,w,h) else RectF(left,0f,left+h*photo.width/photo.height,h)
        canvas.drawBitmap(photo,null,dest,p)
        if(night){
            val tm=android.os.SystemClock.uptimeMillis()
            val skyLimit=if(imageTop>0f)imageTop+imageHeight*.12f else h*.24f
            // Small, slowly twinkling stars only over the sky; no image generation.
            stars.forEachIndexed { i,v ->
                val opacity=(110+110*kotlin.math.sin(tm/1100.0+i*.87)).toInt().coerceIn(20,225)
                p.color=Color.argb(opacity,255,248,211)
                val x=v.first*w;val y=v.second*skyLimit
                canvas.drawCircle(x,y,if(i%7==0)1.7f else .9f,p)
            }
            val cycle=tm%11000L
            if(cycle<1050L){
                val f=cycle/1050f
                val x=w*(.13f+f*.52f)
                val y=skyLimit*(.15f+f*.43f)
                p.color=Color.WHITE;p.strokeWidth=2.0f
                canvas.drawLine(x-w*.13f,y-skyLimit*.1f,x,y,p)
                canvas.drawCircle(x,y,2.3f,p)
            }
            postInvalidateDelayed(95L)
        }
    }
}

/** Nearly white mint with fine translucent geometric motifs; avoids a heavy green screen. */
class AliebaPatternDrawable(private val density:Float):Drawable(){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat()
        p.color=0xfff5f8f6.toInt();p.style=Paint.Style.FILL
        c.drawRect(0f,0f,w,h,p)
        val step=92f*density
        p.style=Paint.Style.STROKE;p.strokeWidth=.65f*density;p.color=0x13a2bbaa
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
        "news"->0xffd54b97.toInt();"donate"->0xffb78b3d.toInt();"advice"->0xffb78b3d.toInt();"hadis"->0xff269356.toInt();"mersiye"->0xffbc3c4c.toInt()
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
                "donate"->if(activity !is NativeDonateActivity)activity.startActivity(Intent(c,NativeDonateActivity::class.java))
                "advice"->if(activity !is NativeAdviceActivity)activity.startActivity(Intent(c,NativeAdviceActivity::class.java))
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
        cell("Kömək et",R.drawable.ic_heart,"donate")
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
