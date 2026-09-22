package com.alieba.app

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NativeDonateActivity:Activity(){
 private val ink=0xff1c463a.toInt()
 private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
 private fun tv(s:String,size:Float,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(ink);if(bold)typeface=Typeface.DEFAULT_BOLD}
 override fun onCreate(b:Bundle?){super.onCreate(b)
   androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,true)
   window.statusBarColor=0xfff5f8f6.toInt();window.navigationBarColor=Color.WHITE
   window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
   val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=AliebaPatternDrawable(resources.displayMetrics.density)}
   val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(11),dp(14),dp(11))}
   header.addView(tv("‹",35f).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(48),dp(50)))
   header.addView(tv("Alieba-ya dəstək",23f,true));root.addView(header)
   val scroll=ScrollView(this);val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(24),dp(35),dp(24),dp(22))}
   scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
   root.addView(AliebaBottomNav.make(this,"donate"),LinearLayout.LayoutParams(-1,dp(87)))
   setContentView(root)
   body.addView(tv("☪",46f,true).apply{gravity=Gravity.CENTER})
   body.addView(tv("Xeyir işinə dəstək ol",25f,true).apply{gravity=Gravity.CENTER;setPadding(0,dp(6),0,dp(18))})
   body.addView(tv("Alieba-nın dini məzmunlarını və inkişafını könüllü dəstəyinizlə yaşada bilərsiniz. Ödəniş üçün məbləği seçin.",15f).apply{gravity=Gravity.CENTER;setPadding(0,0,0,dp(23))})
   val status=tv("Ödəniş seçimləri yüklənir…",13f).apply{gravity=Gravity.CENTER}
   val cards=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};body.addView(cards);body.addView(status)
   Thread{
     val urls=try{val c=URL("https://alieba.ge/api/donation-options.php").openConnection() as HttpURLConnection
       c.connectTimeout=9000;c.readTimeout=10000;val j=JSONObject(c.inputStream.bufferedReader().use{it.readText()});c.disconnect();j
     }catch(_:Exception){null}
     runOnUiThread{
       if(isFinishing||isDestroyed)return@runOnUiThread
       status.text=if(urls?.optBoolean("ok")==true)"Ödəniş Android tərəfindən təsdiqlənir; heç bir məbləğ avtomatik tutulmur." else "Bağlantı əlçatan deyil. Sonra yenidən yoxlayın."
       for(amount in listOf(10,20,30)){
         val link=urls?.optJSONObject("links")?.optString(amount.toString(),"")?:""
         val card=tv("$${amount}  ·  Dəstək ol     ›",22f,true).apply{
           setPadding(dp(20),dp(22),dp(20),dp(22));background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=dp(18).toFloat()}
           setOnClickListener{
             val uri=Uri.parse(link)
             if(uri.scheme=="https" && !uri.host.isNullOrBlank()) startActivity(Intent(Intent.ACTION_VIEW,uri))
             else AlertDialog.Builder(this@NativeDonateActivity).setMessage("Bu məbləğ üçün təhlükəsiz ödəniş bağlantısı hələ qurulmayıb.").setPositiveButton("Bağla",null).show()
           }
         }
         cards.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(13)})
       }
     }
   }.start()
 }
}
