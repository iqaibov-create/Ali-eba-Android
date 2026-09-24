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

/** V17.2: use the approved day/night mosque photos directly, avoid split bands,
 * keep the mosque natural, and show only soft twinkling stars at night. */
class MosqueSceneView(c:Context,private val night:Boolean):View(c){
    private val photo=BitmapFactory.decodeResource(resources,if(night)R.drawable.mosque_night else R.drawable.mosque_day)
    private val p=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    // Keep the photo fully opaque. Star twinkle must never change the bitmap paint.
    private val starPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply {style=Paint.Style.FILL}
    private val stars=(0 until 30).map { i ->
        val x=((i*73+19)%97+1)/100f
        val y=((i*47+11)%58+3)/100f
        x to y
    }
    override fun onDraw(canvas:Canvas){
        val w=width.toFloat();val h=height.toFloat()
        if(w<=0f||h<=0f||photo.width<=0||photo.height<=0)return

        p.colorFilter=null
        p.alpha=255
        canvas.drawColor(if(night)0xff14345e.toInt() else 0xff9bcfee.toInt())

        if(photo.height > photo.width){
            // Portrait image: crop a controlled vertical window so the mosque fits naturally.
            val viewRatio=w/h
            val cropH=((photo.width / viewRatio)*1.12f).toInt().coerceAtMost(photo.height)
            // A slightly wider vertical framing reveals more of the user's ORIGINAL mosque image.
            // Keep the ground in view; never paint or dim the picture for the star effect.
            val srcTop=(photo.height-cropH).coerceAtLeast(0)
            val src=Rect(0,srcTop,photo.width,srcTop+cropH)
            canvas.drawBitmap(photo,src,RectF(0f,0f,w,h),p)
        } else {
            // Landscape fallback: fill only the extra top area with matching sky, never a dark strip.
            val imageHeight=w*photo.height.toFloat()/photo.width.toFloat()
            val imageTop=(h-imageHeight).coerceAtLeast(0f)
            if(imageTop>1f){
                val skyPixels=(photo.height*.18f).toInt().coerceAtLeast(1)
                p.alpha=232
                canvas.drawBitmap(photo,Rect(0,0,photo.width,skyPixels),RectF(0f,0f,w,imageTop+2f),p)
                p.alpha=255
            }
            canvas.drawBitmap(photo,null,RectF(0f,imageTop,w,imageTop+imageHeight),p)
        }

        if(night){
            val tm=android.os.SystemClock.uptimeMillis()
            val skyLimit=h*.42f
            stars.forEachIndexed { i,v ->
                val opacity=(85+120*kotlin.math.sin(tm/1200.0+i*.87)).toInt().coerceIn(22,210)
                starPaint.color=Color.argb(opacity,255,248,219)
                val x=v.first*w;val y=v.second*skyLimit
                canvas.drawCircle(x,y,if(i%8==0)1.7f else 1.0f,starPaint)
            }
            postInvalidateDelayed(110L)
        }
    }
}

