package com.alieba.app

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.hardware.*
import android.hardware.GeomagneticField
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.*

/** Native, sensor-based Qibla. Needs device rotation sensor and a saved location. */
class QiblaActivity:Activity(),SensorEventListener {
    private val green=0xff16483f.toInt()
    private lateinit var manager:SensorManager
    private lateinit var dial:QiblaDial
    private lateinit var info:TextView
    private var target:Float?=null
    private var heading=0f
    private var declination=0f
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        window.statusBarColor=0xfff5f8f6.toInt();window.navigationBarColor=Color.WHITE
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        manager=getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val pref=getSharedPreferences("alieba_setup",MODE_PRIVATE)
        val lat=pref.getString("latitude",null)?.toDoubleOrNull()
        val lon=pref.getString("longitude",null)?.toDoubleOrNull()
        if(lat!=null&&lon!=null&&lat in -90.0..90.0&&lon in -180.0..180.0){
            val phi1=Math.toRadians(lat);val phi2=Math.toRadians(21.422487);val delta=Math.toRadians(39.826206-lon)
            val bearing=Math.toDegrees(atan2(sin(delta),cos(phi1)*tan(phi2)-sin(phi1)*cos(delta)))
            target=((bearing+360.0)%360.0).toFloat()
            declination=GeomagneticField(lat.toFloat(),lon.toFloat(),0f,System.currentTimeMillis()).declination
        }
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=AliebaPatternDrawable(resources.displayMetrics.density)}
        val bar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(8),dp(14),dp(8));setBackgroundColor(Color.WHITE)}
        bar.addView(TextView(this).apply{text="‹";textSize=36f;setTextColor(green);gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(45),dp(49)))
        bar.addView(TextView(this).apply{text="Qiblə kompası";textSize=22f;typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(green)},LinearLayout.LayoutParams(-1,-2))
        root.addView(bar)
        val col=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(18),dp(20),dp(18),dp(20))}
        col.addView(TextView(this).apply{text="Kəbə istiqamətini tap";textSize=20f;typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(green);gravity=Gravity.CENTER})
        dial=QiblaDial(this);col.addView(dial,LinearLayout.LayoutParams(-1,dp(330)))
        info=TextView(this).apply{setTextColor(green);textSize=16f;gravity=Gravity.CENTER;setPadding(0,dp(10),0,dp(14))}
        col.addView(info)
        col.addView(TextView(this).apply{text="Telefonu üfüqi saxlayın. İstiqamət səhvdirsə, telefonu 8 şəklində hərəkət etdirib kompası kalibrləyin. Metal əşyalardan uzaq durun.";textSize=13f;setTextColor(0xff64736d.toInt());gravity=Gravity.CENTER})
        root.addView(col,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(AliebaBottomNav.make(this,"qibla"),LinearLayout.LayoutParams(-1,dp(87)))
        setContentView(root)
        info.text=if(target==null)"Məkan hələ seçilməyib. Ayarlardan şəhəri seçin." else "Qiblə: ${target!!.roundToInt()}° · Kompas hazırlanır…"
    }
    override fun onResume(){super.onResume()
        val sensor=manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if(sensor==null){info.text="Bu cihazda kompas sensoru mövcud deyil.";return}
        manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_UI)
    }
    override fun onPause(){manager.unregisterListener(this);super.onPause()}
    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
    override fun onSensorChanged(event:SensorEvent){
        if(event.sensor.type!=Sensor.TYPE_ROTATION_VECTOR)return
        val mat=FloatArray(9);val orient=FloatArray(3)
        SensorManager.getRotationMatrixFromVector(mat,event.values)
        SensorManager.getOrientation(mat,orient)
        val degrees=((Math.toDegrees(orient[0].toDouble()).toFloat()+declination+360f)%360f)
        val diff=((degrees-heading+540f)%360f)-180f
        heading=(heading+diff*.18f+360f)%360f
        dial.invalidate()
        target?.let{val difference=((it-heading+540f)%360f)-180f
            info.text="Qiblə ${it.roundToInt()}° · İstiqamət ${heading.roundToInt()}° · ${if(abs(difference)<7)"Qibləyə yönəlmisiniz" else if(difference>0)"${abs(difference).roundToInt()}° sağa dönün" else "${abs(difference).roundToInt()}° sola dönün"}"
        }
    }
    private inner class QiblaDial(c:Context):View(c){
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas:Canvas){super.onDraw(canvas)
            val cx=width/2f;val cy=height/2f;val radius=min(width,height)*.39f
            p.color=Color.WHITE;canvas.drawCircle(cx,cy,radius+dp(7),p)
            p.color=0xffe0e9e4.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=dp(2).toFloat();canvas.drawCircle(cx,cy,radius,p);p.style=Paint.Style.FILL
            for(i in 0 until 36){val angle=Math.toRadians((i*10-heading).toDouble()-90);val x=cos(angle).toFloat();val y=sin(angle).toFloat()
                p.color=if(i==0)0xffc79b53.toInt() else 0xff93ada3.toInt();p.strokeWidth=if(i%9==0)dp(3).toFloat() else dp(1).toFloat()
                canvas.drawLine(cx+(radius-dp(12))*x,cy+(radius-dp(12))*y,cx+radius*x,cy+radius*y,p)
            }
            p.color=green;p.typeface=Typeface.DEFAULT_BOLD;p.textAlign=Paint.Align.CENTER;p.textSize=dp(17).toFloat()
            val north=Math.toRadians((-heading-90f).toDouble())
            canvas.drawText("Ş",cx+cos(north).toFloat()*(radius-dp(32)),cy+sin(north).toFloat()*(radius-dp(32))+dp(6),p)
            val a=Math.toRadians((((target?:0f)-heading-90f)+360f).toDouble())
            val dx=cos(a).toFloat();val dy=sin(a).toFloat()
            p.color=if(target==null)0xffbecbc5.toInt() else 0xffc5a05d.toInt()
            p.strokeWidth=dp(7).toFloat();p.strokeCap=Paint.Cap.ROUND
            canvas.drawLine(cx,cy,cx+dx*radius*.67f,cy+dy*radius*.67f,p)
            val tipX=cx+dx*radius*.79f;val tipY=cy+dy*radius*.79f
            val arrow=Path().apply{moveTo(tipX,tipY);lineTo(tipX-dx*dp(25)-dy*dp(11),tipY-dy*dp(25)+dx*dp(11));lineTo(tipX-dx*dp(25)+dy*dp(11),tipY-dy*dp(25)-dx*dp(11));close()}
            canvas.drawPath(arrow,p)
            p.color=green;canvas.drawCircle(cx,cy,dp(12).toFloat(),p)
            p.color=Color.WHITE;p.textSize=dp(19).toFloat();canvas.drawText("☪",cx,cy+dp(6),p)
        }
    }
}
