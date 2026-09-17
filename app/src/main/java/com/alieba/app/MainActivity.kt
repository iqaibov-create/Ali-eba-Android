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
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )
        window.statusBarColor = Color.TRANSPARENT

        val setup = getSharedPreferences(
            "alieba_setup",
            MODE_PRIVATE
        )

        if (!setup.getBoolean("completed", false)) {
            setContentView(firstSetup())
            return
        }

        AzanScheduler.scheduleAll(this)
        showHome()
    }

    private fun showHome() {
        val v = home()
        setContentView(v)

        ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        when (requestCode) {
            41 -> {
                val granted = grantResults.isNotEmpty() &&
                    grantResults.any {
                        it == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }

                getSharedPreferences("alieba_setup", MODE_PRIVATE)
                    .edit()
                    .putBoolean("location_permission", granted)
                    .apply()

                if (granted) {
                    detectAutomaticLocation()
                } else {
                    Toast.makeText(
                        this,
                        "Məkan icazəsi verilmədi. Şəhəri əl ilə seçin.",
                        Toast.LENGTH_LONG
                    ).show()
                    setContentView(locationSetup())
                }
            }

            42 -> {
                val granted = grantResults.isNotEmpty() &&
                    grantResults[0] ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

                getSharedPreferences("alieba_setup", MODE_PRIVATE)
                    .edit()
                    .putBoolean("notifications", granted)
                    .apply()
            }
        }
    }

    private fun detectAutomaticLocation() {
        if (
            checkSelfPermission(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            setContentView(locationSetup())
            return
        }

        val client =
            com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(this)

        val token =
            com.google.android.gms.tasks.CancellationTokenSource()

        client.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            token.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                getSharedPreferences(
                    "alieba_setup",
                    MODE_PRIVATE
                ).edit()
                    .putString("location_mode", "auto")
                    .putString("latitude", location.latitude.toString())
                    .putString("longitude", location.longitude.toString())
                    .apply()

                Toast.makeText(
                    this,
                    "Məkan müəyyən edildi",
                    Toast.LENGTH_SHORT
                ).show()

                setContentView(notificationSetup())
            } else {
                Toast.makeText(
                    this,
                    "Məkan tapılmadı. Şəhəri əl ilə seçin.",
                    Toast.LENGTH_LONG
                ).show()

                setContentView(locationSetup())
            }
        }.addOnFailureListener {
            Toast.makeText(
                this,
                "Məkan müəyyən edilə bilmədi.",
                Toast.LENGTH_LONG
            ).show()

            setContentView(locationSetup())
        }
    }

    private fun firstSetup(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(60), dp(24), dp(28))
            setBackgroundColor(cream)
        }

        root.addView(TextView(this).apply {
            text = "Ali-eba"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(brown)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "1 / 3"
            textSize = 13f
            setTextColor(0xff8d817a.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(30))
        })

        root.addView(TextView(this).apply {
            text = "Dilinizi seçin"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "Tətbiqin istifadə dilini seçin"
            textSize = 14f
            setTextColor(0xff756a64.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(25))
        })

        val languages = listOf(
            "🇦🇿  Azərbaycan" to "az",
            "🇹🇷  Türkçe" to "tr",
            "🇷🇺  Русский" to "ru",
            "🇬🇪  ქართული" to "ka"
        )

        languages.forEach { (label, code) ->
            val button = TextView(this).apply {
                text = label
                textSize = 17f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ink)
                setPadding(dp(20), 0, dp(20), 0)

                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), 0xffeadfd8.toInt())
                }

                setOnClickListener {
                    getSharedPreferences(
                        "alieba_setup",
                        MODE_PRIVATE
                    ).edit()
                        .putString("language", code)
                        .apply()

                    setContentView(locationSetup())
                }
            }

            root.addView(
                button,
                LinearLayout.LayoutParams(
                    -1,
                    dp(62)
                ).apply {
                    bottomMargin = dp(12)
                }
            )
        }

        return root
    }

    private fun locationSetup(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(60), dp(24), dp(28))
            setBackgroundColor(cream)
        }

        root.addView(TextView(this).apply {
            text = "Ali-eba"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(brown)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "2 / 3"
            textSize = 13f
            setTextColor(0xff8d817a.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(30))
        })

        root.addView(TextView(this).apply {
            text = "Məkanınızı seçin"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "Namaz vaxtlarını düzgün hesablamaq üçün məkan lazımdır"
            textSize = 14f
            setTextColor(0xff756a64.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(28))
        })

        val auto = TextView(this).apply {
            text = "◎   Avtomatik müəyyən et"
            textSize = 17f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            setPadding(dp(20), 0, dp(20), 0)

            background = GradientDrawable().apply {
                setColor(brown)
                cornerRadius = dp(16).toFloat()
            }

            setOnClickListener {
                getSharedPreferences("alieba_setup", MODE_PRIVATE)
                    .edit()
                    .putString("location_mode", "auto")
                    .apply()

                if (Build.VERSION.SDK_INT >= 23 &&
                    checkSelfPermission(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        41
                    )
                } else {
                    detectAutomaticLocation()
                }
            }
        }

        root.addView(
            auto,
            LinearLayout.LayoutParams(-1, dp(62)).apply {
                topMargin = dp(8)
                bottomMargin = dp(16)
            }
        )

        val city = EditText(this).apply {
            hint = "Şəhər — məsələn: Marneuli"
            textSize = 16f
            setSingleLine(true)
            setPadding(dp(18), 0, dp(18), 0)

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), 0xffeadfd8.toInt())
            }
        }

        root.addView(
            city,
            LinearLayout.LayoutParams(-1, dp(60))
        )

        val manual = TextView(this).apply {
            text = "Şəhəri yadda saxla və davam et"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(brown)

            setOnClickListener {
                val value = city.text.toString().trim()

                if (value.isEmpty()) {
                    city.error = "Şəhəri yazın"
                    return@setOnClickListener
                }

                getSharedPreferences("alieba_setup", MODE_PRIVATE)
                    .edit()
                    .putString("location_mode", "manual")
                    .putString("city", value)
                    .apply()

                setContentView(notificationSetup())
            }
        }

        root.addView(
            manual,
            LinearLayout.LayoutParams(-1, dp(58)).apply {
                topMargin = dp(10)
            }
        )

        return root
    }

    private fun notificationSetup(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(60), dp(24), dp(28))
            setBackgroundColor(cream)
        }

        root.addView(TextView(this).apply {
            text = "Ali-eba"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(brown)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "3 / 3"
            textSize = 13f
            setTextColor(0xff8d817a.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(30))
        })

        root.addView(TextView(this).apply {
            text = "Azan və bildirişlər"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "Namaz vaxtı daxil olduqda Ali-eba sizə bildiriş göndərə və azan səsləndirə bilər."
            textSize = 14f
            setTextColor(0xff756a64.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(28))
        })

        val notifications = Switch(this).apply {
            text = "Namaz bildirişləri"
            textSize = 17f
            isChecked = true
            setTextColor(ink)
            setPadding(dp(18), 0, dp(12), 0)

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), 0xffeadfd8.toInt())
            }
        }

        root.addView(
            notifications,
            LinearLayout.LayoutParams(-1, dp(64))
        )

        root.addView(TextView(this).apply {
            text = "Sonradan Profil → Azan və namaz bildirişləri bölməsindən Fəcr, Zöhr, Əsr, Məğrib və İşa üçün ayrıca seçim edə bilərsiniz."
            textSize = 13f
            setTextColor(0xff8d817a.toInt())
            setPadding(dp(8), dp(15), dp(8), dp(28))
        })

        val start = TextView(this).apply {
            text = "Ali-eba-ya başla"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)

            background = GradientDrawable().apply {
                setColor(brown)
                cornerRadius = dp(17).toFloat()
            }

            setOnClickListener {
                val enabled = notifications.isChecked

                listOf(
                    "Fəcr",
                    "Zöhr",
                    "Əsr",
                    "Məğrib",
                    "İşa"
                ).forEach { prayer ->
                    AzanPrefs.setPrayerEnabled(
                        this@MainActivity,
                        prayer,
                        enabled
                    )
                }

                getSharedPreferences(
                    "alieba_setup",
                    MODE_PRIVATE
                ).edit()
                    .putBoolean("notifications", enabled)
                    .putBoolean("completed", true)
                    .apply()

                if (
                    enabled &&
                    Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ),
                        42
                    )
                }

                AzanScheduler.scheduleAll(this@MainActivity)
                showHome()
            }
        }

        root.addView(
            start,
            LinearLayout.LayoutParams(-1, dp(62)).apply {
                topMargin = dp(12)
            }
        )

        return root
    }

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

        val heroWave = View(this).apply {
            background = BottomWaveDrawable(cream, dp(12).toFloat())
            rotation = 180f
        }

        hero.addView(
            heroWave,
            FrameLayout.LayoutParams(-1, dp(28), Gravity.BOTTOM)
        )

        hero.addView(
            prayerStrip(),
            FrameLayout.LayoutParams(-1, dp(68), Gravity.BOTTOM).apply {
                bottomMargin = dp(14)
            }
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
                        val iconColor = when (name) {
                            "Quran" -> 0xff2E7D32.toInt()
                            "Məfatih" -> 0xff7B4FA3.toInt()
                            "Əhkam" -> 0xffB06B32.toInt()
                            "Təqvim" -> 0xff3979B8.toInt()
                            "Mərsiyələr" -> 0xffB84B4B.toInt()
                            "Hədis" -> 0xffD17A28.toInt()
                            "Məşvərət" -> 0xff3F7C8C.toInt()
                            "Kitabxana" -> 0xff795548.toInt()
                            "Qiblə" -> 0xff168C83.toInt()
                            "Zikr" -> 0xffC49324.toInt()
                            "Ayarlar" -> 0xff687078.toInt()
                            else -> brown
                        }
                        setColorFilter(iconColor)
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

                setOnClickListener {
                    when (name) {
                        "Quran" -> openSection("Quran", "Quran surələri, ayələr, tərcümə və audio")
                        "Məfatih" -> openSection("Məfatih", "Dualar, ziyarətnamələr və gündəlik əməllər")
                        "Əhkam" -> openSection("Əhkam", "Dini hökmlər və mövzular")
                        "Təqvim" -> openSection("Təqvim", "Hicri təqvim, dini günlər və namaz vaxtları")
                        "Mərsiyələr" -> openSection("Mərsiyələr", "Mərsiyə və dini audio bölməsi")
                        "Hədis" -> openSection("Hədis", "Əhli-beyt hədisləri və mövzular")
                        "Məşvərət" -> openSection("Məşvərət", "Sual verin və məsləhət alın")
                        "Kitabxana" -> openSection("Kitabxana", "Dini kitablar və PDF kitabxanası")
                        "Qiblə" -> openSection("Qiblə", "Qiblə istiqamətini müəyyən edin")
                        "Zikr" -> openZikr()
                        "Ayarlar" -> prayerSettings()
                    }
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

    private fun prayerStrip():View{val p=getSharedPreferences("prayer_settings",MODE_PRIVATE);val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(dp(5),dp(5),dp(5),dp(8))};listOf("Fəcr" to "05:19","Günəş" to "06:42","Zöhr" to "12:55","Əsr" to "16:19","Məğrib" to "19:26","İşa" to "00:14").forEach{(n,d)->row.addView(TextView(this).apply{text="$n\n${p.getString(n,d)}";textSize=11.5f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=GradientDrawable().apply{setColor(0x66000000);cornerRadius=dp(15).toFloat()}},LinearLayout.LayoutParams(0,-1,1f).apply{setMargins(dp(2),0,dp(2),0)})};return row}
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
        val supportNav = nav("Dəstək", R.drawable.ic_heart)

        homeNav.setOnClickListener { setContentView(home()) }

        supportNav.setOnClickListener {
            openSection(
                "Proqrama dəstək",
                "Ali-eba layihəsinə dəstək və əlaqə bölməsi"
            )
        }

        bar.addView(homeNav, LinearLayout.LayoutParams(0, -1, 1f))
        bar.addView(Space(this), LinearLayout.LayoutParams(0, -1, 0.8f))
        bar.addView(supportNav, LinearLayout.LayoutParams(0, -1, 1f))

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
        val user = FirebaseAuth.getInstance().currentUser

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(12), dp(22), dp(10))
        }

        val avatar = TextView(this).apply {
            text = if (user != null) "✓" else "👤"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(brown)
            }
        }

        box.addView(
            avatar,
            LinearLayout.LayoutParams(dp(68), dp(68)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(10)
            }
        )

        box.addView(TextView(this).apply {
            text = user?.displayName ?: "Ali-eba istifadəçisi"
            textSize = 20f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        })

        box.addView(TextView(this).apply {
            text = if (user != null) {
                "${user.email ?: ""}\nAli-eba Coin: 0  •  Premium: Aktiv deyil"
            } else {
                "Hesaba daxil olun\nAli-eba Coin: 0"
            }
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(0xff756a64.toInt())
            setPadding(0, dp(5), 0, dp(14))
        })

        fun item(title: String, subtitle: String, action: () -> Unit): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(11), dp(15), dp(11))
                background = GradientDrawable().apply {
                    setColor(0xfffff7f2.toInt())
                    cornerRadius = dp(14).toFloat()
                }

                addView(TextView(this@MainActivity).apply {
                    text = title
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ink)
                })

                addView(TextView(this@MainActivity).apply {
                    text = subtitle
                    textSize = 11.5f
                    setTextColor(0xff817872.toInt())
                })

                setOnClickListener { action() }
            }
        }

        fun addItem(title: String, subtitle: String, action: () -> Unit) {
            box.addView(
                item(title, subtitle, action),
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(7)
                }
            )
        }

        if (user == null) {
            addItem(
                "Google ilə daxil ol",
                "Profilinizi və məlumatlarınızı hesabınıza bağlayın"
            ) {
                signInWithGoogle()
            }
        } else {
            addItem(
                "Hesabım",
                "Google hesabı qoşulub"
            ) {
                Toast.makeText(this, user.email ?: "Google hesabı", Toast.LENGTH_SHORT).show()
            }
        }

        addItem(
            "Azan və namaz bildirişləri",
            "Fəcr, Zöhr, Əsr, Məğrib və İşa üçün ayrıca seçim"
        ) {
            prayerSettings()
        }

        addItem(
            "Ali-eba Coin və Premium",
            "Balans, Premium və gələcək xidmətlər"
        ) {
            Toast.makeText(this, "Coin və Premium", Toast.LENGTH_SHORT).show()
        }

        addItem(
            "Tətbiq ayarları",
            "Dil, görünüş və digər seçimlər"
        ) {
            Toast.makeText(this, "Tətbiq ayarları", Toast.LENGTH_SHORT).show()
        }

        addItem(
            "Proqrama dəstək",
            "Ali-eba layihəsinə dəstək və əlaqə"
        ) {
            Toast.makeText(this, "Proqrama dəstək", Toast.LENGTH_SHORT).show()
        }

        val role = getSharedPreferences(
            "account",
            MODE_PRIVATE
        ).getString("role", "user")

        if (role == "admin") {
            addItem(
                "Admin Paneli",
                "Kontent, istifadəçilər, Coin, Premium və bildirişlər"
            ) {
                Toast.makeText(this, "Admin API paneli", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Profil")
            .setView(box)
            .setNegativeButton("Bağla", null)
            .show()
    }

    private fun prayerSettings() {
        val prefs = getSharedPreferences("prayer_settings", MODE_PRIVATE)

        val prayers = arrayOf(
            "Fəcr",
            "Zöhr",
            "Əsr",
            "Məğrib",
            "İşa"
        )

        val defaults = arrayOf(
            "05:19",
            "12:55",
            "16:19",
            "19:26",
            "00:14"
        )

        val scroll = ScrollView(this)

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
        }

        box.addView(TextView(this).apply {
            text = "Hər namaz üçün azan bildirişini ayrıca açıb-bağlaya bilərsiniz."
            textSize = 13f
            setTextColor(0xff756a64.toInt())
            setPadding(0, 0, 0, dp(12))
        })

        prayers.forEachIndexed { index, name ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(8), dp(8), dp(8))
                background = GradientDrawable().apply {
                    setColor(0xfffff7f2.toInt())
                    cornerRadius = dp(13).toFloat()
                }
            }

            val info = TextView(this).apply {
                text = "$name\n${prefs.getString("${name}_time", defaults[index])}"
                textSize = 14f
                setTextColor(ink)
            }

            val toggle = Switch(this).apply {
                isChecked = AzanPrefs.isPrayerEnabled(
                    this@MainActivity,
                    name
                )

                setOnCheckedChangeListener { _, checked ->
                    AzanPrefs.setPrayerEnabled(
                        this@MainActivity,
                        name,
                        checked
                    )
                }
            }

            row.addView(
                info,
                LinearLayout.LayoutParams(0, -2, 1f)
            )
            row.addView(toggle)

            row.setOnClickListener {
                val edit = EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_DATETIME
                    setText(
                        prefs.getString(
                            "${name}_time",
                            defaults[index]
                        )
                    )
                    hint = "HH:mm"
                }

                AlertDialog.Builder(this)
                    .setTitle("$name vaxtı")
                    .setView(edit)
                    .setPositiveButton("Yadda saxla") { _, _ ->
                        prefs.edit()
                            .putString("${name}_time", edit.text.toString())
                            .apply()

                        AzanScheduler.scheduleAll(this@MainActivity)
                        info.text = "$name\n${edit.text}"
                    }
                    .setNegativeButton("Ləğv et", null)
                    .show()
            }

            box.addView(
                row,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(7)
                }
            )
        }

        box.addView(TextView(this).apply {
            text = "Günəş vaxtı məlumat üçündür və azan bildirişi kimi idarə edilmir."
            textSize = 11.5f
            setTextColor(0xff817872.toInt())
            setPadding(0, dp(5), 0, 0)
        })

        scroll.addView(box)

        AlertDialog.Builder(this)
            .setTitle("Azan ayarları")
            .setView(scroll)
            .setPositiveButton("Hazır", null)
            .show()
    }

    private fun openSection(title: String, description: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(20))
            setBackgroundColor(cream)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val back = TextView(this).apply {
            text = "‹"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(brown)
            setOnClickListener {
                setContentView(home())
            }
        }

        top.addView(back, LinearLayout.LayoutParams(dp(45), dp(50)))

        top.addView(
            TextView(this).apply {
                text = title
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ink)
                gravity = Gravity.CENTER_VERTICAL
            },
            LinearLayout.LayoutParams(0, dp(50), 1f)
        )

        root.addView(top)

        root.addView(TextView(this).apply {
            text = description
            textSize = 14f
            setTextColor(0xff756a64.toInt())
            setPadding(dp(4), dp(12), dp(4), dp(20))
        })

        root.addView(TextView(this).apply {
            text = "Bu bölmə artıq ana səhifədən açılır.\n\nKontent server API-si ilə bu ekrana əlavə olunacaq."
            textSize = 16f
            setTextColor(ink)
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(45), dp(18), dp(45))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
            }
        })

        setContentView(root)
    }

    private fun openZikr() {
        val prefs = getSharedPreferences("zikr", MODE_PRIVATE)
        var count = prefs.getInt("count", 0)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(cream)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        top.addView(
            TextView(this).apply {
                text = "‹"
                textSize = 38f
                gravity = Gravity.CENTER
                setTextColor(brown)
                setOnClickListener { setContentView(home()) }
            },
            LinearLayout.LayoutParams(dp(45), dp(50))
        )

        top.addView(
            TextView(this).apply {
                text = "Zikr"
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ink)
            },
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        root.addView(top, LinearLayout.LayoutParams(-1, -2))

        val zikrName = TextView(this).apply {
            text = "Subhanallah"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(ink)
            setPadding(0, dp(38), 0, dp(12))
        }
        root.addView(zikrName)

        val counter = TextView(this).apply {
            text = count.toString()
            textSize = 54f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(brown)
        }
        root.addView(counter)

        val tap = TextView(this).apply {
            text = "ZİKR ET"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(brown)
            }

            setOnClickListener {
                count++
                counter.text = count.toString()
                prefs.edit().putInt("count", count).apply()

                if (Build.VERSION.SDK_INT >= 26) {
                    performHapticFeedback(
                        android.view.HapticFeedbackConstants.KEYBOARD_TAP
                    )
                }
            }
        }

        root.addView(
            tap,
            LinearLayout.LayoutParams(dp(150), dp(150)).apply {
                topMargin = dp(30)
            }
        )

        val reset = Button(this).apply {
            text = "Sıfırla"
            setOnClickListener {
                count = 0
                counter.text = "0"
                prefs.edit().putInt("count", 0).apply()
            }
        }

        root.addView(
            reset,
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(30)
            }
        )

        setContentView(root)
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

    private data class Bird(
        var x: Float,
        val y: Float,
        val size: Float,
        val speed: Float,
        val phase: Float
    )

    private val birds = mutableListOf<Bird>()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        birds.clear()

        birds.add(Bird(w * .08f, h * .35f, 34f, 2.5f, 0f))
        birds.add(Bird(w * .42f, h * .57f, 27f, 2.0f, 1.7f))
        birds.add(Bird(w * .70f, h * .25f, 31f, 2.8f, 3.2f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val density = resources.displayMetrics.density

        for (bird in birds) {
            bird.x += bird.speed * density

            if (bird.x > width + bird.size * density * 2f) {
                bird.x = -bird.size * density * 2f
            }

            val size = bird.size * density
            val flap = sin(time * 12f + bird.phase)
            val wingY = flap * size * .62f

            canvas.save()
            canvas.translate(bird.x, bird.y)

            // Gövdə
            canvas.drawOval(
                -size * .45f,
                -size * .12f,
                size * .48f,
                size * .14f,
                paint
            )

            // Baş
            canvas.drawCircle(
                size * .47f,
                -size * .04f,
                size * .15f,
                paint
            )

            // Dimdik
            val beak = Path().apply {
                moveTo(size * .60f, -size * .08f)
                lineTo(size * .82f, -size * .02f)
                lineTo(size * .60f, size * .02f)
                close()
            }
            canvas.drawPath(beak, paint)

            // Quyruq
            val tail = Path().apply {
                moveTo(-size * .40f, -size * .07f)
                lineTo(-size * .82f, -size * .34f)
                lineTo(-size * .65f, size * .02f)
                lineTo(-size * .82f, size * .31f)
                lineTo(-size * .38f, size * .09f)
                close()
            }
            canvas.drawPath(tail, paint)

            // Yuxarı qanad
            val upperWing = Path().apply {
                moveTo(-size * .08f, -size * .05f)

                cubicTo(
                    -size * .20f, -size * .22f,
                    -size * .12f, wingY - size * .28f,
                    size * .04f, wingY - size * .48f
                )

                cubicTo(
                    size * .20f, wingY - size * .25f,
                    size * .24f, -size * .12f,
                    size * .24f, -size * .02f
                )

                close()
            }
            canvas.drawPath(upperWing, paint)

            // Aşağı qanad
            val lowerWing = Path().apply {
                moveTo(-size * .03f, size * .05f)

                cubicTo(
                    -size * .14f, size * .20f,
                    -size * .04f, -wingY + size * .28f,
                    size * .12f, -wingY + size * .46f
                )

                cubicTo(
                    size * .27f, -wingY + size * .22f,
                    size * .28f, size * .12f,
                    size * .20f, size * .04f
                )

                close()
            }
            canvas.drawPath(lowerWing, paint)

            canvas.restore()
        }

        time += .075f
        postInvalidateOnAnimation()
    }
}
