package com.superfastscan

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.superfastscan.navigation.AppNavHost
import com.superfastscan.ui.theme.SuperFastScanTheme
import dagger.hilt.android.AndroidEntryPoint

import com.superfastscan.ads.AdManager
import com.superfastscan.data.local.datastore.SettingsDataStore
import com.superfastscan.domain.model.AppThemeMode
import com.superfastscan.widget.QuickScanWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.content.Intent
import javax.inject.Inject

// Static background gradient colors — no per-frame GPU cost
private val BgGradientStart = Color(0xFF0F172A)  // Deep Navy
private val BgGradientMid = Color(0xFF0B1120)    // Dark Slate
private val BgGradientEnd = Color(0xFF020617)    // Near Black

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val _startScanTrigger = MutableStateFlow(false)
    val startScanTrigger = _startScanTrigger.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleScanIntent(intent)
        
        // Initialize AdMob
        adManager.initialize(this)
        
        // Ensure navigation bar and status bar are permanently visible and do not overlap the app
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

        setContent {
            val autoStartScan by _startScanTrigger.collectAsStateWithLifecycle()
            val themeMode by settingsDataStore.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            DisposableEffect(isDarkTheme) {
                windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
                window.statusBarColor = if (isDarkTheme) {
                    android.graphics.Color.parseColor("#0F172A")
                } else {
                    android.graphics.Color.parseColor("#F8FAFC")
                }
                window.navigationBarColor = if (isDarkTheme) {
                    android.graphics.Color.parseColor("#020617")
                } else {
                    android.graphics.Color.parseColor("#F1F5F9")
                }
                onDispose {}
            }

            val bgGradient = remember(isDarkTheme) {
                Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(BgGradientStart, BgGradientMid, BgGradientEnd)
                    } else {
                        listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE2E8F0))
                    }
                )
            }

            SuperFastScanTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = bgGradient)
                ) {
                    AppNavHost(
                        adManager = adManager,
                        autoStartScan = autoStartScan,
                        onAutoScanConsumed = { _startScanTrigger.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleScanIntent(intent)
    }

    private fun handleScanIntent(intent: Intent?) {
        if (intent?.action == QuickScanWidgetProvider.ACTION_START_SCAN ||
            intent?.getBooleanExtra(QuickScanWidgetProvider.EXTRA_START_SCAN, false) == true
        ) {
            _startScanTrigger.value = true
        }
    }
}

