package com.alieba.app

import android.app.*
import android.os.*
import android.graphics.*
import android.graphics.drawable.*
import android.view.*
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.sin
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : Activity() {
    private val brown=0xff8a4e32.toInt(); private val ink=0xff302823.toInt(); private val cream=0xfffff9f5.toInt()
    override fun onCreate(b:Bundle?){super.onCreate(b); if(Build.VERSION.SDK_INT>=33)requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"),7); val v=home();setContentView(v);ViewCompat.setOnApplyWindowInsetsListener(v){x,i->val q=i.getInsets(WindowInsetsCompat.Type.systemBars());x.setPadding(0,q.top,0,q.bottom);i}}
    private fun home(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(cream)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(82))
        }

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val hero = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(245))
            background = getDrawable(R.drawable.mosque_bg)
        }

        hero.addView(
            View(this).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(0x16000000, 0x78000000)
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        hero.addView(
            TextView(this).apply {
                text = "Ali-eba"
                textSize = 25f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(18), dp(14), 0, 0)
            }
        )

        val profile = ImageButton(this).apply {
            setImageResource(R.drawable.ic_profile)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(0x33000000)
                shape = GradientDrawable.OVAL
            }
            setPadding(dp(9), dp(9), dp(9), dp(9))
            setOnClickListener { showProfile() }
        }

        hero.addView(
            profile,
            FrameLayout.LayoutParams(
                dp(43),
                dp(43),
                Gravity.TOP or Gravity.END
            ).apply {
                setMargins(0, dp(10), dp(14), 0)
            }
        )

        hero.addView(
            BirdSkyView(this),
            FrameLayout.LayoutParams(-1, dp(155))
        )

        hero.addView(
            prayerStrip(),
            FrameLayout.LayoutParams(-1, dp(68), Gravity.BOTTOM)
        )

        body.addView(hero)

        val grid = GridLayout(this).apply {
            columnCount = 3
            setPadding(dp(10), dp(17), dp(10), dp(14))
            setBackgroundColor(cream)
        }

        val items = listOf(
            "Quran" to R.drawable.ic_quran,
            "Məfatih" to R.drawable.ic_dua,
            "Əhkam" to R.drawable.ic_rules,
            "Təqvim" to R.drawable.ic_calendar,
            "Mərsiyələr" to R.drawable.ic_audio,
            "Hədis" to R.drawable.ic_hadith,
            "Məşvərət" to R.drawable.ic_chat,
            "Kitabxana" to R.drawable.ic_library,
            "Qiblə" to R.drawable.ic_qibla,
            "Zikr" to R.drawable.ic_tasbeh,
            "Ayarlar" to R.drawable.ic_settings
        )

        items.forEach { (name, icon) ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true

                addView(
                    ImageView(this@MainActivity).apply {
                        setImageResource(icon)
                        setColorFilter(brown)
                        setPadding(dp(5), dp(5), dp(5), dp(5))
                    },
                    LinearLayout.LayoutParams(dp(42), dp(42))
                )

                addView(
                    TextView(this@MainActivity).apply {
                        text = name
                        textSize = 12.5f
                        gravity = Gravity.CENTER
                        setTextColor(ink)
                        maxLines = 1
                    },
                    LinearLayout.LayoutParams(-1, dp(28))
                )

                if (name == "Ayarlar") {
                    setOnClickListener { prayerSettings() }
                }
            }

            grid.addView(
                cell,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(78)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            )
        }

        body.addView(grid)
        scroll.addView(body)

        root.addView(
            scroll,
            FrameLayout.LayoutParams(-1, -1)
        )

        root.addView(
            bottomWave(),
            FrameLayout.LayoutParams(-1, dp(88), Gravity.BOTTOM)
        )

        return root
    }

    private fun prayerStrip():View{val p=getSharedPreferences("prayer_times",MODE_PRIVATE);val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(dp(5),dp(5),dp(5),dp(8))};listOf("Fəcr" to "05:19","Günəş" to "06:42","Zöhr" to "12:55","Əsr" to "16:19","Məğrib" to "19:26","İşa" to "00:14").forEach{(n,d)->row.addView(TextView(this).apply{text="$n\n${p.getString(n,d)}";textSize=11.5f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=GradientDrawable().apply{setColor(0x66000000);cornerRadius=dp(15).toFloat()}},LinearLayout.LayoutParams(0,-1,1f).apply{setMargins(dp(2),0,dp(2),0)})};return row}
    private fun bottomWave(): View {
        val wrap = FrameLayout(this).apply {
            background = BottomWaveDrawable(Color.WHITE, dp(14).toFloat())
            elevation = dp(10).toFloat()
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(dp(28), dp(26), dp(28), dp(7))
        }

        fun nav(title: String, icon: Int, active: Boolean = false): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                addView(ImageView(this@MainActivity).apply {
                    setImageResource(icon)
                    setColorFilter(if (active) brown else 0xff817872.toInt())
                }, LinearLayout.LayoutParams(dp(23), dp(23)))

                addView(TextView(this@MainActivity).apply {
                    text = title
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setTextColor(if (active) brown else 0xff817872.toInt())
                })
            }
        }

        val homeNav = nav("Ana səhifə", R.drawable.ic_home, true)
        val profileNav = nav("Profil", R.drawable.ic_profile)

        profileNav.setOnClickListener { showProfile() }

        bar.addView(homeNav, LinearLayout.LayoutParams(0, -1, 1f))
        bar.addView(Space(this), LinearLayout.LayoutParams(0, -1, 0.8f))
        bar.addView(profileNav, LinearLayout.LayoutParams(0, -1, 1f))

        wrap.addView(bar, FrameLayout.LayoutParams(-1, -1))

        val ai = TextView(this).apply {
            text = "AI"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(brown)
                setStroke(dp(4), Color.WHITE)
            }

            elevation = dp(12).toFloat()

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Ali-eba AI Köməkçi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        wrap.addView(
            ai,
            FrameLayout.LayoutParams(
                dp(58),
                dp(58),
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                topMargin = dp(1)
            }
        )

        return wrap
    }

    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )

                val googleCredential = GoogleIdTokenCredential.createFrom(
                    result.credential.data
                )

                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleCredential.idToken,
                    null
                )

                FirebaseAuth.getInstance()
                    .signInWithCredential(firebaseCredential)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@MainActivity,
                            "Google hesabına giriş edildi",
                            Toast.LENGTH_SHORT
                        ).show()
                        showProfile()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this@MainActivity,
                            "Google giriş alınmadı: ${it.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Google giriş alınmadı: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showProfile() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(18), dp(24), dp(10))
        }

        box.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.ic_profile)
                setColorFilter(brown)
            },
            LinearLayout.LayoutParams(-1, dp(70))
        )

        box.addView(TextView(this).apply {
            text = "İstifadəçi\nAli-eba Coin: 0"
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(ink)
            setPadding(0, dp(8), 0, dp(14))
        })

        fun button(title: String, action: () -> Unit): Button {
            return Button(this).apply {
                text = title
                setOnClickListener { action() }
            }
        }

        box.addView(button("Google ilə daxil ol") {
            signInWithGoogle()
        })

        val role = getSharedPreferences(
            "account",
            MODE_PRIVATE
        ).getString("role", "user")

        if (role == "admin") {
            box.addView(button("Admin Paneli") {
                Toast.makeText(
                    this,
                    "Admin API paneli",
                    Toast.LENGTH_SHORT
                ).show()
            })
        }

        AlertDialog.Builder(this)
            .setTitle("Profil")
            .setView(box)
            .setNegativeButton("Bağla", null)
            .show()
    }

    private fun prayerSettings() {
        val names = arrayOf("Fəcr", "Günəş", "Zöhr", "Əsr", "Məğrib", "İşa")
        val defaults = arrayOf("05:19", "06:42", "12:55", "16:19", "19:26", "00:14")
        val prefs = getSharedPreferences("prayer_times", MODE_PRIVATE)
        var index = 0

        fun next() {
            if (index >= names.size) {
                Toast.makeText(
                    this,
                    "Azan vaxtları yadda saxlandı",
                    Toast.LENGTH_SHORT
                ).show()
                recreate()
                return
            }

            val name = names[index]
            val edit = EditText(this).apply {
                setText(prefs.getString(name, defaults[index]))
                hint = "HH:mm"
            }

            AlertDialog.Builder(this)
                .setTitle("$name vaxtı")
                .setView(edit)
                .setPositiveButton("Yadda saxla") { _, _ ->
                    prefs.edit()
                        .putString(name, edit.text.toString())
                        .apply()

                    index++
                    next()
                }
                .setNegativeButton("Bitir", null)
                .show()
        }

        next()
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
class BottomWaveDrawable(
    private val fill: Int,
    private val amp: Float
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fill
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()

        val path = Path().apply {
            moveTo(0f, amp)

            cubicTo(
                w * 0.18f, amp,
                w * 0.30f, amp * 0.45f,
                w * 0.38f, amp * 0.45f
            )

            cubicTo(
                w * 0.43f, amp * 0.45f,
                w * 0.43f, amp * 2.05f,
                w * 0.50f, amp * 2.05f
            )

            cubicTo(
                w * 0.57f, amp * 2.05f,
                w * 0.57f, amp * 0.45f,
                w * 0.62f, amp * 0.45f
            )

            cubicTo(
                w * 0.70f, amp * 0.45f,
                w * 0.82f, amp,
                w, amp
            )

            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(filter: ColorFilter?) {
        paint.colorFilter = filter
    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

class BirdSkyView(c: android.content.Context) : View(c) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private var time = 0f

    private val birds = listOf(
        floatArrayOf(-0.15f, 0.28f, 1.00f, 0.00f),
        floatArrayOf(0.12f, 0.48f, 0.72f, 1.20f),
        floatArrayOf(0.43f, 0.20f, 0.88f, 2.30f),
        floatArrayOf(0.72f, 0.38f, 0.62f, 3.10f),
        floatArrayOf(0.90f, 0.16f, 0.52f, 4.20f)
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        birds.forEachIndexed { i, bird ->
            val speed = 0.22f + i * 0.012f
            val x = ((bird[0] + time * speed) % 1.35f) * w
            val y = bird[1] * h + sin(time * 5f + bird[3]) * dp(3)
            val size = dp(12).toFloat() * bird[2]
            val flap = sin(time * 15f + bird[3])

            canvas.save()
            canvas.translate(x, y)

            val body = RectF(
                -size * 0.34f,
                -size * 0.12f,
                size * 0.42f,
                size * 0.18f
            )
            canvas.drawOval(body, paint)

            val head = RectF(
                size * 0.25f,
                -size * 0.20f,
                size * 0.52f,
                size * 0.07f
            )
            canvas.drawOval(head, paint)

            val tail = Path().apply {
                moveTo(-size * 0.28f, 0f)
                lineTo(-size * 0.72f, -size * 0.22f)
                lineTo(-size * 0.52f, size * 0.08f)
                lineTo(-size * 0.72f, size * 0.28f)
                close()
            }
            canvas.drawPath(tail, paint)

            val wingLift = size * (0.48f + flap * 0.30f)

            val upperWing = Path().apply {
                moveTo(-size * 0.05f, 0f)
                cubicTo(
                    -size * 0.18f, -size * 0.18f,
                    -size * 0.48f, -wingLift,
                    -size * 0.88f, -wingLift * 0.72f
                )
                cubicTo(
                    -size * 0.55f, -size * 0.12f,
                    -size * 0.28f, size * 0.05f,
                    -size * 0.05f, size * 0.10f
                )
                close()
            }
            canvas.drawPath(upperWing, paint)

            val lowerWing = Path().apply {
                moveTo(size * 0.05f, size * 0.03f)
                cubicTo(
                    size * 0.18f, size * 0.16f,
                    size * 0.48f, wingLift * 0.72f,
                    size * 0.76f, wingLift * 0.52f
                )
                cubicTo(
                    size * 0.48f, size * 0.12f,
                    size * 0.26f, -size * 0.02f,
                    size * 0.05f, -size * 0.05f
                )
                close()
            }
            canvas.drawPath(lowerWing, paint)

            canvas.restore()
        }

        time += 0.016f
        postInvalidateOnAnimation()
    }

    private fun dp(v: Int): Float =
        v * resources.displayMetrics.density
}
