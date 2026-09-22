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

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )
        window.statusBarColor = Color.TRANSPARENT
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
        if (intent?.getBooleanExtra("open_news",false)==true) showNews() else showHome()
    }

    override fun onNewIntent(i:Intent) {super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("open_news",false))showNews()}

    private fun showHome() {
        currentPage="home"
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
                if (granted) promptExactAlarmPermission()
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
            text = "Alieba"
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
            text = "Alieba"
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
            setPadding(dp(24), dp(60), dp(24), dp(28))
            setBackgroundColor(cream)
        }

        root.addView(TextView(this).apply {
            text = "Alieba"
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
                if(enabled && (Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED)) promptExactAlarmPermission()
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
        val nowText=java.text.SimpleDateFormat("HH:mm",java.util.Locale.US).format(java.util.Date())
        val schedule=PrayerClock.times(this)
        val rising=schedule["sunrise"] ?: "06:00"
        val setting=schedule["sunset"] ?: "19:00"
        val night=nowText < rising || nowText >= setting
        val root=FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        val scroll=ScrollView(this).apply { isFillViewport=true;clipToPadding=false;setPadding(0,0,0,dp(76)) }
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val displayHeight=resources.displayMetrics.heightPixels
        val heroHeight=(displayHeight*0.59f).toInt().coerceAtLeast(dp(370))
        val hero=FrameLayout(this)
        // Reuse the original mosque photograph; apply a night overlay after sunset.
        hero.addView(ImageView(this).apply {setImageResource(R.drawable.mosque_bg);scaleType=ImageView.ScaleType.CENTER_CROP}, FrameLayout.LayoutParams(-1,-1))
        if(night) {
            hero.addView(View(this).apply {setBackgroundColor(0xb6001025.toInt())},FrameLayout.LayoutParams(-1,-1))
            hero.addView(MosqueNightLights(this),FrameLayout.LayoutParams(-1,-1))
        } else hero.addView(View(this).apply {background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0x660c302b,0x05000000,0x99081714.toInt()))},FrameLayout.LayoutParams(-1,-1))
        val top=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL;orientation=LinearLayout.HORIZONTAL;setPadding(dp(16),dp(12),dp(16),0)}
        top.addView(label("☪  Alieba",21f,Color.WHITE,true),LinearLayout.LayoutParams(0,dp(48),1f))
        val avatar=ImageView(this).apply {setImageResource(R.drawable.ic_profile);setColorFilter(Color.WHITE);background=tileBg(0x77000000,30);setPadding(dp(11),dp(11),dp(11),dp(11));setOnClickListener {showProfile()} }
        top.addView(avatar,LinearLayout.LayoutParams(dp(44),dp(44)))
        hero.addView(top,FrameLayout.LayoutParams(-1,dp(65),Gravity.TOP))
        val quote=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(10),0,dp(10),0) }
        quote.addView(label(if(night) "Həyatını Allahın rəngi ilə boya" else "Hər gün Allaha daha yaxın",24f,Color.WHITE),LinearLayout.LayoutParams(-1,-2))
        quote.addView(label(if(night) "“Həqiqətən, Allah qəlbləri zikrlə rahatladır.”" else "“Allah zikr edənləri sevir.”",12f,Color.WHITE),LinearLayout.LayoutParams(-1,-2).apply {topMargin=dp(7)})
        hero.addView(quote,FrameLayout.LayoutParams(-1,dp(100),Gravity.TOP).apply {topMargin=dp(90)})
        val date=label(java.text.SimpleDateFormat("d MMMM, EEEE",java.util.Locale.forLanguageTag("az")).format(java.util.Date()),13f,Color.WHITE,true).apply {background=tileBg(0x99091212.toInt(),13);setPadding(dp(12),dp(4),dp(12),dp(4))}
        hero.addView(date,FrameLayout.LayoutParams(-2,dp(37),Gravity.END or Gravity.CENTER_VERTICAL).apply {rightMargin=dp(12);topMargin=dp(45)})
        hero.addView(prayerStrip(),FrameLayout.LayoutParams(-1,dp(83),Gravity.BOTTOM).apply {bottomMargin=dp(5)})
        body.addView(hero,LinearLayout.LayoutParams(-1,heroHeight))
        val sections=HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false;setBackgroundColor(Color.WHITE)}
        val icons=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(10),dp(8),dp(5))}
        val entries=listOf(Triple("Quran",R.drawable.ic_quran,0xff16b3a2.toInt()),Triple("Məfatih",R.drawable.ic_dua,0xfff58a50.toInt()),Triple("Əhkam",R.drawable.ic_rules,0xff00a5d2.toInt()),Triple("Yeniliklər",R.drawable.ic_calendar,0xffdc449b.toInt()),Triple("Mərsiyələr",R.drawable.ic_audio,0xffd53644.toInt()),Triple("Hədislər",R.drawable.ic_hadith,0xff2c9a50.toInt()),Triple("Kitabxana",R.drawable.ic_library,0xff3257cc.toInt()),Triple("Ayarlar",R.drawable.ic_settings,0xff8a64b7.toInt()))
        entries.forEach { (title,icon,color) ->
            val cell=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
            cell.addView(ImageView(this).apply {setImageResource(icon);setColorFilter(Color.WHITE);background=tileBg(color,12);setPadding(dp(11),dp(11),dp(11),dp(11))},LinearLayout.LayoutParams(dp(47),dp(47)))
            cell.addView(label(title,10.5f,ink),LinearLayout.LayoutParams(dp(77),dp(24)))
            cell.setOnClickListener { when(title) {
                "Ayarlar"->prayerSettings();"Yeniliklər"->showNews()
                else->openWebsiteSection(title)
            } }
            icons.addView(cell,LinearLayout.LayoutParams(dp(77),dp(80)))
        }
        sections.addView(icons);body.addView(sections)
        scroll.addView(body);root.addView(scroll,FrameLayout.LayoutParams(-1,-1))
        root.addView(bottomWave(),FrameLayout.LayoutParams(-1,dp(79),Gravity.BOTTOM))
        return root
    }
    private fun prayerStrip(): View {
        val t=PrayerClock.times(this)
        val row=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL;orientation=LinearLayout.HORIZONTAL;setPadding(dp(3),dp(3),dp(3),dp(4))}
        for(i in PrayerClock.keys.indices) {
            val name=PrayerClock.displayNames[i]
            val time=t[PrayerClock.keys[i]]?:"--:--"
            val item=LinearLayout(this).apply {gravity=Gravity.CENTER;orientation=LinearLayout.VERTICAL;background=tileBg(0xb4000000.toInt(),12);setOnClickListener {prayerSettings()} }
            item.addView(label(name,10f,Color.WHITE),LinearLayout.LayoutParams(-1,dp(22)))
            item.addView(label(time,11f,if(time=="--:--") 0xfff4aa99.toInt() else Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(22)))
            row.addView(item,LinearLayout.LayoutParams(0,dp(63),1f).apply {setMargins(dp(2),0,dp(2),0)})
        }
        return row
    }
    private fun bottomWave(): View {
        val nav=LinearLayout(this).apply {gravity=Gravity.CENTER;orientation=LinearLayout.HORIZONTAL;background=tileBg(Color.WHITE,20);elevation=dp(10).toFloat()}
        fun add(title:String,icon:Int,action:()->Unit){
            val b=LinearLayout(this).apply {gravity=Gravity.CENTER;orientation=LinearLayout.VERTICAL}
            b.addView(ImageView(this).apply {setImageResource(icon);setColorFilter(if(title=="Ana səhifə") 0xff255fd2.toInt() else ink)},LinearLayout.LayoutParams(dp(24),dp(24)))
            b.addView(label(title,10f,ink),LinearLayout.LayoutParams(-1,dp(20)))
            b.setOnClickListener {action()}; nav.addView(b,LinearLayout.LayoutParams(0,-1,1f))
        }
        add("Ana səhifə",R.drawable.ic_home){showHome()}
        add("Yadda saxla",R.drawable.ic_heart){openWebsiteSection("Profilim")}
        val center=ImageView(this).apply {setImageResource(R.drawable.ic_notification_mosque);scaleType=ImageView.ScaleType.CENTER_INSIDE;setPadding(dp(13),dp(13),dp(13),dp(13));background=tileBg(0xff1a58c8.toInt(),44);clipToOutline=true;setOnClickListener {showHome()} }
        nav.addView(center,LinearLayout.LayoutParams(dp(58),dp(58)).apply {setMargins(dp(6),0,dp(6),0)})
        add("Sevimlilər",R.drawable.ic_heart){openWebsiteSection("Profilim")}
        add("Daha çox",R.drawable.ic_settings){moreOptions()}
        return nav
    }
    private fun openWebsiteSection(section:String) {
        val path=when(section){"Quran"->"quran";"Məfatih"->"mafatih";"Əhkam"->"ahkam";"Mərsiyələr"->"mersiye";"Hədislər"->"hadis";"Kitabxana"->"kitabxana";"Profilim"->"profil";else->""}
        if(path.isEmpty())return
        currentPage="section"
        val root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;background=tileBg(Color.WHITE)}
        root.addView(label("‹    $section",20f,green,true).apply {gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),0,0,0);setOnClickListener{showHome()}},LinearLayout.LayoutParams(-1,dp(54)))
        val web=android.webkit.WebView(this).apply {
            settings.javaScriptEnabled=true;settings.domStorageEnabled=true
            webViewClient=object:android.webkit.WebViewClient(){override fun shouldOverrideUrlLoading(v:android.webkit.WebView?,r:android.webkit.WebResourceRequest?):Boolean {
                val u=r?.url ?: return false
                if(u.host=="alieba.ge")return false
                startActivity(Intent(Intent.ACTION_VIEW,u));return true
            }}
            loadUrl("https://alieba.ge/$path")
        }
        root.addView(web,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
    }
    private fun showNews() {
        currentPage="news"
        val root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setBackgroundColor(cream);setPadding(dp(15),dp(12),dp(15),0)}
        root.addView(label("‹    Alieba yenilikləri",23f,green,true).apply {gravity=Gravity.CENTER_VERTICAL;setOnClickListener{showHome()}},LinearLayout.LayoutParams(-1,dp(54)))
        val content=label("Yeniliklər yüklənir…",15f,ink)
        val scroll=ScrollView(this);scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        Thread {
            val text=try {
                val conn=(java.net.URL("https://alieba.ge/api/news.php").openConnection() as java.net.HttpURLConnection).apply{connectTimeout=7000;readTimeout=7000}
                val json=org.json.JSONObject(conn.inputStream.bufferedReader().use{it.readText()})
                conn.disconnect()
                val a=json.optJSONArray("items")
                if(a==null || a.length()==0) "Hələ yenilik yoxdur." else (0 until a.length()).joinToString("\n\n") { j -> val n=a.getJSONObject(j);"${n.optString("title")}\n${n.optString("body")}" }
            } catch (_:Exception){"Xəbər xidməti hələ qoşulmayıb. Sonra yenidən yoxlayın."}
            runOnUiThread{content.text=text;content.gravity=Gravity.START;content.setPadding(dp(8),dp(10),dp(8),dp(10))}
        }.start()
    }
    private fun moreOptions(){
        AlertDialog.Builder(this).setTitle("Alieba").setItems(arrayOf("Profil", "Azan və bildiriş ayarları", "Yeniliklər", "Məkan və dil")) { _,index ->when(index){0->showProfile();1->prayerSettings();2->showNews();3->setContentView(firstSetup())} }.show()
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
            text = user?.displayName ?: "Alieba istifadəçisi"
            textSize = 20f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        })

        box.addView(TextView(this).apply {
            text = if (user != null) {
                "${user.email ?: ""}\nAlieba Coin: 0  •  Premium: Aktiv deyil"
            } else {
                "Hesaba daxil olun\nAlieba Coin: 0"
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
            "Alieba Coin və Premium",
            "Balans, Premium və gələcək xidmətlər"
        ) {
            Toast.makeText(this, "Coin və Premium", Toast.LENGTH_SHORT).show()
        }

        addItem(
            "Tətbiq ayarları",
            "Dil, görünüş və digər seçimlər"
        ) {
            moreOptions()
        }

        addItem(
            "Proqrama dəstək",
            "Alieba layihəsinə dəstək və əlaqə"
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

    private fun promptExactAlarmPermission(){
        if(Build.VERSION.SDK_INT>=31 && !getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()) {
            val p=getSharedPreferences("alieba_setup",MODE_PRIVATE)
            if(p.getBoolean("exact_alarm_prompt_seen",false))return
            p.edit().putBoolean("exact_alarm_prompt_seen",true).apply()
            AlertDialog.Builder(this).setTitle("Azanı vaxtında səsləndir")
                .setMessage("Android ayarlarında Alieba üçün dəqiq zəng icazəsini açın. İcazə verilməsə, bildiriş gecikə bilər.")
                .setPositiveButton("İcazəni aç") {_,_->startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:$packageName")))}
                .setNegativeButton("Sonra",null).show()
        }
    }
    override fun onResume(){
        super.onResume()
        if(getSharedPreferences("alieba_setup",MODE_PRIVATE).getBoolean("completed",false)) PrayerClock.scheduleToday(this)
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
