package com.alieba.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
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
import org.json.JSONObject

class NativeProfileActivity : Activity() {
    private lateinit var body: LinearLayout

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        NativeUi.setBars(this)
        buildBase()
        render()
    }

    override fun onResume() {
        super.onResume()
        MobileApi.heartbeat(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 640) {
            try {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getSignedInAccountFromIntent(data)
                    .getResult(com.google.android.gms.common.api.ApiException::class.java)

                val token = account.idToken
                if (!token.isNullOrBlank()) firebaseLogin(token)
            } catch (e: Exception) {
                Toast.makeText(this, "Google giriş alınmadı", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildBase() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(NativeUi.bg)
        }

        root.addView(NativeUi.topBar(this, "Profil") { finish() })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeProfileActivity, 14),
                NativeUi.dp(this@NativeProfileActivity, 14),
                NativeUi.dp(this@NativeProfileActivity, 14),
                NativeUi.dp(this@NativeProfileActivity, 28)
            )
        }

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(
            NativeNav.make(this, "profile"),
            LinearLayout.LayoutParams(-1, NativeUi.dp(this, 87))
        )

        setContentView(root)
    }

    private fun render() {
        body.removeAllViews()
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            body.addView(
                NativeUi.text(this, "Alieba hesabı", 27f, NativeUi.ink, true)
            )
            body.addView(
                NativeUi.text(
                    this,
                    "Google ilə daxil ol. Sayt və APK eyni hesabdan istifadə edəcək.",
                    13f,
                    NativeUi.muted
                ).apply {
                    setPadding(0, NativeUi.dp(this@NativeProfileActivity, 7), 0, NativeUi.dp(this@NativeProfileActivity, 16))
                }
            )

            body.addView(
                NativeUi.text(this, "G  Google ilə daxil ol", 14f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    background = NativeUi.shape(this@NativeProfileActivity, NativeUi.green, 13)
                    setOnClickListener { signInGoogle() }
                },
                LinearLayout.LayoutParams(-1, NativeUi.dp(this, 50))
            )
            return
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                NativeUi.dp(this@NativeProfileActivity, 17),
                NativeUi.dp(this@NativeProfileActivity, 19),
                NativeUi.dp(this@NativeProfileActivity, 17),
                NativeUi.dp(this@NativeProfileActivity, 17)
            )
            background = NativeUi.card(this@NativeProfileActivity)
        }

        val avatar = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = NativeUi.shape(this@NativeProfileActivity, NativeUi.soft, 50)
            if (user.photoUrl != null) {
                load(user.photoUrl) { crossfade(true) }
            } else {
                setImageResource(R.drawable.ic_profile)
                setPadding(
                    NativeUi.dp(this@NativeProfileActivity, 22),
                    NativeUi.dp(this@NativeProfileActivity, 22),
                    NativeUi.dp(this@NativeProfileActivity, 22),
                    NativeUi.dp(this@NativeProfileActivity, 22)
                )
            }
        }
        card.addView(avatar, LinearLayout.LayoutParams(
            NativeUi.dp(this, 92), NativeUi.dp(this, 92)
        ))

        card.addView(
            NativeUi.text(
                this,
                user.displayName?.takeIf { it.isNotBlank() } ?: "Alieba istifadəçisi",
                22f,
                NativeUi.ink,
                true
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, NativeUi.dp(this@NativeProfileActivity, 10), 0, 0)
            }
        )

        card.addView(
            NativeUi.text(this, user.email ?: "", 11f, NativeUi.muted).apply {
                gravity = Gravity.CENTER
                setPadding(0, NativeUi.dp(this@NativeProfileActivity, 4), 0, 0)
            }
        )

        val serverStatus = NativeUi.text(this, "Sayt hesabı ilə sinxronlaşdırılır…", 10f, NativeUi.muted)
        card.addView(serverStatus, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = NativeUi.dp(this@NativeProfileActivity, 8)
        })

        body.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = NativeUi.dp(this@NativeProfileActivity, 11)
        })

        MobileApi.post(this, "me", auth = true) { result ->
            if (result?.optBoolean("ok") == true) {
                val profile = result.optJSONObject("user") ?: JSONObject()
                val parts = mutableListOf<String>()
                if (profile.optBoolean("verified")) parts.add("✓ Verify")
                if (profile.optString("religious_title").isNotBlank()) {
                    parts.add(profile.optString("religious_title"))
                }
                serverStatus.text =
                    if (parts.isEmpty()) "Sayt hesabı bağlıdır"
                    else parts.joinToString(" • ")
                serverStatus.setTextColor(NativeUi.green)
                MobileApi.registerDevice(this)
            } else {
                serverStatus.text = "Sayt hesabı sinxronlaşmadı"
            }
        }

        row("Suallarım", "Göndərdiyin dini suallar") {
            startActivity(Intent(this, NativeQaActivity::class.java).putExtra("mode", "my"))
        }
        row("Yadda saxlananlar", "Quran ayələri və məzmun") {
            startActivity(Intent(this, NativeContentActivity::class.java).putExtra("section", "saved"))
        }
        row("Ayarlar", "Azan, məkan və bildirişlər") {
            startActivity(Intent(this, NativeSettingsActivity::class.java))
        }
        row("Hesabdan çıxış", user.email ?: "Google hesabı") {
            FirebaseAuth.getInstance().signOut()
            render()
        }
    }

    private fun row(title: String, subtitle: String, action: () -> Unit) {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                NativeUi.dp(this@NativeProfileActivity, 15),
                NativeUi.dp(this@NativeProfileActivity, 13),
                NativeUi.dp(this@NativeProfileActivity, 15),
                NativeUi.dp(this@NativeProfileActivity, 13)
            )
            background = NativeUi.card(this@NativeProfileActivity)
            setOnClickListener { action() }
        }
        line.addView(NativeUi.text(this, title, 14f, NativeUi.ink, true))
        line.addView(
            NativeUi.text(this, subtitle, 10f, NativeUi.muted).apply {
                setPadding(0, NativeUi.dp(this@NativeProfileActivity, 4), 0, 0)
            }
        )
        body.addView(line, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = NativeUi.dp(this@NativeProfileActivity, 9)
        })
    }

    private fun signInGoogle() {
        val webClientId = getString(R.string.default_web_client_id)
        if (webClientId.isBlank()) {
            Toast.makeText(this, "Google Client ID yoxdur", Toast.LENGTH_LONG).show()
            return
        }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = CredentialManager.create(this@NativeProfileActivity)
                    .getCredential(
                        context = this@NativeProfileActivity,
                        request = request
                    )

                val token =
                    GoogleIdTokenCredential.createFrom(result.credential.data).idToken

                firebaseLogin(token)
            } catch (_: androidx.credentials.exceptions.GetCredentialCancellationException) {
            } catch (_: Exception) {
                legacyGoogle(webClientId)
            }
        }
    }

    private fun legacyGoogle(webClientId: String) {
        try {
            val options =
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()

            val client =
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, options)

            startActivityForResult(client.signInIntent, 640)
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
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Google giriş xətası. Firebase SHA-1/SHA-256 ayarlarını yoxla.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
