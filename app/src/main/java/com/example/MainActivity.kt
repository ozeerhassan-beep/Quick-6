package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp
import com.example.data.AppDatabase
import com.example.data.CatalogRepository
import com.example.ui.AppThemeMode
import com.example.ui.CatalogManagerNavHost
import com.example.ui.CatalogViewModel
import com.example.ui.CatalogViewModelFactory
import com.example.ui.FontStyleOption
import com.example.ui.theme.CatalogManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(applicationContext).isEmpty()) {
                val initialized = try {
                    FirebaseApp.initializeApp(applicationContext)
                } catch (e: Exception) {
                    null
                }
                if (initialized == null) {
                    try {
                        val options = com.google.firebase.FirebaseOptions.Builder()
                            .setApplicationId("1:281205414143:android:b8f526484d02b0cf9a54ca")
                            .setProjectId("shopping-cart-c4900")
                            .setApiKey("AIzaSyDSuxDEzQtD-TjOXG055LlvcCO2DYM5uZ4")
                            .setStorageBucket("shopping-cart-c4900.firebasestorage.app")
                            .setGcmSenderId("281205414143")
                            .build()
                        FirebaseApp.initializeApp(applicationContext, options)
                    } catch (e: Throwable) {
                        android.util.Log.w("MainActivity", "Firebase options init fallback: ${e.message}")
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "FirebaseApp init skipped: ${e.message}")
        }
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = CatalogRepository(database.catalogDao())
        val factory = CatalogViewModelFactory(repository, applicationContext)
        val viewModel = ViewModelProvider(this, factory)[CatalogViewModel::class.java]

        // Initialize Firebase AppCheck and FCM asynchronously so startup is immediate
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
                val isDebug = (applicationContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebug) {
                    firebaseAppCheck.installAppCheckProviderFactory(
                        com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                    )
                    android.util.Log.d("MainActivity", "Firebase App Check initialized with Debug Provider")
                } else {
                    firebaseAppCheck.installAppCheckProviderFactory(
                        com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                    android.util.Log.d("MainActivity", "Firebase App Check initialized with Play Integrity Provider")
                }
            } catch (e: Throwable) {
                android.util.Log.w("MainActivity", "Firebase App Check initialization skipped: ${e.message}")
            }

            try {
                val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                val resultCode = availability.isGooglePlayServicesAvailable(applicationContext)
                if (resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                android.util.Log.d("MainActivity", "FCM Registration Token: ${task.result}")
                            } else {
                                android.util.Log.w("MainActivity", "FCM registration token retrieval skipped: ${task.exception?.message}")
                            }
                        }
                }
            } catch (e: Throwable) {
                android.util.Log.w("MainActivity", "FCM token initialization skipped: ${e.message}")
            }
        }

        // Request notification permission on Android 13+ (API 33+)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "Notification permission check error: ${e.message}")
        }

        handleIncomingIntent(intent, viewModel)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val primaryTheme by viewModel.primaryColorTheme.collectAsState()
            val fontStyleOpt by viewModel.fontStyleOption.collectAsState()
            val fontScaleOpt by viewModel.fontScaleOption.collectAsState()
            val cardShapeOpt by viewModel.cardShapeOption.collectAsState()
            val dpiScaleMode by viewModel.dpiScaleMode.collectAsState()
            val dpiCustomScale by viewModel.dpiCustomScale.collectAsState()

            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            val fontFamily = when (fontStyleOpt) {
                FontStyleOption.SANS_SERIF -> FontFamily.SansSerif
                FontStyleOption.SERIF -> FontFamily.Serif
                FontStyleOption.MONOSPACE -> FontFamily.Monospace
            }

            CatalogManagerTheme(
                darkTheme = isDarkTheme,
                primaryColor = Color(primaryTheme.primaryColorHex),
                primaryContainerColor = Color(primaryTheme.containerHex),
                fontFamily = fontFamily,
                fontScaleMultiplier = fontScaleOpt.multiplier,
                cornerRadiusDp = cardShapeOpt.cornerRadiusDp,
                dpiScaleMode = dpiScaleMode,
                dpiCustomScale = dpiCustomScale
            ) {
                CatalogManagerNavHost(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val factory = CatalogViewModelFactory(
            CatalogRepository(AppDatabase.getDatabase(applicationContext, lifecycleScope).catalogDao()),
            applicationContext
        )
        val vm = ViewModelProvider(this, factory)[CatalogViewModel::class.java]
        handleIncomingIntent(intent, vm)
    }

    private fun handleIncomingIntent(intent: android.content.Intent?, vm: CatalogViewModel) {
        if (intent == null) return
        val action = intent.action ?: return

        when (action) {
            android.content.Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    vm.addIncomingExternalUris(applicationContext, listOf(uri))
                }
            }
            android.content.Intent.ACTION_SEND -> {
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM) as? android.net.Uri
                }
                if (uri != null) {
                    vm.addIncomingExternalUris(applicationContext, listOf(uri))
                }
            }
            android.content.Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) {
                    vm.addIncomingExternalUris(applicationContext, uris)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        com.example.util.UserDensityManager.startTracking(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        com.example.util.UserDensityManager.startTracking(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        com.example.util.UserDensityManager.stopTracking(applicationContext)
    }

    override fun onStop() {
        super.onStop()
        com.example.util.UserDensityManager.stopTracking(applicationContext)
    }
}

