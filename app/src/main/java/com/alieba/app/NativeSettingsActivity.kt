package com.alieba.app

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.firebase.messaging.FirebaseMessaging

/** Native settings and permission controls; toggles reflect real app preferences. */
class NativeSettingsActivity: Activity() {
 private val ink=0xff183e37.toInt()
 private val prefs by lazy{getSharedPreferences("alieba_setup",MODE_PRIVATE)}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun bg(c:Int,r:Int=17)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
 private fun text(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(ink);if(bold)typeface=Typeface.DEFAULT_BOLD}
 override fun onCreate(b:Bundle?){super.onCreate(b)
  androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,true)
  window.statusBarColor=0xfff7f8fa.toInt();window.navigationBarColor=Color.WHITE
  window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  draw()
 }
 private fun draw(){
  val scroll=ScrollView(this).apply{setBackgroundColor(0xfff7f8fa.toInt())}
  val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(20),dp(18),dp(30))};scroll.addView(body)
  val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  top.addView(text("‹",35f).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(45),dp(52)))
  top.addView(text("Tətbiq ayarları",24f,true).apply{gravity=Gravity.CENTER_VERTICAL})
  body.addView(top)
  body.addView(text("AZAN VƏ BİLDİRİŞLƏR",13f,true).apply{setPadding(0,dp(23),0,dp(11))})
  fun toggle(label:String,desc:String,value:Boolean,change:(Boolean)->Unit){
   val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(13),dp(14),dp(13));background=bg(Color.WHITE)}
   val labels=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
   labels.addView(text(label,15f,true))
   labels.addView(text(desc,12f).apply{setTextColor(0xff838d98.toInt())})
   row.addView(labels,LinearLayout.LayoutParams(0,-2,1f))
   row.addView(Switch(this).apply{isChecked=value;setOnCheckedChangeListener {_,on->change(on)}},LinearLayout.LayoutParams(-2,-2))
   body.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
  }
  fun option(label:String,action:()->Unit){
   body.addView(text(label+"     ›",15f,true).apply{setPadding(dp(15),dp(17),dp(15),dp(17));background=bg(Color.WHITE);setOnClickListener{action()}},LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
  }
  toggle("Azan bildirişləri","Bütün namaz vaxtları",prefs.getBoolean("notifications",false)) {on->
   prefs.edit().putBoolean("notifications",on).apply()
   if(on&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),42)}
   PrayerClock.scheduleToday(this)
  }
  for(name in listOf("Fəcr","Zöhr","Əsr","Məğrib","İşa"))toggle("$name azanı","Səsi aç / bağla",AzanPrefs.isPrayerEnabled(this,name)){on->AzanPrefs.setPrayerEnabled(this,name,on);PrayerClock.scheduleToday(this)}
  toggle("Yenilik bildirişləri","Sayta yeni xəbər əlavə olunanda",prefs.getBoolean("news_notifications",true)){on->
   prefs.edit().putBoolean("news_notifications",on).apply()
   if(on) FirebaseMessaging.getInstance().subscribeToTopic("alieba_news") else FirebaseMessaging.getInstance().unsubscribeFromTopic("alieba_news")
  }
  option("Azan səsi seç / telefondan əlavə et"){
   AlertDialog.Builder(this).setTitle("Azan səsi").setItems(arrayOf("Şiə azanı","Azan 1","Azan 2","Telefondan seç")){_,i->
    when(i){0->AzanPrefs.setSound(this,"shia");1->AzanPrefs.setSound(this,"beautiful1");2->AzanPrefs.setSound(this,"beautiful2");3->{
     val pick=Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
     startActivityForResult(pick,98)
    }}
   }.show()
  }
  option("Bildirişləri yuxarıda və kilid ekranında göstər"){
   startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))
  }
  option("Dəqiq siqnal icazəsi"){
   if(Build.VERSION.SDK_INT>=31&&!getSystemService(AlarmManager::class.java).canScheduleExactAlarms())startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:$packageName")))
   else Toast.makeText(this,"Dəqiq siqnal icazəsi aktivdir",Toast.LENGTH_SHORT).show()
  }
  option("Batareya məhdudiyyətləri"){
   startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
  }
  option("Namaz vaxtlarını yenilə"){
   PrayerClock.fetchAndSchedule(this){ok->Toast.makeText(this,if(ok)"Namaz vaxtları yeniləndi" else "Vaxtlar yüklənmədi; internet və məkanı yoxlayın",Toast.LENGTH_LONG).show()}
  }
  body.addView(text("DİL VƏ MƏKAN",13f,true).apply{setPadding(0,dp(25),0,dp(10))})
  option("Dil: "+(prefs.getString("language","az")?:"az")){
   val langs=arrayOf("Azərbaycan dili","Türkçe","Русский","ქართული")
   val codes=arrayOf("az","tr","ru","ka")
   AlertDialog.Builder(this).setTitle("Dil seç").setItems(langs){_,i->prefs.edit().putString("language",codes[i]).apply();draw()}.show()
  }
  option("Məkan və şəhər"){
   val edit=EditText(this).apply{hint="Şəhər (məsələn, Marneuli)";setSingleLine(true)}
   AlertDialog.Builder(this).setTitle("Şəhər seç").setView(edit).setPositiveButton("Yadda saxla"){_,_->
    val city=edit.text.toString().trim()
    if(city.isNotEmpty())Thread{
     val loc=try{@Suppress("DEPRECATION") val a=android.location.Geocoder(this).getFromLocationName(city,1);a?.firstOrNull()}catch(_:Exception){null}
     runOnUiThread{
      if(loc==null)Toast.makeText(this,"Şəhər tapılmadı",Toast.LENGTH_LONG).show()
      else{prefs.edit().putString("city",city).putString("latitude",loc.latitude.toString()).putString("longitude",loc.longitude.toString()).apply();PrayerClock.fetchAndSchedule(this);Toast.makeText(this,"Şəhər: $city",Toast.LENGTH_SHORT).show()}
     }
    }.start()
   }.setNegativeButton("Ləğv et",null).show()
  }
  setContentView(scroll)
 }
 @Deprecated("File chooser compatibility")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==98&&resultCode==RESULT_OK&&data?.data!=null){
   val uri=data.data!!
   try{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){}
   getSharedPreferences("azan_settings",MODE_PRIVATE).edit().putString("custom_uri",uri.toString()).apply()
   AzanPrefs.setSound(this,"custom")
   Toast.makeText(this,"Azan səsi seçildi",Toast.LENGTH_SHORT).show()
  }
 }
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults)
  if(requestCode==42){prefs.edit().putBoolean("notifications",grantResults.any{it==PackageManager.PERMISSION_GRANTED}).apply();PrayerClock.scheduleToday(this);draw()}
 }
}