/** Warm ivory arabesque paper shared by home and all inner screens. */
class AliebaPatternDrawable(private val density:Float):Drawable(){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat()
        p.style=Paint.Style.FILL;p.color=0xfffff9ed.toInt()
        c.drawRect(0f,0f,w,h,p)
        val step=102f*density
        p.style=Paint.Style.STROKE;p.strokeWidth=.73f*density
        var y=-step
        while(y<h+step){
            var x=-step
            while(x<w+step){
                val r=step*.28f
                p.color=0x1fb38c54
                // Small four-petal arabesque with a pointed arch and restrained gold detailing.
                val motif=Path().apply {
                    moveTo(x,y-r)
                    cubicTo(x+r*.62f,y-r*.45f,x+r*.62f,y+r*.45f,x,y+r)
                    cubicTo(x-r*.62f,y+r*.45f,x-r*.62f,y-r*.45f,x,y-r)
                    close()
                    moveTo(x-r,y)
                    cubicTo(x-r*.45f,y-r*.62f,x+r*.45f,y-r*.62f,x+r,y)
                    cubicTo(x+r*.45f,y+r*.62f,x-r*.45f,y+r*.62f,x-r,y)
                    close()
                }
                c.drawPath(motif,p)
                p.color=0x12b68f57
                c.drawCircle(x,y,r*.38f,p)
                c.drawCircle(x+r*.92f,y+r*.92f,r*.17f,p)
                c.drawCircle(x-r*.92f,y-r*.92f,r*.17f,p)
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

/** A gold-trimmed Islamic arch at the bottom of the mosque, NOT an S-shaped wave. */
class AliebaHeroDividerDrawable(private val density:Float):Drawable(){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat()
        val d=density
        val edge=Path().apply {
            moveTo(0f,h*.32f)
            lineTo(w*.38f,h*.32f)
            cubicTo(w*.44f,h*.32f,w*.46f,h*.14f,w*.5f,h*.07f)
            cubicTo(w*.54f,h*.14f,w*.56f,h*.32f,w*.62f,h*.32f)
            lineTo(w,h*.32f)
            lineTo(w,h);lineTo(0f,h);close()
        }
        p.style=Paint.Style.FILL;p.color=0xfffff9ed.toInt();c.drawPath(edge,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=1.35f*d;p.color=0xffd6ac60.toInt()
        val top=Path().apply{
            moveTo(0f,h*.32f);lineTo(w*.38f,h*.32f)
            cubicTo(w*.44f,h*.32f,w*.46f,h*.14f,w*.5f,h*.07f)
            cubicTo(w*.54f,h*.14f,w*.56f,h*.32f,w*.62f,h*.32f)
            lineTo(w,h*.32f)
        }
        c.drawPath(top,p)
        p.strokeWidth=.65f*d;p.color=0x88b98a48.toInt()
        c.save();c.translate(0f,3.6f*d);c.drawPath(top,p);c.restore()
        p.style=Paint.Style.FILL;p.color=0xffc99b49.toInt()
        val cx=w*.5f;val cy=h*.48f
        val gem=Path().apply{moveTo(cx,cy-6*d);lineTo(cx+5*d,cy);lineTo(cx,cy+6*d);lineTo(cx-5*d,cy);close()}
        c.drawPath(gem,p)
        p.color=0xfffbf1d7.toInt();c.drawCircle(cx,cy,1.7f*d,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=.65f*d;p.color=0x66bc9656
        val spacing=23f*d
        var x=12f*d
        while(x<w){
            if(kotlin.math.abs(x-cx)>12*d){
                val yy=h*.56f
                c.drawCircle(x,yy,1.65f*d,p)
                c.drawLine(x-5*d,yy,x-2.7f*d,yy,p)
                c.drawLine(x+2.7f*d,yy,x+5*d,yy,p)
            }
            x+=spacing
        }
        p.style=Paint.Style.FILL
    }
    override fun setAlpha(alpha:Int){p.alpha=alpha}
    override fun setColorFilter(filter:android.graphics.ColorFilter?){p.colorFilter=filter}
    override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}

/** Time-specific vector icon: sun, horizon, moon and stars; no static/faked times. */
class AliebaPrayerSymbolView(c:Context,private val key:String):View(c){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c:Canvas){
        super.onDraw(c)
        val d=resources.displayMetrics.density
        val cx=width*.5f;val cy=height*.5f;val r=kotlin.math.min(width,height)*.19f
        p.color=0xffffdb8c.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=1.45f*d
        p.strokeCap=Paint.Cap.ROUND
        fun line(x1:Float,y1:Float,x2:Float,y2:Float){c.drawLine(x1,y1,x2,y2,p)}
        fun sun(x:Float,y:Float,rad:Float){
            c.drawCircle(x,y,rad,p)
            for(i in 0..7){
                val a=i*Math.PI/4.0
                val dx=kotlin.math.cos(a).toFloat();val dy=kotlin.math.sin(a).toFloat()
                line(x+dx*rad*1.5f,y+dy*rad*1.5f,x+dx*rad*1.95f,y+dy*rad*1.95f)
            }
        }
        fun moon(x:Float,y:Float,rad:Float){
            p.style=Paint.Style.FILL
            val path=Path().apply{
                fillType=Path.FillType.EVEN_ODD
                addCircle(x,y,rad,Path.Direction.CW)
                addCircle(x+rad*.55f,y-rad*.24f,rad*.89f,Path.Direction.CW)
            }
            c.drawPath(path,p);p.style=Paint.Style.STROKE
        }
        when(key){
            "fajr"->{moon(cx-r*.5f,cy-r*.12f,r*.78f);line(cx-r*1.8f,cy+r*1.2f,cx+r*1.8f,cy+r*1.2f);c.drawCircle(cx+r*1.3f,cy-r*1.2f,r*.13f,p)}
            "sunrise"->{sun(cx,cy+r*.15f,r*.57f);line(cx-r*1.85f,cy+r*.95f,cx+r*1.85f,cy+r*.95f);line(cx,cy-r*1.4f,cx,cy-r*.78f)}
            "dhuhr"->sun(cx,cy,r*.8f)
            "sunset"->{sun(cx,cy+r*.4f,r*.55f);line(cx-r*1.8f,cy+r*.93f,cx+r*1.8f,cy+r*.93f);line(cx,cy-r*1.2f,cx,cy-r*.65f)}
            "maghrib"->{moon(cx-r*.22f,cy,r*.91f);c.drawCircle(cx+r*1.3f,cy-r*1.15f,r*.16f,p)}
            else->{moon(cx-r*.18f,cy,r*.94f);val x=cx+r*1.1f;val y=cy-r*1.1f;line(x-r*.31f,y,x+r*.31f,y);line(x,y-r*.31f,x,y+r*.31f)}
        }
        p.style=Paint.Style.FILL
    }
}

/** Ornamental native drawing; actual prayer times remain dynamic, never baked into an image. */
class AliebaPrayerCardDrawable(private val density:Float,private val active:Boolean):Drawable(){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val gold=0xffdfb66c.toInt()
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat()
        val rr=RectF(1.5f*density,3f*density,w-1.5f*density,h-1.5f*density)
        p.style=Paint.Style.FILL
        p.shader=LinearGradient(0f,0f,0f,h,
            if(active)0xff17665b.toInt() else 0xff0d304b.toInt(),
            if(active)0xff0b3f3c.toInt() else 0xff071e33.toInt(),Shader.TileMode.CLAMP)
        c.drawRoundRect(rr,13f*density,13f*density,p);p.shader=null
        p.style=Paint.Style.STROKE;p.strokeWidth=(if(active)2f else .9f)*density
        p.color=if(active)0xffffdd8f.toInt() else gold
        c.drawRoundRect(rr,13f*density,13f*density,p)
        p.color=0xb9dcb570.toInt();p.strokeWidth=.65f*density
        val inset=5f*density
        c.drawRoundRect(RectF(rr.left+inset,rr.top+inset,rr.right-inset,rr.bottom-inset),10f*density,10f*density,p)
        // Gold Islamic arch and a small centred diamond at the top.
        val cy=rr.top+8f*density
        val arch=Path().apply{
            moveTo(rr.left+9*density,cy+5*density)
            cubicTo(rr.left+15*density,cy+4*density,w*.37f,cy+4*density,w*.5f,cy-3*density)
            cubicTo(w*.63f,cy+4*density,rr.right-15*density,cy+4*density,rr.right-9*density,cy+5*density)
        }
        c.drawPath(arch,p)
        val diamond=Path().apply{moveTo(w*.5f,cy-5*density);lineTo(w*.5f+3*density,cy-2*density);lineTo(w*.5f,cy+1*density);lineTo(w*.5f-3*density,cy-2*density);close()}
        p.style=Paint.Style.FILL;p.color=0xffffdb8c.toInt();c.drawPath(diamond,p)
        if(active){p.style=Paint.Style.STROKE;p.strokeWidth=1.6f*density;p.color=0x99ffe1a1.toInt();c.drawRoundRect(RectF(rr.left-1*density,rr.top-1*density,rr.right+1*density,rr.bottom+1*density),14*density,14*density,p)}
        p.style=Paint.Style.FILL
    }
    override fun setAlpha(alpha:Int){p.alpha=alpha}
    override fun setColorFilter(filter:android.graphics.ColorFilter?){p.colorFilter=filter}
    override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}

/** A hairline gold frame only: never covers/dims the original mosque photograph. */
class AliebaHeroFrameDrawable(private val density:Float):Drawable(){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat()
        val m=7*density
        p.style=Paint.Style.STROKE;p.color=0xdbe9c987.toInt();p.strokeWidth=1.5f*density
        c.drawRoundRect(RectF(m,m,w-m,h-18*density),24*density,24*density,p)
        p.color=0x66fff8de;p.strokeWidth=.5f*density
        c.drawRoundRect(RectF(m+3*density,m+3*density,w-m-3*density,h-21*density),22*density,22*density,p)
        p.style=Paint.Style.FILL
    }
    override fun setAlpha(alpha:Int){p.alpha=alpha}
    override fun setColorFilter(filter:android.graphics.ColorFilter?){p.colorFilter=filter}
    override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}

/** Shared ivory/gold footer wave on the home screen and EVERY inner activity. */
class AliebaIvoryWaveDrawable(private val density:Float):Drawable(){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat();val a=19*density
        val path=Path().apply {
            moveTo(0f,a)
            cubicTo(w*.15f,a*.72f,w*.34f,a*.85f,w*.40f,a*.60f)
            cubicTo(w*.45f,a*.50f,w*.46f,0f,w*.50f,0f)
            cubicTo(w*.54f,0f,w*.55f,a*.50f,w*.60f,a*.60f)
            cubicTo(w*.72f,a*.90f,w*.85f,a*.72f,w,a)
            lineTo(w,h);lineTo(0f,h);close()
        }
        p.style=Paint.Style.FILL;p.color=0xfffffbf2.toInt();c.drawPath(path,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=1.1f*density;p.color=0xffd8b573.toInt()
        c.drawPath(Path().apply{
            moveTo(0f,a)
            cubicTo(w*.15f,a*.72f,w*.34f,a*.85f,w*.40f,a*.60f)
            cubicTo(w*.45f,a*.50f,w*.46f,0f,w*.50f,0f)
            cubicTo(w*.54f,0f,w*.55f,a*.50f,w*.60f,a*.60f)
            cubicTo(w*.72f,a*.90f,w*.85f,a*.72f,w,a)
        },p)
        p.style=Paint.Style.FILL
    }
    override fun setAlpha(alpha:Int){p.alpha=alpha}
    override fun setColorFilter(filter:android.graphics.ColorFilter?){p.colorFilter=filter}
    override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}

class AliebaGoldTileDrawable(private val density:Float):Drawable(){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat();val r=18*density
        val box=RectF(2*density,2*density,w-2*density,h-3*density)
        p.style=Paint.Style.FILL;p.shader=LinearGradient(0f,0f,w,h,0xfffffff8.toInt(),0xffe6d7ba.toInt(),Shader.TileMode.CLAMP)
        c.drawRoundRect(box,r,r,p);p.shader=null
        p.style=Paint.Style.STROKE;p.color=0xffd4ae67.toInt();p.strokeWidth=1.1f*density;c.drawRoundRect(box,r,r,p)
        p.style=Paint.Style.FILL
    }
    override fun setAlpha(alpha:Int){p.alpha=alpha}
    override fun setColorFilter(filter:android.graphics.ColorFilter?){p.colorFilter=filter}
    override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
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
            background=AliebaIvoryWaveDrawable(c.resources.displayMetrics.density)
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
                setColorFilter(if(selected)0xff146c7e.toInt() else 0xff81683f.toInt())
            },LinearLayout.LayoutParams(dp(c,22),dp(c,22)))
            v.addView(TextView(c).apply {
                text=title;textSize=9.5f;gravity=Gravity.CENTER
                setTextColor(if(selected)0xff195f74.toInt() else 0xff584736.toInt())
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
            elevation=dp(c,4).toFloat()
            setOnClickListener{open("ai")}
        }
        center.addView(ImageView(c).apply {
            setImageResource(R.drawable.alieba_gold_ai)
            scaleType=ImageView.ScaleType.FIT_CENTER
            contentDescription="Alieba köməkçi"
        },FrameLayout.LayoutParams(-1,-1))
        frame.addView(center,FrameLayout.LayoutParams(dp(c,76),dp(c,76),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {topMargin=dp(c,0)})
        return frame
    }
}
