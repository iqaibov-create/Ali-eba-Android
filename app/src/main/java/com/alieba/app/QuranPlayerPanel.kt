package com.alieba.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import java.net.URL

/** A persistent native verse-by-verse player above the footer. */
class QuranPlayerPanel(c:Context):LinearLayout(c) {
    private val dark=0xff16483f.toInt()
    private var ayahs=JSONArray()
    private var surah=1
    private var index=0
    private var media:MediaPlayer?=null
    private var prepared=false
    private val main=Handler(Looper.getMainLooper())
    private val title=TextView(c)
    private val play=TextView(c)
    private val seeker=SeekBar(c)
    private val timer=TextView(c)
    private var onVerse:((Int,Int)->Unit)?=null
    private fun dp(x:Int)=(x*resources.displayMetrics.density).toInt()
    private fun style(s:String,size:Float):TextView=TextView(context).apply{text=s;textSize=size;setTextColor(dark);gravity=Gravity.CENTER}
    init {
        orientation=VERTICAL
        background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadii=floatArrayOf(dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),0f,0f,0f,0f)}
        elevation=dp(6).toFloat()
        setPadding(dp(12),dp(7),dp(12),dp(4))
        val head=LinearLayout(c).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        title.apply{textSize=12f;typeface=Typeface.DEFAULT_BOLD;setTextColor(dark);text="Quran qiraəti";isSingleLine=true;ellipsize=android.text.TextUtils.TruncateAt.END}
        head.addView(title,LayoutParams(0,dp(25),1f))
        timer.apply{text="00:00 / 00:00";textSize=11f;setTextColor(0xff6b7b76.toInt())}
        head.addView(timer);addView(head)
        val controls=LinearLayout(c).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        val prev=style("⏮",24f).apply{contentDescription="Əvvəlki ayə";setOnClickListener{if(ayahs.length()>0)playAt((index-1).coerceAtLeast(0))}}
        controls.addView(prev,LayoutParams(dp(40),dp(42)))
        play.apply{text="▶";textSize=26f;gravity=Gravity.CENTER;setTextColor(dark);contentDescription="Oxut və ya dayandır";setOnClickListener{toggle()}}
        controls.addView(play,LayoutParams(dp(46),dp(42)))
        val next=style("⏭",24f).apply{contentDescription="Növbəti ayə";setOnClickListener{if(index+1<ayahs.length())playAt(index+1)}}
        controls.addView(next,LayoutParams(dp(40),dp(42)))
        seeker.max=1000
        seeker.progressTintList=android.content.res.ColorStateList.valueOf(0xff2e9580.toInt())
        seeker.thumbTintList=android.content.res.ColorStateList.valueOf(0xff2e9580.toInt())
        controls.addView(seeker,LayoutParams(0,dp(38),1f))
        addView(controls)
        seeker.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(s:SeekBar?){}
            override fun onStopTrackingTouch(s:SeekBar?){if(prepared)runCatching{media?.let{m->if(m.duration>0)m.seekTo((m.duration.toLong()*seeker.progress/1000).toInt())}}}
            override fun onProgressChanged(s:SeekBar?,value:Int,fromUser:Boolean){}
        })
        visibility=View.GONE
        main.post(object:Runnable {override fun run(){
            if(prepared){runCatching{media?.let {m->if(m.duration>0){seeker.progress=(m.currentPosition.toLong()*1000/m.duration).toInt();timer.text="${fmt(m.currentPosition)} / ${fmt(m.duration)}"}}}}
            main.postDelayed(this,450L)
        }})
    }
    private fun fmt(ms:Int):String="%02d:%02d".format(java.util.Locale.US,ms/60000,(ms/1000)%60)
    fun bind(surahNumber:Int,verses:JSONArray,lastAyah:Int=1,verseChange:((Int,Int)->Unit)?=null){
        releasePlayer();surah=surahNumber;ayahs=verses;onVerse=verseChange
        index=(0 until verses.length()).firstOrNull{verses.optJSONObject(it)?.optInt("number")==lastAyah}?:0
        visibility=if(verses.length()>0)View.VISIBLE else View.GONE
        showTitle()
    }
    private fun showTitle(){val n=ayahs.optJSONObject(index)?.optInt("number")?:0
        title.text="${surah}. surə · $n. ayə";play.text=if(prepared&&media?.isPlaying==true)"Ⅱ" else "▶"
    }
    private fun safeMedia(raw:String):String?=try{val u=URL(raw)
        if(u.protocol=="https"&&u.host in setOf("cdn.islamic.network","audio.alquran.cloud","api.alquran.cloud","everyayah.com","alieba.ge"))u.toString() else null
    }catch(_:Exception){null}
    fun playAt(pos:Int){
        val verse=ayahs.optJSONObject(pos)?:return
        val url=safeMedia(verse.optString("audio"))?:run{Toast.makeText(context,"Bu ayənin audio faylı yoxdur",Toast.LENGTH_SHORT).show();return}
        releasePlayer();index=pos;seeker.progress=0;timer.text="00:00 / 00:00";title.text="${surah}. surə · ${verse.optInt("number")}. ayə · yüklənir"
        onVerse?.invoke(surah,verse.optInt("number"))
        val m=MediaPlayer();media=m
        try {
            m.setDataSource(url)
            m.setOnPreparedListener {ready-> if(media===ready){prepared=true;ready.start();showTitle()} }
            m.setOnCompletionListener {ready->if(media===ready){if(index+1<ayahs.length())playAt(index+1) else{releasePlayer();showTitle()}}}
            m.setOnErrorListener {ready,_,_->if(media===ready){releasePlayer();showTitle();Toast.makeText(context,"Qiraət yüklənmədi",Toast.LENGTH_SHORT).show()};true}
            m.prepareAsync()
        }catch(_:Exception){releasePlayer();showTitle();Toast.makeText(context,"Qiraət açıla bilmədi",Toast.LENGTH_SHORT).show()}
    }
    private fun toggle(){if(ayahs.length()==0)return
        if(!prepared){if(media==null)playAt(index);return}
        runCatching {media?.let{if(it.isPlaying)it.pause() else it.start()}}
        showTitle()
    }
    private fun releasePlayer(){val m=media;media=null;prepared=false
        if(m!=null)runCatching{m.reset();m.release()}
        play.text="▶"
    }
    fun stopAndHide(){releasePlayer();visibility=View.GONE;ayahs=JSONArray()}
    fun close(){stopAndHide();main.removeCallbacksAndMessages(null)}
}
