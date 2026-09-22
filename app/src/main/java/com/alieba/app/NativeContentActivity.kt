package com.alieba.app

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Native reading screens. Only content comes from alieba.ge; no WebView is used. */
class NativeContentActivity:Activity() {
    private val ink=0xff123d36.toInt()
    private val muted=0xff71817d.toInt()
    private val bg=0xfff7f8fa.toInt()
    private val gold=0xffb79150.toInt()
    private lateinit var body:LinearLayout
    private lateinit var heading:TextView
    private var section="mafatih"
    private var categoryId=0
    private var categories=JSONArray()
    private var entries=JSONArray()
    private var query=""
    private var audio:MediaPlayer?=null
    private var playingButton:TextView?=null
    private val favorites by lazy {getSharedPreferences("alieba_saved_native",Context.MODE_PRIVATE)}
    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,true)
        window.statusBarColor=bg
        window.navigationBarColor=Color.WHITE
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        section=intent.getStringExtra("section")?.takeIf {it in setOf("quran","mafatih","ahkam","hadis","mersiye","kitabxana","news","saved")}?:"mafatih"
        baseScreen(titleFor(section))
        when(section){"quran"->surahList();"news"->loadNews();"saved"->showSaved();else->loadContent()}
    }
    override fun onDestroy(){stopAudio();super.onDestroy()}
    private fun dp(x:Int)=(x*resources.displayMetrics.density).toInt()
    private fun shape(color:Int,r:Int=18)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
    private fun text(t:String,size:Float=16f,color:Int=ink,bold:Boolean=false)=TextView(this).apply{
        this.text=t;textSize=size;setTextColor(color);if(bold)typeface=Typeface.DEFAULT_BOLD
    }
    private fun titleFor(s:String)=when(s){"quran"->"Quran";"mafatih"->"Məfatih";"ahkam"->"Əhkam";"hadis"->"Hədislər";"mersiye"->"Mərsiyələr";"kitabxana"->"Kitabxana";"news"->"Yeniliklər";"saved"->"Yadda saxlananlar";else->"Alieba"}
    private fun baseScreen(title:String){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=AliebaPatternDrawable(resources.displayMetrics.density)}
        val bar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(10),dp(18),dp(10));setBackgroundColor(Color.WHITE)}
        bar.addView(text("‹",36f,ink).apply{gravity=Gravity.CENTER;setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(44),dp(48)))
        heading=text(title,23f,ink,true).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),0,0,0)}
        bar.addView(heading,LinearLayout.LayoutParams(0,dp(50),1f))
        bar.addView(text("☪",25f,gold),LinearLayout.LayoutParams(-2,-2))
        root.addView(bar,LinearLayout.LayoutParams(-1,-2))
        val scroll=ScrollView(this).apply{isFillViewport=true;clipToPadding=false;background=AliebaPatternDrawable(resources.displayMetrics.density)}
        body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(17),dp(16),dp(17),dp(30))}
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(AliebaBottomNav.make(this,section),LinearLayout.LayoutParams(-1,dp(93)))
        setContentView(root)
    }
    private fun heading(t:String){body.addView(text(t,26f,ink,true),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})}
    private fun message(t:String){body.addView(text(t,15f,muted).apply{setPadding(dp(8),dp(16),dp(8),dp(16))},LinearLayout.LayoutParams(-1,-2))}
    private fun button(t:String,action:()->Unit):TextView{
        val b=text(t,15f,ink,true).apply {gravity=Gravity.CENTER_VERTICAL;setPadding(dp(17),dp(12),dp(17),dp(12));background=shape(Color.WHITE);setOnClickListener{action()}}
        body.addView(b,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
        return b
    }
    private fun fetch(path:String,finished:(JSONObject?)->Unit){
        Thread {
            val value=try {
                val u=URL("https://alieba.ge"+path)
                val con=(u.openConnection() as HttpURLConnection).apply{connectTimeout=11000;readTimeout=16000;setRequestProperty("Accept","application/json")}
                try{if(con.responseCode in 200..299) JSONObject(con.inputStream.bufferedReader().use{it.readText()}) else null} finally{con.disconnect()}
            }catch(_:Exception){null}
            runOnUiThread {if(!isFinishing&&!isDestroyed)finished(value)}
        }.start()
    }
    private fun loadContent(){
        body.removeAllViews();heading(titleFor(section));message("Kateqoriyalar yüklənir…")
        fetch("/api/content-v19.php?section=$section") { json ->
            body.removeAllViews();heading(titleFor(section))
            if(json?.optBoolean("ok")!=true){message("Məzmun hazırda yüklənmədi. İnterneti və sayt API-sini yoxlayın.");button("Yenidən yoxla"){loadContent()};return@fetch}
            categories=json.optJSONArray("categories")?:JSONArray()
            entries=json.optJSONArray("items")?:JSONArray()
            drawContent()
        }
    }
    private fun drawContent(){
        body.removeAllViews();heading(titleFor(section))
        val finder=EditText(this).apply{hint="Məzmun axtar…";setSingleLine(true);textSize=15f;background=shape(Color.WHITE);setPadding(dp(16),0,dp(16),0);setText(query)}
        body.addView(finder,LinearLayout.LayoutParams(-1,dp(52)).apply{bottomMargin=dp(12)})
        val chips=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false}
        val line=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val listBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        fun refresh(){
            listBox.removeAllViews();var count=0
            for(i in 0 until entries.length()){
                val entry=entries.optJSONObject(i)?:continue
                if(categoryId!=0&&entry.optInt("category_id")!=categoryId)continue
                val t=entry.optString("title")
                if(query.isNotBlank()&&!(t+" "+entry.optString("summary")).contains(query,ignoreCase=true))continue
                count++
                val row=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(17),dp(16),dp(17),dp(16));background=shape(Color.WHITE);setOnClickListener{showEntry(entry)}}
                row.addView(text(t+"    ›",17f,ink,true))
                if(entry.optString("summary").isNotBlank())row.addView(text(entry.optString("summary"),13f,muted).apply{setPadding(0,dp(5),0,0)})
                listBox.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})
            }
            if(count==0)listBox.addView(text("Bu kateqoriyada hələ məzmun yoxdur.",15f,muted).apply{setPadding(dp(7),dp(22),dp(7),0)})
        }
        fun chip(name:String,id:Int){
            val selected=id==categoryId
            val b=text(name,13f,if(selected)Color.WHITE else ink,selected).apply{
                gravity=Gravity.CENTER;background=shape(if(selected)ink else Color.WHITE,22)
                setPadding(dp(16),dp(9),dp(16),dp(9));setOnClickListener{categoryId=id;drawContent()}
            }
            line.addView(b,LinearLayout.LayoutParams(-2,dp(39)).apply{rightMargin=dp(7)})
        }
        chip("Hamısı",0)
        for(i in 0 until categories.length()){
            val c=categories.optJSONObject(i)?:continue
            // SVG supplied by the admin is intentionally not interpreted as Android/HTML markup.
            chip(c.optString("name"),c.optInt("id"))
        }
        chips.addView(line)
        body.addView(chips,LinearLayout.LayoutParams(-1,dp(48)).apply{bottomMargin=dp(12)})
        body.addView(listBox)
        finder.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){query=s?.toString()?:"";refresh()}
            override fun afterTextChanged(s:Editable?){}
        })
        refresh()
    }
    private fun showEntry(e:JSONObject){
        stopAudio();body.removeAllViews()
        heading(e.optString("title"))
        val summary=e.optString("summary")
        if(summary.isNotBlank())message(summary)
        val arabic=e.optString("arabic_text")
        if(arabic.isNotBlank()){
            val a=text(arabic,23f,ink).apply{gravity=Gravity.RIGHT;setTextDirection(View.TEXT_DIRECTION_RTL);setPadding(dp(17),dp(22),dp(17),dp(22));background=shape(Color.WHITE)}
            body.addView(a,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        }
        val az=e.optString("az_text")
        if(az.isNotBlank())body.addView(text(az,16f,ink).apply{setPadding(dp(17),dp(17),dp(17),dp(17));background=shape(Color.WHITE);setLineSpacing(dp(5).toFloat(),1f)},LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        val url=e.optString("audio_url")
        if(url.isNotBlank())button("▶  Dinlə / dayandır"){playAudio(url)}
        val pdf=e.optString("pdf_file")
        if(pdf.isNotBlank())button("↓  PDF-i telefona yüklə"){downloadPdf(pdf,e.optString("title"))}
        button(if(isSaved(section,e.optInt("id").toString()))"♥  Yadda saxlanıb" else "♡  Yadda saxla"){
            toggleSaved(section,e.optInt("id").toString(),e.optString("title"));showEntry(e)
        }
        button("‹  Siyahıya qayıt"){stopAudio();drawContent()}
    }
    private fun safeMedia(raw:String):String? {
        val url=try{URL(URL("https://alieba.ge"),raw)}catch(_:Exception){return null}
        return if(url.protocol=="https" && (url.host=="alieba.ge" || url.host=="cdn.islamic.network" || url.host=="api.alquran.cloud" || url.host=="audio.alquran.cloud" || url.host=="everyayah.com"))url.toString() else null
    }
    private fun stopAudio(){audio?.run{try{if(isPlaying)stop()}catch(_:Exception){};release()};audio=null;playingButton?.text="▶  Dinlə";playingButton=null}
    private fun playAudio(source:String){
        val url=safeMedia(source)?:run{Toast.makeText(this,"Audio ünvanı etibarsızdır",Toast.LENGTH_LONG).show();return}
        if(audio!=null){stopAudio();return}
        Toast.makeText(this,"Audio hazırlanır…",Toast.LENGTH_SHORT).show()
        val p=MediaPlayer();audio=p
        try{p.setDataSource(url);p.setOnPreparedListener{if(audio===it)it.start()};p.setOnCompletionListener{stopAudio()};p.setOnErrorListener{_,_,_->stopAudio();true};p.prepareAsync()}
        catch(_:Exception){stopAudio();Toast.makeText(this,"Audio açıla bilmədi",Toast.LENGTH_SHORT).show()}
    }
    private fun downloadPdf(url:String,title:String){
        val target=safeMedia(url)?:run{Toast.makeText(this,"PDF ünvanı etibarsızdır",Toast.LENGTH_LONG).show();return}
        try{
            val name=title.replace(Regex("[^\\p{L}\\p{N}._ -]"),"").take(60).ifBlank{"Alieba"}+".pdf"
            val req=DownloadManager.Request(Uri.parse(target)).setTitle(name).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,name)
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            Toast.makeText(this,"PDF yükləməyə göndərildi",Toast.LENGTH_SHORT).show()
        }catch(_:Exception){Toast.makeText(this,"PDF yüklənmədi",Toast.LENGTH_LONG).show()}
    }
    private fun savedArray():JSONArray=try{JSONArray(favorites.getString("items","[]"))}catch(_:Exception){JSONArray()}
    private fun isSaved(s:String,id:String):Boolean {val a=savedArray();return (0 until a.length()).any {val x=a.optJSONObject(it);x!=null&&x.optString("section")==s&&x.optString("id")==id}}
    private fun toggleSaved(s:String,id:String,title:String){
        val a=savedArray();val out=JSONArray();var found=false
        for(i in 0 until a.length()){
            val x=a.optJSONObject(i)?:continue
            if(x.optString("section")==s&&x.optString("id")==id)found=true else out.put(x)
        }
        if(!found)out.put(JSONObject().put("section",s).put("id",id).put("title",title))
        favorites.edit().putString("items",out.toString()).apply()
        Toast.makeText(this,if(found)"Yadda saxlanandan silindi" else "Yadda saxlandı",Toast.LENGTH_SHORT).show()
    }
    private fun showSaved(){
        body.removeAllViews();heading("Yadda saxlananlar")
        val all=savedArray()
        if(all.length()==0){message("Hələ məzmun saxlamamısınız. Bu bölmədə telefonunuzda saxladığınız seçimlər görünəcək.");return}
        for(i in 0 until all.length()){
            val e=all.optJSONObject(i)?:continue
            button("♥  ${e.optString("title")}  ·  ${titleFor(e.optString("section"))}"){
                val dest=e.optString("section")
                if(dest=="quran"){
                    section="quran";showSurah(e.optString("id").substringBefore(':').toIntOrNull()?:1)
                }else{section=dest;categoryId=0;query="";loadContent()}
            }
        }
    }
    private fun loadNews(){
        body.removeAllViews();heading("Alieba yenilikləri");message("Yeniliklər yüklənir…")
        fetch("/api/news.php"){ json ->
            body.removeAllViews();heading("Alieba yenilikləri")
            val list=json?.optJSONArray("items")
            if(json?.optBoolean("ok")!=true){message("Yeniliklər hazırda yüklənmədi.");button("Yenilə"){loadNews()};return@fetch}
            if(list==null||list.length()==0){message("Hələ yenilik yoxdur.");return@fetch}
            for(i in 0 until list.length()){
                val n=list.optJSONObject(i)?:continue
                val row=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(17),dp(18),dp(17));background=shape(Color.WHITE)}
                row.addView(text(n.optString("title"),19f,ink,true))
                row.addView(text(n.optString("created_at").take(10),12f,gold).apply{setPadding(0,dp(7),0,dp(7))})
                row.addView(text(n.optString("body"),15f,ink).apply{setLineSpacing(dp(5).toFloat(),1f)})
                body.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
            }
        }
    }
    private fun surahList(){
        body.removeAllViews();heading("Qurani-Kərim")
        val last=favorites.getInt("last_surah",0)
        if(last in 1..114)button("▶  Qaldığım yerdən davam et · ${last}. ${surahs[last-1]}"){showSurah(last)}
        val search=EditText(this).apply{hint="Surə axtar…";setSingleLine(true);background=shape(Color.WHITE);setPadding(dp(15),0,dp(15),0)}
        body.addView(search,LinearLayout.LayoutParams(-1,dp(50)).apply{bottomMargin=dp(12)})
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};body.addView(list)
        fun draw(q:String){list.removeAllViews();surahs.forEachIndexed{index,name->
            if(q.isNotBlank()&&!name.contains(q,true)&&!(index+1).toString().contains(q))return@forEachIndexed
            val item=text("${index+1}.  $name      ›",17f,ink,true).apply{setPadding(dp(17),dp(17),dp(17),dp(17));background=shape(Color.WHITE);setOnClickListener{showSurah(index+1)}}
            list.addView(item,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
        }}
        search.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun afterTextChanged(s:Editable?){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){draw(s?.toString()?:"")}})
        draw("")
    }
    private fun showSurah(number:Int){
        section="quran";stopAudio();body.removeAllViews();heading("$number. ${surahs[number-1]}")
        message("Ayələr və Azərbaycan dilində tərcümə yüklənir…")
        fetch("/api/quran.php?surah=$number"){ json ->
            body.removeAllViews();heading("$number. ${surahs[number-1]}")
            val verses=json?.optJSONArray("ayahs")
            if(json?.optBoolean("ok")!=true||verses==null){message("Quran ayələri hazırda yüklənmədi.");button("Yenilə"){showSurah(number)};return@fetch}
            favorites.edit().putInt("last_surah",number).apply()
            button("‹  Surə siyahısı"){stopAudio();surahList()}
            val last=favorites.getInt("last_ayah_$number",0)
            if(last>0)message("Qaldığınız ayə: $number:$last")
            for(i in 0 until verses.length()){
                val ayah=verses.optJSONObject(i)?:continue
                val n=ayah.optInt("number")
                val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(17),dp(17),dp(17),dp(17));background=shape(Color.WHITE)}
                card.addView(text("$number:$n",14f,gold,true))
                card.addView(text(ayah.optString("text"),24f,ink).apply{gravity=Gravity.RIGHT;setTextDirection(View.TEXT_DIRECTION_RTL);setPadding(0,dp(16),0,dp(16));setLineSpacing(dp(6).toFloat(),1f)})
                card.addView(text(ayah.optString("translation"),15f,ink).apply{setLineSpacing(dp(5).toFloat(),1f)})
                val actions=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(12),0,0)}
                val listen=text("▶ Dinlə",14f,ink,true).apply{setPadding(0,dp(9),dp(14),dp(9));setOnClickListener{
                    favorites.edit().putInt("last_surah",number).putInt("last_ayah_$number",n).apply()
                    playAudio(ayah.optString("audio"))
                }}
                actions.addView(listen)
                val fav=text(if(isSaved("quran","$number:$n"))"♥" else "♡",22f,gold).apply{setPadding(dp(9),dp(6),dp(9),dp(6));setOnClickListener{
                    toggleSaved("quran","$number:$n","${surahs[number-1]} $number:$n")
                    this.text=if(isSaved("quran","$number:$n"))"♥" else "♡"
                }}
                actions.addView(fav)
                card.addView(actions)
                body.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
            }
        }
    }
    companion object {val surahs=arrayOf(
        "Fatihə","Bəqərə","Ali-İmran","Nisa","Maidə","Ənam","Əraf","Ənfal","Tövbə","Yunus","Hud","Yusuf","Rəd","İbrahim","Hicr","Nəhl","İsra","Kəhf","Məryəm","Taha","Ənbiya","Həcc","Muminun","Nur","Furqan","Şuəra","Nəml","Qəsəs","Ənkəbut","Rum","Loğman","Səcdə","Əhzab","Səba","Fatir","Yasin","Saffat","Sad","Zumər","Ğafir","Fussilət","Şura","Zuxruf","Duxan","Casiyə","Əhqaf","Muhəmməd","Fəth","Hucurat","Qaf","Zariyat","Tur","Nəcm","Qəmər","Rəhman","Vaqiə","Hədid","Mucadilə","Həşr","Mumtəhənə","Saff","Cümə","Munafiqun","Təğabun","Talaq","Təhrim","Mulk","Qələm","Haqqə","Məaric","Nuh","Cin","Muzzəmmil","Muddəssir","Qiyamə","İnsan","Mursəlat","Nəbə","Naziat","Əbəsə","Təkvir","İnfitar","Mutaffifin","İnşiqaq","Buruc","Tariq","Əla","Ğaşiyə","Fəcr","Bələd","Şəms","Leyl","Duha","Şərh","Tin","Ələq","Qədr","Bəyyinə","Zəlzələ","Adiyat","Qariə","Təkasur","Əsr","Huməzə","Fil","Qureyş","Maun","Kövsər","Kafirun","Nəsr","Məsəd","İxlas","Fələq","Nas"
    )}
}
