package com.alieba.app

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL

/** In-app, user-confirmed updates from Alieba's own HTTPS distribution endpoint. */
object AliebaUpdateChecker {
    private const val MANIFEST_URL = "https://alieba.ge/apk/version.json"
    private const val PREFS = "alieba_apk_update"
    private const val DOWNLOAD_ID = "download_id"
    private const val DOWNLOAD_NAME = "download_name"
    private var activeActivity: WeakReference<Activity>? = null
    private var appReceiver: BroadcastReceiver? = null
    private var watching = false
    private var installerDialogShowing = false

    fun check(activity: Activity) {
        resumePending(activity)
        Thread {
            try {
                val connection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.setRequestProperty("Accept", "application/json")
                val data = try {
                    if (connection.responseCode == HttpURLConnection.HTTP_OK)
                        JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    else null
                } finally { connection.disconnect() }
                if (data == null) return@Thread
                val installed = if (Build.VERSION.SDK_INT >= 28)
                    activity.packageManager.getPackageInfo(activity.packageName, 0).longVersionCode
                else activity.packageManager.getPackageInfo(activity.packageName, 0).versionCode.toLong()
                val next = data.optLong("versionCode", 0L)
                val apk = Uri.parse(data.optString("apkUrl", ""))
                if (next <= installed || !allowedApk(apk)) return@Thread
                activity.runOnUiThread {
                    if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                    val pending = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(DOWNLOAD_ID, -1L)
                    if (pending > 0) return@runOnUiThread
                    AlertDialog.Builder(activity)
                        .setTitle("Alieba yenilənməsi")
                        .setMessage("Yeni versiya: ${data.optString("versionName", next.toString())}\n\n" +
                                data.optString("notes", "Yeniliklər və düzəlişlər") +
                                "\n\nYenilə düyməsindən sonra Android quraşdırmanı təsdiqləməyi istəyəcək.")
                        .setPositiveButton("Yenilə") { _, _ -> startDownload(activity, apk, next) }
                        .setNegativeButton("Sonra", null)
                        .show()
                }
            } catch (_: Exception) { /* Offline: keep the already installed app usable. */ }
        }.start()
    }

    private fun allowedApk(uri: Uri): Boolean =
        uri.scheme.equals("https", true) && uri.host.equals("alieba.ge", true) &&
                uri.path?.lowercase()?.endsWith(".apk") == true

    private fun manager(c: Context): DownloadManager =
        c.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private fun startDownload(a: Activity, apk: Uri, version: Long) {
        try {
            if (!allowedApk(apk)) return
            val fileName = "alieba-update-$version.apk"
            val file = File(a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (file.exists()) file.delete()
            val request = DownloadManager.Request(apk)
                .setTitle("Alieba yenilənməsi")
                .setDescription("APK endirilir — bitəndə quraşdırma açılacaq")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setDestinationInExternalFilesDir(a, Environment.DIRECTORY_DOWNLOADS, fileName)
            val id = manager(a).enqueue(request)
            a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(DOWNLOAD_ID, id).putString(DOWNLOAD_NAME, fileName).apply()
            Toast.makeText(a, "Yeniləmə endirilir", Toast.LENGTH_LONG).show()
            resumePending(a)
        } catch (_: Exception) {
            Toast.makeText(a, "APK endirilə bilmədi. İnterneti yoxlayın.", Toast.LENGTH_LONG).show()
        }
    }

    /** Called on resume too: a finished download can install after the user reopens the app. */
    fun resumePending(a: Activity) {
        activeActivity = WeakReference(a)
        val prefs = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(DOWNLOAD_ID, -1L) <= 0L) return
        watchDownload(a.applicationContext)
        queryDownload(a)
    }

    private fun watchDownload(context: Context) {
        if (watching) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                val a = activeActivity?.get() ?: return
                val wanted = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(DOWNLOAD_ID, -1L)
                if (wanted > 0L && wanted == intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2L))
                    a.runOnUiThread { queryDownload(a) }
            }
        }
        // DownloadManager may send the completion broadcast from a privileged system app.
        if (Build.VERSION.SDK_INT >= 33)
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        else context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        appReceiver = receiver
        watching = true
    }

    private fun clearPending(a: Activity) {
        a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(DOWNLOAD_ID).remove(DOWNLOAD_NAME).apply()
    }

    private fun queryDownload(a: Activity) {
        if (a.isFinishing || a.isDestroyed || installerDialogShowing) return
        val prefs = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getLong(DOWNLOAD_ID, -1L)
        if (id <= 0L) return
        val cursor = try { manager(a).query(DownloadManager.Query().setFilterById(id)) } catch (_: Exception) { null }
        val status = try {
            if (cursor != null && cursor.moveToFirst())
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            else DownloadManager.STATUS_FAILED
        } catch (_: Exception) { DownloadManager.STATUS_FAILED }
        finally { cursor?.close() }
        when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> requestInstall(a, prefs.getString(DOWNLOAD_NAME, "").orEmpty())
            DownloadManager.STATUS_FAILED -> {
                clearPending(a)
                Toast.makeText(a, "Yeniləmə endirilə bilmədi.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestInstall(a: Activity, filename: String) {
        if (installerDialogShowing) return
        val folder = a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
        val file = File(folder, filename)
        // Refuse HTML error pages, empty files, and installers belonging to another app.
        if (filename.isEmpty() || file.length() < 100_000L ||
            a.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.packageName != a.packageName) {
            clearPending(a)
            Toast.makeText(a, "Yeniləmə faylı etibarlı APK deyil.", Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 26 && !a.packageManager.canRequestPackageInstalls()) {
            installerDialogShowing = true
            AlertDialog.Builder(a).setTitle("Quraşdırma icazəsi")
                .setMessage("Alieba-nı yeniləmək üçün bu tətbiqdən APK quraşdırmasına icazə verin. Sonra tətbiqə qayıdın.")
                .setPositiveButton("İcazə ver") { _, _ ->
                    installerDialogShowing = false
                    a.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${a.packageName}")))
                }
                .setNegativeButton("Sonra") { _, _ -> installerDialogShowing = false }
                .setOnCancelListener { installerDialogShowing = false }
                .show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(a, "${a.packageName}.updateprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            a.startActivity(intent)
            clearPending(a)
        } catch (_: Exception) {
            Toast.makeText(a, "Quraşdırma açıla bilmədi.", Toast.LENGTH_LONG).show()
        }
    }
}
