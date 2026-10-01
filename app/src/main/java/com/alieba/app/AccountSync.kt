package com.alieba.app
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
object AccountSync{
 fun sync(done:(Boolean)->Unit={}){
  val u=FirebaseAuth.getInstance().currentUser?:run{done(false);return}
  u.getIdToken(true).addOnSuccessListener{r->
   val token=r.token?:run{done(false);return@addOnSuccessListener}
   Thread{val ok=try{val c=(URL("https://alieba.ge/api/auth/google.php").openConnection() as HttpURLConnection).apply{requestMethod="POST";doOutput=true;connectTimeout=8000;readTimeout=10000;setRequestProperty("Authorization","Bearer $token");setRequestProperty("Content-Type","application/json; charset=UTF-8")};val body=JSONObject().put("uid",u.uid).put("name",u.displayName?:"").put("email",u.email?:"").put("photo",u.photoUrl?.toString()?:"").toString();c.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))};val code=c.responseCode;c.disconnect();code in 200..299}catch(_:Exception){false};done(ok)}.start()
  }.addOnFailureListener{done(false)}
 }
}