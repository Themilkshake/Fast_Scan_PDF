package com.superfastscan.domain.repository

import com.superfastscan.domain.model.AppLanguage
import com.superfastscan.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSystemDefaultLanguage(): AppLanguage
    fun getThemeMode(): Flow<AppThemeMode>
    suspend fun setThemeMode(themeMode: AppThemeMode)
    fun getLanguage(): Flow<AppLanguage>
    suspend fun setLanguage(language: AppLanguage)
    fun getDefaultFormat(): Flow<String>
    suspend fun setDefaultFormat(format: String)
    fun isInitialSetupCompleted(): Flow<Boolean>
    suspend fun setInitialSetupCompleted(completed: Boolean)
    fun getCompletedScanCount(): Flow<Int>
    suspend fun incrementCompletedScanCount(): Int
    fun isReviewPrompted(): Flow<Boolean>
    suspend fun setReviewPrompted(prompted: Boolean)
    suspend fun resetReviewState()
}
