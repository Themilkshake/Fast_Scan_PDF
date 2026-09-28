package com.superfastscan.data.repository

import com.superfastscan.data.local.datastore.SettingsDataStore
import com.superfastscan.domain.model.AppLanguage
import com.superfastscan.domain.model.AppThemeMode
import com.superfastscan.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun getSystemDefaultLanguage(): AppLanguage =
        settingsDataStore.getSystemDefaultLanguage()

    override fun getThemeMode(): Flow<AppThemeMode> =
        settingsDataStore.themeMode

    override suspend fun setThemeMode(themeMode: AppThemeMode) {
        settingsDataStore.setThemeMode(themeMode)
    }

    override fun getLanguage(): Flow<AppLanguage> =
        settingsDataStore.language

    override suspend fun setLanguage(language: AppLanguage) {
        settingsDataStore.setLanguage(language)
    }

    override fun getDefaultFormat(): Flow<String> =
        settingsDataStore.defaultExportFormat

    override suspend fun setDefaultFormat(format: String) {
        settingsDataStore.setDefaultExportFormat(format)
    }

    override fun isInitialSetupCompleted(): Flow<Boolean> =
        settingsDataStore.isInitialSetupCompleted

    override suspend fun setInitialSetupCompleted(completed: Boolean) {
        settingsDataStore.setInitialSetupCompleted(completed)
    }

    override fun getCompletedScanCount(): Flow<Int> =
        settingsDataStore.completedScanCount

    override suspend fun incrementCompletedScanCount(): Int =
        settingsDataStore.incrementCompletedScanCount()

    override fun isReviewPrompted(): Flow<Boolean> =
        settingsDataStore.isReviewPrompted

    override suspend fun setReviewPrompted(prompted: Boolean) {
        settingsDataStore.setReviewPrompted(prompted)
    }

    override suspend fun resetReviewState() {
        settingsDataStore.resetReviewState()
    }
}
