package com.alieba.app

import android.app.*
import android.content.Intent
import java.util.Calendar
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
    private var currentPage="home"
    private val brown=0xffb78c43.toInt(); private val ink=0xff153f35.toInt(); private val cream=0xfffffcf5.toInt()
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = 0xff102e39.toInt()
        if(getSharedPreferences("alieba_setup",MODE_PRIVATE).getBoolean("news_notifications",true)) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("alieba_news")
        }

        val setup = getSharedPreferences(
            "alieba_setup",
            MODE_PRIVATE
        )

        if (!setup.getBoolean("completed", false)) {
            setContentView(firstSetup())
            return
        }

        PrayerClock.fetchAndSchedule(this) { if(it && currentPage == "home") showHome() }
        AliebaUpdateChecker.check(this)
        if (intent?.getBooleanExtra("open_news",false)==true) showNews() else showHome()
        // Users upgrading from V12 have already finished the older three-step setup.
        if(setup.getBoolean("notifications",false)) window.decorView.post { if(!isFinishing) showFirstPermissionGuide() }
    }

    override fun onResume() {
        super.onResume()
        // Keep both the original prayer refresh and the new APK installer resumption.
        if (getSharedPreferences("alieba_setup", MODE_PRIVATE).getBoolean("completed", false)) {
            PrayerClock.scheduleToday(this)
        }
        AliebaUpdateChecker.resumePending(this)
    }

    override fun onNewIntent(i:Intent) {super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("open_news",false))showNews()}

    private fun showHome() {
        currentPage="home"
        val v = home()
        setContentView(v)

        // Edge-to-edge screens must reserve BOTH the phone's status-bar and
        // gesture/navigation areas; do not place Alieba icons beneath either.
        window.statusBarColor = 0xff102e39.toInt()
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
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
                if (granted) showFirstPermissionGuide()
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

                PrayerClock.fetchAndSchedule(this@MainActivity) { ready ->
                    if(ready && currentPage=="home") showHome()
                }
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
            setPadding(dp(20), dp(18), dp(20), dp(24))
            background=AliebaPatternDrawable(resources.displayMetrics.density)
        }

        root.addView(LinearLayout(this).apply {
            gravity=Gravity.CENTER_VERTICAL
            orientation=LinearLayout.VERTICAL
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff0d473e.toInt(),0xff247668.toInt())).apply {cornerRadius=dp(22).toFloat()}
            setPadding(dp(18),dp(16),dp(18),dp(16))
            addView(TextView(this@MainActivity).apply {
                text="☪  Alieba"
                textSize=25f;typeface=Typeface.DEFAULT_BOLD;setTextColor(0xfff4d995.toInt())
            })
            addView(TextView(this@MainActivity).apply {
                text="Qəlbinə yaxın bir dünya"
                textSize=13f;setTextColor(Color.WHITE);setPadding(0,dp(6),0,0)
            })
        },LinearLayout.LayoutParams(-1,dp(100)).apply {bottomMargin=dp(18)})
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
            setPadding(dp(20), dp(18), dp(20), dp(24))
            background=AliebaPatternDrawable(resources.displayMetrics.density)
        }

        root.addView(LinearLayout(this).apply {
            gravity=Gravity.CENTER_VERTICAL
            orientation=LinearLayout.VERTICAL
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff0d473e.toInt(),0xff247668.toInt())).apply {cornerRadius=dp(22).toFloat()}
            setPadding(dp(18),dp(16),dp(18),dp(16))
            addView(TextView(this@MainActivity).apply {
                text="☪  Alieba"
                textSize=25f;typeface=Typeface.DEFAULT_BOLD;setTextColor(0xfff4d995.toInt())
            })
            addView(TextView(this@MainActivity).apply {
                text="Namaz vaxtları olduğun məkana uyğun"
                textSize=13f;setTextColor(Color.WHITE);setPadding(0,dp(6),0,0)
            })
        },LinearLayout.LayoutParams(-1,dp(100)).apply {bottomMargin=dp(18)})
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

                saveManualCityLocation(value)
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

    private fun saveManualCityLocation(city: String) {
        Thread {
            try {
                @Suppress("DEPRECATION")
                val results = android.location.Geocoder(this).getFromLocationName(city, 1)
                val location = results?.firstOrNull()
                runOnUiThread {
                    if (location == null) {
                        Toast.makeText(this, "Şəhər tapılmadı. Adı yoxlayın.", Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }
                    getSharedPreferences("alieba_setup", MODE_PRIVATE).edit()
                        .putString("location_mode", "manual")
                        .putString("city", city)
                        .putString("latitude", location.latitude.toString())
                        .putString("longitude", location.longitude.toString())
                        .apply()
                    setContentView(notificationSetup())
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Şəhər müəyyən edilə bilmədi. İnterneti yoxlayın.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun notificationSetup(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
            background=AliebaPatternDrawable(resources.displayMetrics.density)
        }

        root.addView(LinearLayout(this).apply {
            gravity=Gravity.CENTER_VERTICAL
            orientation=LinearLayout.VERTICAL
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff0d473e.toInt(),0xff247668.toInt())).apply {cornerRadius=dp(22).toFloat()}
            setPadding(dp(18),dp(16),dp(18),dp(16))
            addView(TextView(this@MainActivity).apply {
                text="☪  Alieba"
                textSize=25f;typeface=Typeface.DEFAULT_BOLD;setTextColor(0xfff4d995.toInt())
            })
            addView(TextView(this@MainActivity).apply {
                text="Azan səsləri və bildirişlər sənin seçimində"
                textSize=13f;setTextColor(Color.WHITE);setPadding(0,dp(6),0,0)
            })
        },LinearLayout.LayoutParams(-1,dp(100)).apply {bottomMargin=dp(18)})
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
            text = "Namaz vaxtı daxil olduqda Alieba sizə bildiriş göndərə və azan səsləndirə bilər."
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
            text = "Alieba-ya başla"
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

                PrayerClock.fetchAndSchedule(this@MainActivity)
                showHome()
                if(enabled) showFirstPermissionGuide()
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

    private val green=0xff0b4138.toInt()
    private val gold=0xffe7c47b.toInt()
    private fun tileBg(color:Int,r:Int=18):GradientDrawable = GradientDrawable().apply {setColor(color);cornerRadius=dp(r).toFloat()}
    private fun label(text:String,size:Float,color:Int,bold:Boolean=false):TextView = TextView(this).apply {
        this.text=text; textSize=size;setTextColor(color); gravity=Gravity.CENTER
        if(bold) typeface=Typeface.DEFAULT_BOLD
    }
    private fun home(): View {
        val t=PrayerClock.times(this)
        val nowText=java.text.SimpleDateFormat("HH:mm",java.util.Locale.US).format(java.util.Date())
        val dawn=t["sunrise"] ?: "06:00"
        val dusk=t["sunset"] ?: "19:00"
        val night=nowText < dawn || nowText >= dusk
        val root=FrameLayout(this).apply {
            background=AliebaPatternDrawable(resources.displayMetrics.density)
        }
        val scroll=ScrollView(this).apply {isFillViewport=true;clipToPadding=false;setPadding(0,0,0,dp(108))}
        val body=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        val heroHeight=(resources.displayMetrics.heightPixels * .49f).toInt().coerceIn(dp(355),dp(450))
        val hero=FrameLayout(this)
        hero.addView(MosqueSceneView(this,night),FrameLayout.LayoutParams(-1,-1))
        val top=LinearLayout(this).apply {
            gravity=Gravity.CENTER_VERTICAL;orientation=LinearLayout.HORIZONTAL
            setPadding(dp(13),dp(8),dp(17),0)
        }
        val brandIcon=label("☪",23f,0xfff5d999.toInt(),true).apply {
            background=tileBg(0x990e4644.toInt(),13)
        }
        top.addView(brandIcon,LinearLayout.LayoutParams(dp(45),dp(45)))
        val names=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(9),0,0,0)}
        names.addView(label("Alieba App",18f,Color.WHITE,true).apply {gravity=Gravity.START})
        names.addView(label("İmanla yaşa",10f,0xfff3e4bb.toInt()).apply {gravity=Gravity.START})
        top.addView(names,LinearLayout.LayoutParams(0,dp(52),1f))
        val avatar=ImageView(this).apply {
            setImageResource(R.drawable.ic_profile);setColorFilter(Color.WHITE)
            background=tileBg(0xad103d3a.toInt(),28)
            setPadding(dp(12),dp(12),dp(12),dp(12));setOnClickListener{showProfile()}
        }
        top.addView(avatar,LinearLayout.LayoutParams(dp(48),dp(48)))
        hero.addView(top,FrameLayout.LayoutParams(-1,dp(68),Gravity.TOP).apply {topMargin=dp(9)})
        // A single native heading renders sharply on both day and night photos.
        // Do not bake a second copy of the wording into the images.
        val quote=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
            setPadding(dp(8),dp(2),dp(8),dp(2))
        }
        val heading=label("Hər gün Allaha\ndaha yaxın",26f,Color.WHITE,true).apply {
            typeface=Typeface.create("serif",Typeface.BOLD)
            letterSpacing=.014f
            setLineSpacing(dp(1).toFloat(),1.03f)
            setShadowLayer(dp(3).toFloat(),0f,dp(2).toFloat(),0xcc092c3e.toInt())
            includeFontPadding=false
        }
        quote.addView(heading,LinearLayout.LayoutParams(-1,-2))
        val ornament=LinearLayout(this).apply {gravity=Gravity.CENTER;orientation=LinearLayout.HORIZONTAL}
        ornament.addView(View(this).apply {setBackgroundColor(0xfff4d99c.toInt())},LinearLayout.LayoutParams(dp(44),dp(1)))
        ornament.addView(label("✦",13f,0xfff9e2a9.toInt(),true),LinearLayout.LayoutParams(dp(29),dp(19)))
        ornament.addView(View(this).apply {setBackgroundColor(0xfff4d99c.toInt())},LinearLayout.LayoutParams(dp(44),dp(1)))
        quote.addView(ornament,LinearLayout.LayoutParams(-1,dp(21)).apply {topMargin=dp(3)})
        quote.addView(label("Allah zikr edənləri sevir. · Bəqərə, 152",
            11f,0xfffff7e9.toInt()).apply {
            typeface=Typeface.create("serif",Typeface.NORMAL)
            setShadowLayer(dp(2).toFloat(),0f,dp(1).toFloat(),0xcc102a3d.toInt())
        },LinearLayout.LayoutParams(-1,-2))
        hero.addView(quote,FrameLayout.LayoutParams(-1,dp(122),Gravity.TOP).apply {
            topMargin=dp(67);leftMargin=dp(12);rightMargin=dp(12)
        })
        val date=label(java.text.SimpleDateFormat("d MMMM, EEEE",java.util.Locale.forLanguageTag("az")).format(java.util.Date()),12f,Color.WHITE,true).apply {setShadowLayer(4f,0f,2f,0xbb001c20.toInt());setPadding(dp(8),dp(3),dp(8),dp(3))}
        hero.addView(date,FrameLayout.LayoutParams(-2,dp(30),Gravity.END or Gravity.BOTTOM).apply {rightMargin=dp(12);bottomMargin=dp(94)})
        // S-shaped white boundary. Timings float ABOVE the boundary like the reference.
        hero.addView(View(this).apply {background=HeroWaveDrawable(0xfff7faf7.toInt(),dp(23).toFloat())},FrameLayout.LayoutParams(-1,dp(39),Gravity.BOTTOM))
        hero.addView(prayerStrip(),FrameLayout.LayoutParams(-1,dp(73),Gravity.BOTTOM).apply {bottomMargin=dp(23)})
        body.addView(hero,LinearLayout.LayoutParams(-1,heroHeight))
        val sections=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=AliebaPatternDrawable(resources.displayMetrics.density)
            setPadding(dp(11),dp(9),dp(11),dp(18))
        }
        val icons=android.widget.GridLayout(this).apply {
            columnCount=3
            alignmentMode=android.widget.GridLayout.ALIGN_BOUNDS
            useDefaultMargins=false
            setPadding(dp(3),dp(5),dp(3),dp(10))
        }
        val entries=listOf(
            Triple("Quran",R.drawable.ic_quran,0xff16b3a2.toInt()),
            Triple("Məfatih",R.drawable.ic_dua,0xffef8856.toInt()),
            Triple("Əhkam",R.drawable.ic_rules,0xff00a5c8.toInt()),
            Triple("Yeniliklər",R.drawable.ic_calendar,0xffdc449b.toInt()),
            Triple("Mərsiyələr",R.drawable.ic_audio,0xffc74051.toInt()),
            Triple("Hədislər",R.drawable.ic_hadith,0xff2c9a50.toInt()),
            Triple("Kitabxana",R.drawable.ic_library,0xff3257cc.toInt()),
            Triple("Məsləhət",R.drawable.ic_hadith,0xffb69038.toInt()),
            Triple("Kömək et",R.drawable.ic_heart,0xffb77b3f.toInt()),
            Triple("Yadda saxla",R.drawable.ic_heart,0xff8a64b7.toInt()),
            Triple("Zikr və təsbeh",R.drawable.ic_tasbeh,0xff128977.toInt()),
            Triple("Qiblə kompası",R.drawable.ic_qibla,0xff367f91.toInt()),
            Triple("Ayarlar",R.drawable.ic_settings,0xff528d71.toInt())
        )
        entries.forEach { (title,icon,color) ->
            val cell=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
            cell.addView(ImageView(this).apply {setImageResource(icon);setColorFilter(Color.WHITE);background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(color,blend(color,Color.WHITE,.12f))).apply {cornerRadius=dp(20).toFloat();setStroke(dp(1),0x33ffffff)};setPadding(dp(17),dp(17),dp(17),dp(17));elevation=dp(3).toFloat()},LinearLayout.LayoutParams(dp(70),dp(70)))
            cell.addView(label(title,12f,ink),LinearLayout.LayoutParams(-1,dp(25)))
            cell.setOnClickListener {when(title) {"Ayarlar"->showSettings();"Yeniliklər"->showNews();"Məsləhət"->startActivity(Intent(this,NativeAdviceActivity::class.java));"Kömək et"->startActivity(Intent(this,NativeDonateActivity::class.java));"Zikr və təsbeh"->startActivity(Intent(this,ZikrActivity::class.java));"Qiblə kompası"->startActivity(Intent(this,QiblaActivity::class.java));else->openWebsiteSection(title)}}
            icons.addView(cell,android.widget.GridLayout.LayoutParams().apply {
                width=0;height=dp(118)
                columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f)
                setMargins(dp(1),dp(4),dp(1),dp(4))
            })
        }
        sections.addView(icons,LinearLayout.LayoutParams(-1,-2));body.addView(sections)
        scroll.addView(body);root.addView(scroll,FrameLayout.LayoutParams(-1,-1))
        root.addView(bottomWave(),FrameLayout.LayoutParams(-1,dp(87),Gravity.BOTTOM))
        return root
    }
    private fun blend(a:Int,b:Int,f:Float):Int = Color.rgb(
        (Color.red(a)*(1-f)+Color.red(b)*f).toInt(),
        (Color.green(a)*(1-f)+Color.green(b)*f).toInt(),
        (Color.blue(a)*(1-f)+Color.blue(b)*f).toInt()
    )
    private fun prayerStrip():View {
        val t=PrayerClock.times(this)
        val active=PrayerClock.activeKey(this)
        val row=LinearLayout(this).apply {gravity=Gravity.BOTTOM;orientation=LinearLayout.HORIZONTAL;setPadding(dp(3),dp(5),dp(3),dp(2))}
        for(i in PrayerClock.keys.indices) {
            val key=PrayerClock.keys[i]
            val name=PrayerClock.displayNames[i]
            val time=t[key] ?: "--:--"
            val selected=active==key && time!="--:--"
            val background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(if(selected)0xed183c36.toInt() else 0xa20b2429.toInt(),if(selected)0xe8234c42.toInt() else 0xb509171d.toInt())).apply {
                cornerRadius=dp(11).toFloat()
                if(selected)setStroke(dp(2),0xffedcc78.toInt()) else setStroke(dp(1),0x33ffffff)
            }
            val card=LinearLayout(this).apply {gravity=Gravity.CENTER;orientation=LinearLayout.VERTICAL;this.background=background;elevation=if(selected)dp(7).toFloat() else dp(1).toFloat();setOnClickListener{prayerSettings()} }
            card.addView(label(name,8f,if(selected)0xffffe8a3.toInt() else Color.WHITE,selected).apply {setLines(2);setShadowLayer(if(selected)5f else 0f,0f,0f,0xfff2cb74.toInt())},LinearLayout.LayoutParams(-1,dp(32)))
            card.addView(label(time,10f,if(selected)0xffffe09b.toInt() else Color.WHITE,true).apply {setShadowLayer(if(selected)6f else 0f,0f,0f,0xfff2cb74.toInt())},LinearLayout.LayoutParams(-1,dp(19)))
            row.addView(card,LinearLayout.LayoutParams(0,if(selected)dp(66) else dp(59),1f).apply {setMargins(dp(1),0,dp(1),0)})
        }
        return row
    }
    private fun bottomWave():View = AliebaBottomNav.make(this,"home")
    // All reading/news sections are native Android views, not a WebView.
    private fun openWebsiteSection(section:String) {
        val slug=when(section) {
            "Quran"->"quran"; "Məfatih"->"mafatih"; "Əhkam"->"ahkam"
            "Mərsiyələr"->"mersiye"; "Hədislər"->"hadis"
            "Kitabxana"->"kitabxana"; "Yadda saxla"->"saved"
            else->return
        }
        startActivity(Intent(this,NativeContentActivity::class.java).putExtra("section",slug))
    }
    private fun showNews() {
        startActivity(Intent(this,NativeContentActivity::class.java).putExtra("section","news"))
    }
    private fun showSettings() {
        startActivity(Intent(this,NativeSettingsActivity::class.java))
    }
    private fun moreOptions(){
        AlertDialog.Builder(this).setTitle("Alieba").setItems(arrayOf("Profil", "Azan və bildiriş ayarları", "Yeniliklər", "Məkan və dil")) { _,index ->when(index){0->showProfile();1->prayerSettings();2->showNews();3->showSettings()} }.show()
    }

    private fun signInWithGoogle() {
        val webClientId=getString(R.string.default_web_client_id)
        if(webClientId.isBlank()){
            Toast.makeText(this,"Google Client ID quraşdırılmayıb",Toast.LENGTH_LONG).show();return
        }
        val request=GetCredentialRequest.Builder().addCredentialOption(
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()
        ).build()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result=CredentialManager.create(this@MainActivity).getCredential(
                    context=this@MainActivity,request=request)
                val token=GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                firebaseGoogleLogin(token)
            } catch (_: androidx.credentials.exceptions.GetCredentialCancellationException) {
                // The user dismissed account selection.
            } catch (e: Exception) {
                // Some Android/Google Play Services versions have no Credential Manager
                // credentials even when a Google account is on the device. Offer the
                // classic account chooser instead of failing with 'No credentials available'.
                legacyGoogleChooser(webClientId)
            }
        }
    }
    private fun legacyGoogleChooser(webClientId:String){
        try {
            val options=com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId).requestEmail().build()
            val client=com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this,options)
            startActivityForResult(client.signInIntent,640)
        }catch(e:Exception){
            Toast.makeText(this,"Google hesabı seçimi açıla bilmədi: ${e.localizedMessage}",Toast.LENGTH_LONG).show()
        }
    }
    private fun firebaseGoogleLogin(token:String){
        FirebaseAuth.getInstance().signInWithCredential(
            GoogleAuthProvider.getCredential(token,null)
        ).addOnSuccessListener {
            Toast.makeText(this,"Google hesabına giriş edildi",Toast.LENGTH_SHORT).show()
            showProfile()
        }.addOnFailureListener {error ->
            Toast.makeText(this,"Google giriş xətası: ${error.localizedMessage}. Firebase Android SHA-1/SHA-256 imzalarını yoxlayın.",Toast.LENGTH_LONG).show()
        }
    }
    private fun showProfile() {
        val user=FirebaseAuth.getInstance().currentUser
        val sheet=com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val scroll=ScrollView(this).apply {setBackgroundColor(0xfff8f8fa.toInt())}
        val body=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(15),dp(18),dp(35))
        }
        scroll.addView(body)
        val close=label("✕",25f,0xff667078.toInt()).apply {gravity=Gravity.END;setOnClickListener{sheet.dismiss()}}
        body.addView(close,LinearLayout.LayoutParams(-1,dp(38)))
        val photo=ImageView(this).apply {
            setImageResource(R.drawable.ic_profile)
            setColorFilter(0xffe0e5eb.toInt())
            setPadding(dp(22),dp(22),dp(22),dp(22))
            background=GradientDrawable().apply {shape=GradientDrawable.OVAL;setColor(0xff898d93.toInt())}
        }
        body.addView(photo,LinearLayout.LayoutParams(dp(95),dp(95)).apply {gravity=Gravity.CENTER_HORIZONTAL})
        if(user?.photoUrl!=null) Thread {
            val bmp=try {
                val con=(java.net.URL(user.photoUrl.toString()).openConnection() as java.net.HttpURLConnection).apply {connectTimeout=6000;readTimeout=6000}
                try {android.graphics.BitmapFactory.decodeStream(con.inputStream)} finally {con.disconnect()}
            }catch(_:Exception){null}
            if(bmp!=null)runOnUiThread{photo.clearColorFilter();photo.setPadding(0,0,0,0);photo.setImageBitmap(bmp);photo.clipToOutline=true}
        }.start()
        body.addView(label(user?.displayName?.takeIf{it.isNotBlank()}?:"Alieba istifadəçisi",22f,0xff273039.toInt(),true),LinearLayout.LayoutParams(-1,dp(52)))
        body.addView(label(user?.email?:"Hesabınıza daxil olun",13f,0xff7a8591.toInt()),LinearLayout.LayoutParams(-1,dp(30)))
        fun row(title:String,description:String,icon:Int,action:()->Unit) {
            val line=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
                background=tileBg(Color.WHITE,19)
                setPadding(dp(15),dp(12),dp(13),dp(12))
                setOnClickListener {sheet.dismiss();action()}
            }
            line.addView(ImageView(this).apply {setImageResource(icon);setColorFilter(0xff35475c.toInt());setPadding(dp(10),dp(10),dp(10),dp(10));background=tileBg(0xfff0f2f6.toInt(),13)},LinearLayout.LayoutParams(dp(49),dp(49)))
            val words=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(13),0,0,0)}
            words.addView(label(title,16f,0xff202b36.toInt(),true).apply{gravity=Gravity.START})
            words.addView(label(description,12f,0xff84909b.toInt()).apply{gravity=Gravity.START})
            line.addView(words,LinearLayout.LayoutParams(0,-2,1f))
            line.addView(label("›",25f,0xff88929e.toInt()))
            body.addView(line,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(10)})
        }
        if(user==null)row("Google ilə daxil ol","Profil və hesabınızı əlaqələndirin",R.drawable.ic_profile){signInWithGoogle()}
        else row("Hesabım",user.email?:"Google hesabı",R.drawable.ic_profile){Toast.makeText(this,"Google hesabı qoşulub",Toast.LENGTH_SHORT).show()}
        row("Azan ayarları","Namaz vaxtı və səslər",R.drawable.ic_notification_mosque){prayerSettings()}
        row("Dil","Tətbiq dilini seçin",R.drawable.ic_settings){showSettings()}
        row("Bildirişlər","Azan, yeniliklər və icazələr",R.drawable.ic_calendar){showSettings()}
        row("Tətbiq ayarları","Məkan və görünüş",R.drawable.ic_settings){showSettings()}
        row("Yadda saxlananlar","Kitab rəfiniz və Quran ayələri",R.drawable.ic_heart){openWebsiteSection("Yadda saxla")}
        row("Alieba haqqında","Dini məzmun və əlaqə",R.drawable.ic_library){showSettings()}
        if(user!=null)row("Hesabdan çıxış","Google hesabı",R.drawable.ic_profile){FirebaseAuth.getInstance().signOut();showProfile()}
        sheet.setContentView(scroll)
        sheet.show()
        sheet.window?.setLayout(-1,-2)
    }
    private fun showFirstPermissionGuide() {
        val pref=getSharedPreferences("alieba_setup",MODE_PRIVATE)
        if(pref.getBoolean("permission_guide_seen",false))return
        pref.edit().putBoolean("permission_guide_seen",true).apply()
        val panel=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(12),dp(20),dp(15))}
        panel.addView(label("Azan icazələrini tamamlayın",20f,ink,true))
        panel.addView(label("Səsi vaxtında eşitmək üçün Android-də dəqiq siqnal və bildiriş icazələrini yoxlayın. Batareya məhdudiyyəti bəzi telefonlarda gecikməyə səbəb ola bilər.",14f,ink).apply {setPadding(0,dp(12),0,dp(16))})
        fun option(title:String,action:()->Unit){
            panel.addView(label(title,15f,green,true).apply{gravity=Gravity.START or Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(12),0,dp(12));background=tileBg(0xfff2f5f4.toInt(),12);setOnClickListener{action()}},LinearLayout.LayoutParams(-1,dp(55)).apply{bottomMargin=dp(8)})
        }
        option("Bildirişləri aktiv et") {startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName))}
        option("Dəqiq siqnal icazəsi") {promptExactAlarmPermission(force=true)}
        option("Batareya məhdudiyyətlərini yoxla") {startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))}
        option("Üst bildiriş və kilid ekranı") {startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName))}
        AlertDialog.Builder(this).setView(panel).setPositiveButton("Tamamlandı",null).show()
    }
    private fun promptExactAlarmPermission(force:Boolean=false){
        if(Build.VERSION.SDK_INT>=31 && !getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()) {
            val p=getSharedPreferences("alieba_setup",MODE_PRIVATE)
            if(!force && p.getBoolean("exact_alarm_prompt_seen",false))return
            p.edit().putBoolean("exact_alarm_prompt_seen",true).apply()
            AlertDialog.Builder(this).setTitle("Azanı vaxtında səsləndir")
                .setMessage("Android ayarlarında Alieba üçün dəqiq zəng icazəsini açın. İcazə verilməsə, bildiriş gecikə bilər.")
                .setPositiveButton("İcazəni aç") {_,_->startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:$packageName")))}
                .setNegativeButton("Sonra",null).show()
        }
    }
    private fun prayerSettings() {
        val p=getSharedPreferences("alieba_setup",MODE_PRIVATE)
        val box=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(10),dp(18),dp(10))}
        val status=label("Namaz vaxtları: ${if(PrayerClock.times(this).isEmpty()) "API-dən yüklənməyib" else "alieba.ge API"}",13f,ink)
        box.addView(status)
        val global=Switch(this).apply {text="Azan bildirişləri";isChecked=p.getBoolean("notifications",false);setOnCheckedChangeListener { _,b ->p.edit().putBoolean("notifications",b).apply();PrayerClock.scheduleToday(this@MainActivity)}}
        box.addView(global)
        listOf("Fəcr","Zöhr","Əsr","Məğrib","İşa").forEach { name ->
            val sw=Switch(this).apply { text="$name azanı";isChecked=AzanPrefs.isPrayerEnabled(this@MainActivity,name);setOnCheckedChangeListener { _,b ->AzanPrefs.setPrayerEnabled(this@MainActivity,name,b);PrayerClock.scheduleToday(this@MainActivity)}}
            box.addView(sw)
        }
        val news=Switch(this).apply {text="Yenilik xəbər bildirişləri";isChecked=p.getBoolean("news_notifications",true);setOnCheckedChangeListener{_,b->p.edit().putBoolean("news_notifications",b).apply();com.google.firebase.messaging.FirebaseMessaging.getInstance().let { if(b)it.subscribeToTopic("alieba_news") else it.unsubscribeFromTopic("alieba_news") } }}
        box.addView(news)
        val sound=label("Azan səsini seç və ya öz səsini yüklə",15f,green,true)
        box.addView(sound,LinearLayout.LayoutParams(-1,dp(45)))
        sound.setOnClickListener {
            AlertDialog.Builder(this).setTitle("Azan səsi").setItems(arrayOf("Şiə azanı", "Azan 1", "Azan 2", "Telefonumdan azan seç")) {_,which->
                when(which){0->AzanPrefs.setSound(this,"shia");1->AzanPrefs.setSound(this,"beautiful1");2->AzanPrefs.setSound(this,"beautiful2");3->{val pick=Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(pick,98)}}
            }.show()
        }
        val refresh=label("Namaz vaxtlarını yenilə",15f,green,true)
        box.addView(refresh,LinearLayout.LayoutParams(-1,dp(48)))
        refresh.setOnClickListener {status.text="Vaxtlar yüklənir…";PrayerClock.fetchAndSchedule(this){ok->status.text=if(ok)"Bugünkü vaxtlar yeniləndi" else "API əlçatan deyil; vaxtlar təxmin edilmədi.";if(ok)showHome()} }
        val exact=label("Dəqiq zəng icazəsini yoxla",14f,green)
        box.addView(exact,LinearLayout.LayoutParams(-1,dp(46)))
        exact.setOnClickListener {if(Build.VERSION.SDK_INT>=31 && !(getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()))startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:$packageName"))) else Toast.makeText(this,"Dəqiq siqnallar aktivdir",Toast.LENGTH_SHORT).show() }
        AlertDialog.Builder(this).setTitle("Azan və bildirişlər").setView(ScrollView(this).apply{addView(box)}).setPositiveButton("Hazır") {_,_->PrayerClock.scheduleToday(this);showHome()}.show()
    }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==640 && resultCode==RESULT_OK){
            try {
                val account=com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(com.google.android.gms.common.api.ApiException::class.java)
                val token=account.idToken
                if(token.isNullOrBlank()) Toast.makeText(this,"Google ID token alınmadı. Firebase SHA-1 imzasını yoxlayın.",Toast.LENGTH_LONG).show()
                else firebaseGoogleLogin(token)
            }catch(e:Exception){Toast.makeText(this,"Google hesabı seçilmədi: ${e.localizedMessage}",Toast.LENGTH_LONG).show()}
        }
        if(requestCode==98 && resultCode==RESULT_OK && data?.data!=null) {
            val uri=data.data!!
            try{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){}
            getSharedPreferences("azan_settings",MODE_PRIVATE).edit().putString("custom_uri",uri.toString()).apply()
            AzanPrefs.setSound(this,"custom")
            Toast.makeText(this,"Seçdiyiniz azan yadda saxlandı",Toast.LENGTH_SHORT).show()
        }
    }
    @Deprecated("Handled for legacy Android and the section view")
    override fun onBackPressed(){if(currentPage!="home"){showHome()}else super.onBackPressed()}

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
class HeroWaveDrawable(
    private val fill: Int,
    private val amp: Float
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill; style = Paint.Style.FILL }
    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
        val p = Path().apply {
            moveTo(0f, h * .42f)
            cubicTo(w * .18f, h * .08f, w * .34f, h * .78f, w * .52f, h * .43f)
            cubicTo(w * .70f, h * .10f, w * .84f, h * .68f, w, h * .34f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        canvas.drawPath(p, paint)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter }
    override fun getOpacity(): Int = PixelFormat.OPAQUE
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
            cubicTo(w * .18f, amp * .95f, w * .30f, amp * .78f, w * .37f, amp * .72f)
            cubicTo(w * .42f, amp * .68f, w * .43f, 0f, w * .50f, 0f)
            cubicTo(w * .57f, 0f, w * .58f, amp * .68f, w * .63f, amp * .72f)
            cubicTo(w * .70f, amp * .78f, w * .82f, amp * .95f, w, amp)
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

        birds.add(Bird(w * .10f, h * .34f, 18f, 2.2f, 0f))
        birds.add(Bird(w * .38f, h * .54f, 18f, 2.0f, 1.7f))
        birds.add(Bird(w * .68f, h * .28f, 18f, 2.4f, 3.2f))
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

            // V11: eyni ölçülü, yüngül quş silueti — iri "təyyarə" gövdəsi yoxdur
            val wingLift = flap * size * .34f
            val birdPath = Path().apply {
                moveTo(-size * .95f, wingLift * .18f)
                cubicTo(-size * .62f, -size * .10f, -size * .34f, -size * .42f + wingLift, 0f, -size * .08f)
                cubicTo(size * .34f, -size * .42f - wingLift, size * .62f, -size * .10f, size * .95f, wingLift * .18f)
                cubicTo(size * .58f, size * .02f, size * .28f, size * .18f, 0f, size * .10f)
                cubicTo(-size * .28f, size * .18f, -size * .58f, size * .02f, -size * .95f, wingLift * .18f)
                close()
            }
            canvas.drawPath(birdPath, paint)

            canvas.restore()
        }

        time += .075f
        postInvalidateOnAnimation()
    }
}
