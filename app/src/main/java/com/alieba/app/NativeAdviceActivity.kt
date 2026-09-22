package com.alieba.app

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class NativeAdviceActivity:Activity(){
    private val ink=0xff1b443c.toInt()
    private lateinit var content:LinearLayout
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun rounded(color:Int=Color.WHITE)=GradientDrawable().apply{setColor(color);cornerRadius=dp(19).toFloat()}
    private fun tv(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(ink);if(bold)typeface=Typeface.DEFAULT_BOLD}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,true)
        window.statusBarColor=0xfff5f8f6.toInt();window.navigationBarColor=Color.WHITE
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=AliebaPatternDrawable(resources.displayMetrics.density)}
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(8))}
        top.addView(tv("‹",34f).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(50),dp(48)))
        top.addView(tv("Dini sual və məsləhət",21f,true),LinearLayout.LayoutParams(0,-2,1f))
        root.addView(top)
        val scroll=ScrollView(this);content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(13),dp(18),dp(24))}
        scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(AliebaBottomNav.make(this,"advice"),LinearLayout.LayoutParams(-1,dp(87)))
        setContentView(root);refresh()
    }
    private fun add(s:String,size:Float=15f,bold:Boolean=false){content.addView(tv(s,size,bold),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})}
    private fun refresh(){
        content.removeAllViews()
        add("Sualını paylaş",25f,true)
        add("Sualınız yoxlanıldıqdan sonra cavablandırılacaq. Cavablar dini məsləhətdir; təcili tibbi, hüquqi və ya fərdi dini hökm əvəzi deyil.")
        val name=EditText(this).apply{hint="Adınız və ya ləqəbiniz";setSingleLine(true);setPadding(dp(16),dp(12),dp(16),dp(12));background=rounded()}
        content.addView(name,LinearLayout.LayoutParams(-1,dp(53)).apply{bottomMargin=dp(10)})
        val question=EditText(this).apply{hint="Dini sualınızı yazın…";minLines=3;maxLines=7;gravity=Gravity.TOP;setPadding(dp(16),dp(12),dp(16),dp(12));background=rounded()}
        content.addView(question,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
        val send=tv("Sualı göndər",16f,true).apply{gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=rounded(0xff146451.toInt());setOnClickListener{
            val n=name.text.toString().trim();val q=question.text.toString().trim()
            if(n.length !in 2..60 || q.length !in 12..1500){Toast.makeText(this@NativeAdviceActivity,"Ad 2–60, sual 12–1500 simvol olmalıdır",Toast.LENGTH_LONG).show();return@setOnClickListener}
            isEnabled=false
            request("POST", "name="+URLEncoder.encode(n,"UTF-8")+"&question="+URLEncoder.encode(q,"UTF-8")){ result ->
                isEnabled=true
                if(result?.optBoolean("ok")==true){question.setText("");Toast.makeText(this@NativeAdviceActivity,"Sualınız yoxlamaya göndərildi",Toast.LENGTH_LONG).show()}
                else Toast.makeText(this@NativeAdviceActivity,"Göndərilmədi: "+(result?.optString("error")?:"Bağlantını yoxlayın"),Toast.LENGTH_LONG).show()
            }
        }}
        content.addView(send,LinearLayout.LayoutParams(-1,dp(53)).apply{bottomMargin=dp(24)})
        add("Cavablandırılmış suallar",21f,true)
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};content.addView(list)
        list.addView(tv("Yüklənir…"))
        request("GET",null){response->
            list.removeAllViews()
            if(response?.optBoolean("ok")!=true){list.addView(tv("Suallar yüklənmədi. İnternet bağlantısını yoxlayın."));return@request}
            val items=response.optJSONArray("items")
            if(items==null||items.length()==0){list.addView(tv("Hələ cavablandırılmış sual yoxdur."));return@request}
            for(i in 0 until items.length()){
                val x=items.optJSONObject(i)?:continue
                val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(15),dp(15),dp(15));background=rounded()}
                card.addView(tv(x.optString("name"),12f,true))
                card.addView(tv(x.optString("question"),16f,true).apply{setPadding(0,dp(9),0,dp(9))})
                card.addView(tv("Cavab: "+x.optString("answer"),15f))
                list.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(11)})
            }
        }
    }
    private fun request(method:String,form:String?,done:(JSONObject?)->Unit){
        Thread {
            val result=try{
                val c=URL("https://alieba.ge/api/advice.php").openConnection() as HttpURLConnection
                c.connectTimeout=10000;c.readTimeout=12000;c.requestMethod=method
                if(form!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");c.outputStream.use{it.write(form.toByteArray(Charsets.UTF_8))}}
                val text=(if(c.responseCode in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}?:"{}"
                c.disconnect();JSONObject(text)
            }catch(_:Exception){null}
            runOnUiThread{if(!isFinishing&&!isDestroyed)done(result)}
        }.start()
    }
}
