package com.alieba.app

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MobileApi {
    private const val BASE = "https://alieba.ge/api/mobile-v21.php"
    private val main = Handler(Looper.getMainLooper())

    private fun cacheFile(c: Context, key: String): File {
        val dir = File(c.filesDir, "mobile_cache")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, key.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json")
    }

    private fun readCache(c: Context, key: String): JSONObject? = try {
        val f = cacheFile(c, key)
        if (f.isFile) JSONObject(f.readText()) else null
    } catch (_: Exception) {
        null
    }

    private fun saveCache(c: Context, key: String, value: JSONObject) {
        Thread {
            try { cacheFile(c, key).writeText(value.toString()) } catch (_: Exception) {}
        }.start()
    }

    private fun query(action: String, params: Map<String, String>): String {
        val builder = Uri.parse(BASE).buildUpon().appendQueryParameter("action", action)
        params.forEach { (k, v) -> builder.appendQueryParameter(k, v) }
        return builder.build().toString()
    }

    fun get(
        activity: Activity,
        action: String,
        params: Map<String, String> = emptyMap(),
        cacheKey: String? = null,
        done: (JSONObject, Boolean) -> Unit
    ) {
        if (cacheKey != null) {
            readCache(activity, cacheKey)?.let { cached ->
                done(cached, true)
            }
        }

        Thread {
            val result = try {
                val con = (URL(query(action, params)).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 9000
                    readTimeout = 14000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Alieba-Android/21")
                    setRequestProperty("Cache-Control", "no-cache")
                }

                try {
                    val stream = if (con.responseCode in 200..299) con.inputStream else con.errorStream
                    val raw = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
                    JSONObject(raw)
                } finally {
                    con.disconnect()
                }
            } catch (_: Exception) {
                null
            }

            if (result != null && result.optBoolean("ok") && cacheKey != null) {
                saveCache(activity, cacheKey, result)
            }

            if (result != null) {
                main.post {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        done(result, false)
                    }
                }
            }
        }.start()
    }

    fun post(
        activity: Activity,
        action: String,
        params: Map<String, String> = emptyMap(),
        auth: Boolean = false,
        done: (JSONObject?) -> Unit
    ) {
        fun execute(token: String?) {
            Thread {
                val result = try {
                    val con = (URL(BASE).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10000
                        readTimeout = 15000
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty(
                            "Content-Type",
                            "application/x-www-form-urlencoded; charset=UTF-8"
                        )
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", "Alieba-Android/21")
                        if (!token.isNullOrBlank()) {
                            setRequestProperty("Authorization", "Bearer $token")
                        }
                    }

                    val body = buildList {
                        add("action=" + URLEncoder.encode(action, "UTF-8"))
                        params.forEach { (k, v) ->
                            add(
                                URLEncoder.encode(k, "UTF-8") + "=" +
                                    URLEncoder.encode(v, "UTF-8")
                            )
                        }
                    }.joinToString("&")

                    con.outputStream.use {
                        it.write(body.toByteArray(Charsets.UTF_8))
                    }

                    try {
                        val stream = if (con.responseCode in 200..299) con.inputStream else con.errorStream
                        val raw = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
                        JSONObject(raw)
                    } finally {
                        con.disconnect()
                    }
                } catch (_: Exception) {
                    null
                }

                main.post {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        done(result)
                    }
                }
            }.start()
        }

        if (!auth) {
            execute(null)
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            done(JSONObject().put("ok", false).put("error", "login_required"))
            return
        }

        user.getIdToken(false)
            .addOnSuccessListener { execute(it.token) }
            .addOnFailureListener {
                done(JSONObject().put("ok", false).put("error", "token"))
            }
    }

    fun heartbeat(activity: Activity) {
        if (FirebaseAuth.getInstance().currentUser == null) return
        post(activity, "heartbeat", auth = true) {}
    }

    fun registerDevice(activity: Activity) {
        if (FirebaseAuth.getInstance().currentUser == null) return

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (token.isBlank()) return@addOnSuccessListener
            post(
                activity,
                "register_token",
                mapOf("token" to token),
                auth = true
            ) {}
        }
    }

    fun absolute(path: String): String {
        if (path.startsWith("https://") || path.startsWith("http://")) return path
        return "https://alieba.ge/" + path.trimStart('/')
    }
}
