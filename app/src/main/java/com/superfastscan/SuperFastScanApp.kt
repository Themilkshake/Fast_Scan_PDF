package com.superfastscan

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.superfastscan.data.local.datastore.SettingsDataStore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SuperFastScanApp : Application() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate() {
        super.onCreate()
        initAppLocale()
    }

    private fun initAppLocale() {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (currentLocales.isEmpty) {
            val defaultLanguage = settingsDataStore.getSystemDefaultLanguage()
            val localeList = LocaleListCompat.forLanguageTags(defaultLanguage.code)
            AppCompatDelegate.setApplicationLocales(localeList)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val localeManager = getSystemService(android.app.LocaleManager::class.java)
                localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(defaultLanguage.code)
            }
        }
    }
}
