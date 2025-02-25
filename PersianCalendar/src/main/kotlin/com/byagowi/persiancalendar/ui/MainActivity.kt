package com.byagowi.persiancalendar.ui

import android.Manifest
import android.app.Activity
import android.app.ComponentCaller
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryBannerAdView
import com.byagowi.persiancalendar.PreferenceManager
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.entities.Jdn
import com.byagowi.persiancalendar.global.initGlobal
import com.byagowi.persiancalendar.global.language
import com.byagowi.persiancalendar.ui.theme.AppTheme
import com.byagowi.persiancalendar.ui.utils.isLight
import com.byagowi.persiancalendar.utils.applyAppLanguage
import com.byagowi.persiancalendar.utils.applyLanguageToConfiguration
import com.byagowi.persiancalendar.utils.eventKey
import com.byagowi.persiancalendar.utils.jdnActionKey
import com.byagowi.persiancalendar.utils.logException
import com.byagowi.persiancalendar.utils.readAndStoreDeviceCalendarEventsOfTheDay
import com.byagowi.persiancalendar.utils.startWorker
import com.byagowi.persiancalendar.utils.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    private val requestPermissionLaunche =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        }
    private lateinit var preferenceManager: PreferenceManager

    private var backButtonPressed = false
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)
    }
    override fun onCreate(savedInstanceState: Bundle?) {


        // Just to make sure we have an initial transparent system bars
        // System bars are tweaked later with project's with real values
        applyEdgeToEdge(isBackgroundColorLight = false, isSurfaceColorLight = true)
        preferenceManager = PreferenceManager(this)

        setTheme(R.style.BaseTheme)
        applyAppLanguage(this)
        super.onCreate(savedInstanceState)

        Adivery.configure(application, "96d0cdb8-ca2c-4bd3-9669-64986c7beecf")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {

                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {

                }
                else -> {
                    requestPermissionLaunche.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        }
        createNotificationChannel(this)
        intent.getLongExtra(eventKey, -1L).takeIf { it != -1L }?.let { eventId ->
            val intent = Intent(Intent.ACTION_VIEW).setData(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            )
            runCatching { startActivity(intent) }.onFailure(logException)
            return finish()
        }

        initGlobal(this)

        startWorker(this)

        readAndStoreDeviceCalendarEventsOfTheDay(applicationContext)
        update(applicationContext, false)

        val initialJdn =
            (intent.getLongExtra(jdnActionKey, -1L).takeIf { it != -1L } ?: intent.action?.takeIf {
                it.startsWith(jdnActionKey)
            }?.replace(jdnActionKey, "")?.toLongOrNull())?.let(::Jdn)
        setContent {
            ExitHandler()

            AppTheme {
                val isBackgroundColorLight = MaterialTheme.colorScheme.background.isLight
                val isSurfaceColorLight = MaterialTheme.colorScheme.surface.isLight

                LaunchedEffect(isBackgroundColorLight, isSurfaceColorLight) {
                    applyEdgeToEdge(isBackgroundColorLight, isSurfaceColorLight)
                }

                val view = LocalView.current
                LaunchedEffect(Unit) {
                    language.collect {
                        onConfigurationChanged(resources.configuration)
                        view.dispatchConfigurationChanged(resources.configuration)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // بنر تبلیغاتی را بالای صفحه نمایش می‌دهیم
                    AndroidView(
                        modifier = Modifier
//                            .fillMaxWidth()
                            .statusBarsPadding(),
                        factory = { context ->
                            AdiveryBannerAdView(context).apply {
                                setPlacementId("6454c01a-3872-4803-86e5-3de2af5d67a2")
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            }
                        },
                        update = { adView ->
                            adView.loadAd()
                        }
                    )

//                    Spacer(modifier = Modifier.height(16.dp))

                    App(intent?.action, initialJdn, ::finish)
                }
            }
        }

        applyAppLanguage(this)

        // There is a window:enforceNavigationBarContrast set to false in styles.xml as the following
        // isn't as effective in dark themes.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "UserInactivityChannel"
            val descriptionText = "Channel for user inactivity notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("USER_INACTIVITY_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun applyEdgeToEdge(isBackgroundColorLight: Boolean, isSurfaceColorLight: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) enableEdgeToEdge(
            if (isBackgroundColorLight)
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            else SystemBarStyle.dark(Color.TRANSPARENT),
            if (isSurfaceColorLight)
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            else SystemBarStyle.dark(Color.TRANSPARENT),
        ) else enableEdgeToEdge( // Just don't tweak navigation bar in older Android versions
            if (isBackgroundColorLight)
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            else SystemBarStyle.dark(Color.TRANSPARENT)
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(applyLanguageToConfiguration(newConfig))
        applyAppLanguage(this)
    }

    override fun onResume() {
        super.onResume()
        applyAppLanguage(this)
        update(applicationContext, false)
        ++resumeToken_.value
    }

    @Composable
    fun ExitHandler() {

        val cafeBazaarLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Toast.makeText(this, "باتشکر از نظر ارزشمندتان", Toast.LENGTH_SHORT).show()
        }

        val emailLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Toast.makeText(this, "باتشکر از نظر ارزشمندتان", Toast.LENGTH_SHORT).show()
        }

//        // لانچر برای کافه‌بازار
//        val cafeBazaarLauncher = rememberLauncherForActivityResult(
//            contract = ActivityResultContracts.StartActivityForResult()
//        ) { result ->
//            Toast.makeText(this, "باتشکر از نظر ارزشمندتان", Toast.LENGTH_SHORT).show()
//        }
        val context = LocalContext.current
        val sharedPreferences = remember { context.getSharedPreferences("exit_prefs", Context.MODE_PRIVATE) }

        // خواندن مقدار ذخیره‌شده برای تعداد خروج‌ها
        var exitCount by remember { mutableStateOf(sharedPreferences.getInt("exit_count", 0)) }
        var showDialog by remember { mutableStateOf(false) }
        var showFeedbackDialog by remember { mutableStateOf(false) }
        var showCriticismDialog by remember { mutableStateOf(false) }

        // بررسی اینکه آیا کاربر قبلاً نظر خود را ثبت کرده است
        val hasSubmittedFeedback = remember { mutableStateOf(sharedPreferences.getBoolean("submitted_feedback", false)) }

        // مدیریت دکمه بازگشت
        BackHandler {
            if (hasSubmittedFeedback.value) {
                // اگر کاربر نظر داده باشد، مستقیماً خارج شود
                exitApp(context)
            } else {
                exitCount++
                sharedPreferences.edit().putInt("exit_count", exitCount).apply()

                if (exitCount == 3) {
                    sharedPreferences.edit().putInt("exit_count", 0).apply()
                    showDialog = true
                } else {
                    exitApp(context)
                }
            }
        }

        // نمایش دیالوگ بعد از 3 بار خروج
        if (showDialog) {
            AlertDialog(
                onDismissRequest = {

                }
                ,
                title = { Text("آیا از برنامه راضی هستید؟") },
                confirmButton = {
                    TextButton(onClick = {
                        sharedPreferences.edit().putBoolean("feedback_yes", true).apply()
                        showDialog = false
                        showFeedbackDialog = true
                    }) {
                        Text("بله")
                    }
                },
                dismissButton = {
                    Column {
                        Row {
                            TextButton(onClick = {
                                sharedPreferences.edit().putBoolean("feedback_no", true).apply()
                                showDialog = false
                                showCriticismDialog = true
                            }) {
                                Text("خیر")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { exitApp(context) }) {
                                Text("خارج می‌شوم", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        }

        // دیالوگ برای ثبت نظر مثبت
        if (showFeedbackDialog) {
            AlertDialog(
                onDismissRequest = {
                    showFeedbackDialog = false
                },
                title = { Text("خیلی ممنون از شما!") },
                text = {
                    Column {
                        Text("ممنون می‌شویم نظرتان را ثبت کنید.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.Center) {
                            repeat(5) { Text("⭐", fontSize = 16.sp) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
//                        val cafeBazaarIntent = Intent(Intent.ACTION_EDIT)
//                        cafeBazaarIntent.data = Uri.parse("bazaar://details?id=" + "com.outfit7.tomsbubbles")
//                        cafeBazaarIntent.setPackage("com.farsitel.bazaar")
//                        cafeBazaarLauncher.launch(cafeBazaarIntent)

//                        val cafeBazaarIntent = Intent(Intent.ACTION_VIEW)
//                        cafeBazaarIntent.data = Uri.parse("bazaar://details?id=com.outfit7.tomsbubbles")
//                        cafeBazaarIntent.setPackage("com.farsitel.bazaar")

                        val packageManager = this.packageManager
                        try {
//                            packageManager.getPackageInfo("com.farsitel.bazaar", PackageManager.GET_ACTIVITIES)
                            // اپلیکیشن کافه بازار نصب است، می‌توانید به آن Intent ارسال کنید
                            val cafeBazaarIntent = Intent(Intent.ACTION_VIEW)
                            cafeBazaarIntent.setData(Uri.parse("bazaar://details?id=" + "com.seyyedstar.calendar"))
                            cafeBazaarIntent.setPackage("com.farsitel.bazaar")
//                            startActivity(cafeBazaarIntent)
                            cafeBazaarLauncher.launch(cafeBazaarIntent)
                        } catch (e: PackageManager.NameNotFoundException) {
                            // کافه بازار نصب نیست
                            Toast.makeText(this, "کافه بازار نصب نیست.", Toast.LENGTH_SHORT).show()
                        }
//                        val intent = Intent(Intent.ACTION_EDIT)
//                        intent.setData(Uri.parse("bazaar://details?id=" + "PACKAGE_NAME"))
//                        intent.setPackage("com.farsitel.bazaar")
//                        startActivity(intent)

//                        showFeedbackDialog = false
                    }) {
                        Text("ثبت نظر")
                    }
                }
            )
        }

        // دیالوگ برای دریافت انتقادات و پیشنهادات
        if (showCriticismDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCriticismDialog = false
                },
                title = { Text("خیلی ممنون از شما!") },
                text = { Text("لطفاً ایرادات و انتقادات خود را برای بهبود برنامه ارسال کنید.") },
                confirmButton = {
                    TextButton(onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:seyyedstarappssuport@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Feedback")
                        }
                        emailLauncher.launch(emailIntent)
                        // لانچر برای ایمیل
                        sharedPreferences.edit().putBoolean("submitted_feedback", true).apply()
                        showCriticismDialog = false

                    }) {
                        Text("ثبت نظر")
                    }
                }
            )
        }
    }

    // تابع خروج از برنامه
    private fun exitApp(context: Context) {
        (context as? Activity)?.finish()
    }

    // تابع باز کردن لینک ثبت نظر
    private fun openWebLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    // تابع ارسال ایمیل برای نظرات کاربران
    private fun sendEmail(context: Context, email: String, subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        context.startActivity(intent)
    }

//    override fun onBackPressed() {
//        val preferenceManager = PreferenceManager(this)
//
//        var valueToSave = preferenceManager.getInt("CounterForShowDialogScore0")
//        if (!backButtonPressed) {
//            backButtonPressed = true
//            valueToSave++
//            preferenceManager.saveInt("CounterForShowDialogScore0", valueToSave)
//            if(preferenceManager.getInt("CounterForShowDialogScore0")<=2){
//                Toast.makeText(this, "برای خروج دوباره بزنید", Toast.LENGTH_SHORT).show()
//            }
//            if (preferenceManager.getInt("CounterForShowDialogScore0")==3){
//                showDialogToExit()
//                preferenceManager.saveInt("CounterForShowDialogScore0",0)
//            }
//
//        } else {
//            super.onBackPressed()
//        }
//    }
    private fun setFounts(context: Context, v: View) {
        if (v is ViewGroup) {
            val vg = v
            for (i in 0 until vg.childCount) {
                val child = vg.getChildAt(i)
                setFounts(context, child)
            }
        } else if (v is TextView) {
            v.typeface = Typeface.createFromAsset(context.assets, "fonts/Yekan.ttf")
        }
    }
//    private fun showDialogToExit() {
//        preferenceManager.saveInt("CounterForShowDialogScore0",0)
//        if (preferenceManager.getBoolean("CounterForNotShowDialogScore1")==false){
//            val binding = DialogToExitBinding.inflate(layoutInflater)
//            val dialog=Dialog(this)
//            dialog.setContentView(binding.root)
//            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
//
//            dialog.show()
//            setFounts(this,binding.root)
//            val exit = dialog.findViewById<Button>(R.id.exit)
//            val score = dialog.findViewById<Button>(R.id.gotoMyAplicationInBazaar)
//            val notScore = dialog.findViewById<Button>(R.id.gotoMyEmail)
//            binding.exit.setOnClickListener {
//                dialog.dismiss()
//                finish()
//            }
//
//            binding.gotoMyAplicationInBazaar.setOnClickListener {
//                try {
//                    if (preferenceManager.getBoolean("CounterForNotShowDialogScore1")==false){
//                        val binding2 = ScoreDialogBinding.inflate(layoutInflater)
//                        val dialog2=Dialog(this)
//                        dialog.dismiss()
//                        dialog2.setContentView(binding2.root)
//                        dialog2.show()
//                        setFounts(this,binding2.root)
//                        binding2.gettingScore.setOnClickListener {
//                            val intent = Intent(Intent.ACTION_EDIT)
//                            intent.data = Uri.parse("bazaar://details?id=" + "com.seyyedstar.impcleaner")
//                            intent.setPackage("com.farsitel.bazaar")
//                            Toast.makeText(this, "لطفا نظرات و پیشنهاداتتان به برنامه را ثبت کنید", Toast.LENGTH_SHORT).show()
//                            startActivityForResult(intent,7)
//                            dialog2.dismiss()
//                        }
//
//
//                    }else{
//                        Toast.makeText(this, "شما قبلا نظر دادید", Toast.LENGTH_SHORT).show()
//                    }
//
//
//                } catch (e: ActivityNotFoundException) {
//                    Toast.makeText(this, "کافه بازار نصب نیست", Toast.LENGTH_SHORT).show()
//                }
//            }
//            binding.gotoMyEmail.setOnClickListener {
//                val preferenceManager = PreferenceManager(this)
//                val valueToNotShowScore = preferenceManager.getBoolean("CounterForNotShowDialogScore1")
//                if (!valueToNotShowScore){
//                    preferenceManager.saveBoolean("CounterForNotShowDialogScore1",true)
//                    val dialog3=Dialog(this)
//                    val binding3 = NoScoreDialogBinding.inflate(layoutInflater)
//                    dialog.dismiss()
//                    dialog3.setContentView(binding3.root)
//                    dialog3.show()
//                    setFounts(this,binding3.root)
//                    binding3.gettingError.setOnClickListener {
//                        val email = "seyyedstarappssuport@gmail.com"
//                        val intent = Intent(Intent.ACTION_SENDTO)
//                        intent.data = Uri.parse("mailto:$email") // تنظیم آدرس ایمیل
//                        intent.putExtra(Intent.EXTRA_SUBJECT, "مشکلات برنامه") // می‌توانید موضوع ایمیل را هم تنظیم کنید
//
//                        try {
//                            Toast.makeText(this, "لطفا نظرات و انتقادات و ایرادات و پیشنهاداتتان به برنامه را ثبت کنید", Toast.LENGTH_SHORT).show()
//                            startActivityForResult(intent,7)
//                            dialog3.dismiss()
////                            finish()
//                        } catch (e: ActivityNotFoundException) {
//                            Toast.makeText(this, "خطا در ارتباط با ایمیل", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//
//
//                }
//                //                else{
////                    Toast.makeText(this, "شما قبلا نظر دادید", Toast.LENGTH_SHORT).show()
////                }
//
//            }
//        }else{
//            Toast.makeText(this, "برای خروج دوباره بزنید", Toast.LENGTH_SHORT).show()
//        }
//
//    }

}

private val resumeToken_ = MutableStateFlow(0)
val resumeToken: StateFlow<Int> = resumeToken_
