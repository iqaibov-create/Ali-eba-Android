package com.alieba.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import coil.load
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabibV222ProfileActivity : Activity() {
    private lateinit var root: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun round(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        NativeUi.setBars(this)
        render()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 642) {
            try {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getSignedInAccountFromIntent(data)
                    .getResult(com.google.android.gms.common.api.ApiException::class.java)
                account.idToken?.let { firebaseLogin(it) }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                val extra = if (e.statusCode == 10) " Firebase release SHA-1 əlavə olunmalıdır: 54:AD:90:2B:E3:96:C6:49:37:5F:37:D0:09:B1:37:ED:9B:55:12:C7" else ""
                Toast.makeText(this, "Google giriş alınmadı (kod ${e.statusCode}).$extra", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Google giriş alınmadı: ${e.localizedMessage ?: "naməlum xəta"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xfffbfcfc.toInt())
        }

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(10))
        }
        head.addView(text("‹", 36f, 0xff5e6663.toInt()).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        head.addView(text("Hesab", 19f, 0xff25282a.toInt(), true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(48), 1f))
        head.addView(Space(this), LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(head)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) signedOut() else signedIn()
        setContentView(root)
    }

    private fun signedOut() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(32), dp(26), dp(24))
        }

        val icon = FrameLayout(this).apply {
            background = round(0xfff2efff.toInt(), 45, 0xffe0d8ff.toInt())
        }
        icon.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_profile)
            setColorFilter(0xff6b4fee.toInt())
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(28), dp(28), dp(28), dp(28))
        }, FrameLayout.LayoutParams(-1, -1))
        content.addView(icon, LinearLayout.LayoutParams(dp(120), dp(120)).apply { bottomMargin = dp(34) })

        content.addView(text("Hesaba Giriş", 25f, 0xff222426.toInt(), true).apply { gravity = Gravity.CENTER })
        content.addView(text("Alieba hesabınıza Google ilə daxil olun. Profil və bildirişlər eyni hesabla işləyəcək.", 13f, 0xff8a8f92.toInt()).apply {
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(12), dp(18), 0)
        })

        content.addView(Space(this), LinearLayout.LayoutParams(-1, 0, 1f))

        val google = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(18), 0, dp(18), 0)
            background = round(Color.WHITE, 20, 0xffe7eaeb.toInt())
            elevation = dp(3).toFloat()
            setOnClickListener { signInGoogle() }
        }
        google.addView(text("G", 24f, 0xff4285f4.toInt(), true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(40), -1))
        google.addView(text("Google-la giriş", 15f, 0xff242729.toInt(), true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, -1, 1f))
        content.addView(google, LinearLayout.LayoutParams(-1, dp(62)).apply { topMargin = dp(26); bottomMargin = dp(10) })

        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun signedIn() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(24), dp(22), dp(24))
        }

        val avatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = round(0xffeef4f2.toInt(), 48)
            if (user.photoUrl != null) load(user.photoUrl) else setImageResource(R.drawable.ic_profile)
        }
        content.addView(avatar, LinearLayout.LayoutParams(dp(96), dp(96)).apply { bottomMargin = dp(14) })
        content.addView(text(user.displayName?.takeIf { it.isNotBlank() } ?: "Alieba istifadəçisi", 22f, 0xff1f2f2b.toInt(), true).apply { gravity = Gravity.CENTER })
        content.addView(text(user.email ?: "", 12f, 0xff7d8985.toInt()).apply { gravity = Gravity.CENTER; setPadding(0, dp(5), 0, dp(24)) })

        fun item(title: String, action: () -> Unit) {
            content.addView(text(title, 14f, 0xff17362f.toInt(), true).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), 0, dp(16), 0)
                background = round(Color.WHITE, 16, 0xffe1e9e6.toInt())
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(10) })
        }

        item("Suallarım") { startActivity(Intent(this, NativeQaActivity::class.java).putExtra("mode", "my")) }
        item("Ayarlar") { startActivity(Intent(this, NativeSettingsActivity::class.java)) }
        item("Hesabdan çıxış") { FirebaseAuth.getInstance().signOut(); render() }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun signInGoogle() {
        val webClientId = getString(R.string.default_web_client_id)
        if (webClientId.isBlank()) {
            Toast.makeText(this, "Google Client ID tapılmadı", Toast.LENGTH_LONG).show()
            return
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            ).build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = CredentialManager.create(this@HabibV222ProfileActivity)
                    .getCredential(context = this@HabibV222ProfileActivity, request = request)
                val token = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                firebaseLogin(token)
            } catch (_: androidx.credentials.exceptions.GetCredentialCancellationException) {
            } catch (_: Exception) {
                legacyGoogle(webClientId)
            }
        }
    }

    private fun legacyGoogle(webClientId: String) {
        try {
            val options = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            ).requestIdToken(webClientId).requestEmail().build()
            val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, options)
            startActivityForResult(client.signInIntent, 642)
        } catch (_: Exception) {
            Toast.makeText(this, "Google hesabı seçimi açıla bilmədi", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseLogin(token: String) {
        FirebaseAuth.getInstance()
            .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
            .addOnSuccessListener {
                MobileApi.heartbeat(this)
                MobileApi.registerDevice(this)
                Toast.makeText(this, "Google hesabına giriş edildi", Toast.LENGTH_SHORT).show()
                render()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Google giriş xətası: ${error.localizedMessage ?: "naməlum xəta"}. Firebase release SHA-1/SHA-256 və yeni google-services.json tələb olunur.", Toast.LENGTH_LONG).show()
            }
    }
}
