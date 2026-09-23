package com.alieba.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

/** Device-local dhikr counter; no account or internet required. */
class ZikrActivity: Activity() {
    private val green=0xff16483f.toInt()
    private val gold=0xffc6a35a.toInt()
    private val dhikr=listOf("Sübhanallah", "Əlhəmdulillah", "Allahu əkbər", "Allahummə səlli əla Muhəmməd və ali Muhəmməd", "Əstəğfirullah")
    private val prefs by lazy {getSharedPreferences("alieba_zikr_v17",MODE_PRIVATE)}
    private var selected=0
    private var goal=33
    private var count=0
    private lateinit var number:TextView
    private lateinit var progress:TextView
    private lateinit var label:TextView
    private lateinit var beads:BeadsView
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun rounded(c:Int,r:Int=18)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
    private fun text(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(green);gravity=Gravity.CENTER;if(bold)typeface=android.graphics.Typeface.DEFAULT_BOLD}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        window.statusBarColor=0xfff5f8f6.toInt();window.navigationBarColor=android.graphics.Color.WHITE
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        selected=prefs.getInt("selection",0).coerceIn(0,dhikr.lastIndex)
        goal=prefs.getInt("goal",33).coerceAtLeast(1)
        count=prefs.getInt("count_$selected",0)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=AliebaPatternDrawable(resources.displayMetrics.density)}
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(17),dp(8),dp(17),dp(8));setBackgroundColor(android.graphics.Color.WHITE)}
        top.addView(text("‹",36f).apply{setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(42),dp(48)))
        top.addView(text("Zikr və təsbeh",22f,true).apply{gravity=Gravity.CENTER_VERTICAL},LinearLayout.LayoutParams(0,dp(48),1f))
        root.addView(top)
        val scroll=ScrollView(this)
        val column=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(17),dp(16),dp(17),dp(20))}
        column.addView(text("Hər zikr bir xatırlamadır",15f))
        label=text(dhikr[selected],18f,true).apply{setPadding(dp(10),dp(15),dp(10),dp(15));background=rounded(android.graphics.Color.WHITE);setOnClickListener{chooseDhikr()}}
        column.addView(label,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(15)})
        beads=BeadsView(this)
        column.addView(beads,LinearLayout.LayoutParams(-1,dp(255)).apply{topMargin=dp(10)})
        number=text("$count",51f,true)
        column.addView(number)
        progress=text("Hədəf: $goal · Qalan: ${(goal-count%goal)%goal}",14f)
        column.addView(progress,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(18)})
        val tap=text("ZİKR ET  ✦",23f,true).apply{
            setTextColor(android.graphics.Color.WHITE);background=rounded(green,25);elevation=dp(3).toFloat()
            setOnClickListener{count++;prefs.edit().putInt("count_$selected",count).apply();performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);updateCount()}
        }
        column.addView(tap,LinearLayout.LayoutParams(-1,dp(82)))
        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(0,dp(13),0,0)}
        fun action(s:String,onClick:()->Unit){row.addView(text(s,15f,true).apply{background=rounded(android.graphics.Color.WHITE);setOnClickListener{onClick()}},LinearLayout.LayoutParams(0,dp(56),1f).apply{rightMargin=dp(7)})}
        action("Zikri seç"){chooseDhikr()}
        action("Hədəf"){AlertDialog.Builder(this).setTitle("Zikr hədəfi").setItems(arrayOf("33", "99", "100", "1000")){_,n->goal=intArrayOf(33,99,100,1000)[n];prefs.edit().putInt("goal",goal).apply();updateCount()}.show()}
        action("Sıfırla"){AlertDialog.Builder(this).setMessage("Bu zikrin sayını sıfırlamaq istəyirsiniz?").setPositiveButton("Sıfırla"){_,_->count=0;prefs.edit().putInt("count_$selected",0).apply();updateCount()}.setNegativeButton("Ləğv et",null).show()}
        column.addView(row,LinearLayout.LayoutParams(-1,-2))
        scroll.addView(column);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(AliebaBottomNav.make(this,"zikr"),LinearLayout.LayoutParams(-1,dp(87)))
        setContentView(root);updateCount()
    }
    private fun chooseDhikr(){AlertDialog.Builder(this).setTitle("Zikr seçin").setItems(dhikr.toTypedArray()){_,i->
        selected=i;count=prefs.getInt("count_$selected",0);prefs.edit().putInt("selection",selected).apply();label.text=dhikr[selected];updateCount()
    }.show()}
    private fun updateCount(){number.text=count.toString();progress.text="Hədəf: $goal · Qalan: ${(goal-count%goal)%goal}";beads.invalidate()}
    private inner class BeadsView(context:android.content.Context):View(context){
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c:Canvas){super.onDraw(c)
            val cx=width/2f;val cy=height/2f;val radius=kotlin.math.min(width,height)*.32f
            val steps=33;val filled=count%goal.toDouble()/goal*steps
            for(i in 0 until steps){
                val angle=(i.toFloat()/steps*2*Math.PI-Math.PI/2)
                val x=cx+radius*kotlin.math.cos(angle).toFloat();val y=cy+radius*kotlin.math.sin(angle).toFloat()
                p.color=if(i<filled)0xff168a76.toInt() else 0xffd4dfd9.toInt();c.drawCircle(x,y,dp(6).toFloat(),p)
                p.color=if(i<filled)0xffa8e9cb.toInt() else android.graphics.Color.WHITE;c.drawCircle(x-dp(1),y-dp(2),dp(1.4f),p)
            }
            p.color=0xffedf5ef.toInt();c.drawCircle(cx,cy,radius*.66f,p)
            p.color=gold;p.textSize=dp(27).toFloat();p.textAlign=Paint.Align.CENTER;p.typeface=android.graphics.Typeface.DEFAULT_BOLD
            c.drawText("☪",cx,cy+dp(10),p)
        }
    }
    private fun dp(n:Float)=(n*resources.displayMetrics.density)
}
