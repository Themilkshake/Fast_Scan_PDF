package com.superfastscan.data.local.datastore

import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.superfastscan.domain.model.AppLanguage
import com.superfastscan.domain.model.AppThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
        val INITIAL_SETUP_COMPLETED = booleanPreferencesKey("initial_setup_completed")
        val IS_PREMIUM_USER = booleanPreferencesKey("is_premium_user")
        val COMPLETED_SCAN_COUNT = intPreferencesKey("completed_scan_count")
        val REVIEW_PROMPTED = booleanPreferencesKey("review_prompted")
    }

    fun getSystemDefaultLanguage(): AppLanguage {
        // 1. If an application-specific locale has been set via AppCompatDelegate, respect it
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val appLang = appLocales[0]?.language?.lowercase(Locale.ROOT) ?: ""
            if (appLang.startsWith("tr")) {
                return AppLanguage.TURKISH
            } else if (appLang.startsWith("en")) {
                return AppLanguage.ENGLISH
            }
        }

        // 2. Otherwise determine default based on phone's system language
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val locales = Resources.getSystem().configuration.locales
            if (!locales.isEmpty) locales[0] else Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locale ?: Locale.getDefault()
        }
        val lang = systemLocale?.language?.lowercase(Locale.ROOT) ?: ""
        return if (lang.startsWith("tr")) {
            AppLanguage.TURKISH
        } else {
            AppLanguage.ENGLISH
        }
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppThemeMode.fromStorageKey(preferences[Keys.THEME_MODE])
        }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.storageKey
        }
    }

    val language: Flow<AppLanguage> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val savedCode = preferences[Keys.LANGUAGE]
            if (savedCode != null) {
                AppLanguage.fromCode(savedCode, getSystemDefaultLanguage())
            } else {
                getSystemDefaultLanguage()
            }
        }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LANGUAGE] = language.code
        }
    }

    val defaultExportFormat: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.DEFAULT_EXPORT_FORMAT] ?: "PDF"
        }

    suspend fun setDefaultExportFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_EXPORT_FORMAT] = format
        }
    }

    val isInitialSetupCompleted: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.INITIAL_SETUP_COMPLETED] ?: false
        }

    suspend fun setInitialSetupCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.INITIAL_SETUP_COMPLETED] = completed
        }
    }

    val isPremiumUser: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.IS_PREMIUM_USER] ?: false
        }

    suspend fun setPremiumUser(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_PREMIUM_USER] = isPremium
        }
    }

    val completedScanCount: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.COMPLETED_SCAN_COUNT] ?: 0
        }

    suspend fun incrementCompletedScanCount(): Int {
        var newCount = 0
        context.dataStore.edit { preferences ->
            val current = preferences[Keys.COMPLETED_SCAN_COUNT] ?: 0
            newCount = current + 1
            preferences[Keys.COMPLETED_SCAN_COUNT] = newCount
        }
        return newCount
    }

    val isReviewPrompted: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.REVIEW_PROMPTED] ?: false
        }

    suspend fun setReviewPrompted(prompted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.REVIEW_PROMPTED] = prompted
        }
    }

    suspend fun resetReviewState() {
        context.dataStore.edit { preferences ->
            preferences[Keys.COMPLETED_SCAN_COUNT] = 0
            preferences[Keys.REVIEW_PROMPTED] = false
        }
    }
}
