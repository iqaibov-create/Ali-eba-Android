package com.alieba.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Optional version manifest on the Alieba server; silent if not yet deployed. */
object AliebaUpdateChecker {
    fun check(a:Activity){
        Thread{
            try{
                val con=(URL("https://alieba.ge/apk/version.json").openConnection() as HttpURLConnection).apply {
                    connectTimeout=5000;readTimeout=5000;setRequestProperty("Accept","application/json")
                }
                val data=try{if(con.responseCode==200)JSONObject(con.inputStream.bufferedReader().use{it.readText()}) else null}finally{con.disconnect()}
                if(data!=null){
                    val installed=if(android.os.Build.VERSION.SDK_INT>=28)a.packageManager.getPackageInfo(a.packageName,0).longVersionCode
                                  else a.packageManager.getPackageInfo(a.packageName,0).versionCode.toLong()
                    val next=data.optLong("versionCode",0L)
                    val url=Uri.parse(data.optString("apkUrl",""))
                    if(next>installed && url.scheme=="https" && url.host=="alieba.ge" && url.path?.endsWith(".apk",true)==true){
                        a.runOnUiThread{
                            if(a.isFinishing||a.isDestroyed)return@runOnUiThread
                            AlertDialog.Builder(a).setTitle("Yeni Alieba versiyası")
                                .setMessage(data.optString("notes","Yeniləmə mövcuddur.")+"\nYeniləmə mövcud tətbiqin üzərinə quraşdırılır; Android təsdiqi tələb olunur.")
                                .setPositiveButton("Güncəllə"){_,_->
                                    a.startActivity(Intent(Intent.ACTION_VIEW,url).addCategory(Intent.CATEGORY_BROWSABLE))
                                }.setNegativeButton("Sonra",null).show()
                        }
                    }
                }
            }catch(_:Exception) { /* offline: keep the installed version usable */ }
        }.start()
    }
}
