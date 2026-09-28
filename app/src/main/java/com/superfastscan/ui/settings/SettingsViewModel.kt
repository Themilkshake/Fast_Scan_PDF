package com.superfastscan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Activity
import com.superfastscan.domain.model.AppLanguage
import com.superfastscan.domain.model.AppThemeMode
import com.superfastscan.domain.model.PurchaseUiState
import com.superfastscan.domain.model.SubscriptionPlan
import com.superfastscan.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.superfastscan.data.review.InAppReviewManager
import com.superfastscan.domain.manager.PremiumManager

data class SettingsUiState(
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
    val selectedThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val isPremium: Boolean = false,
    val defaultFormat: String = "PDF",
    val completedScanCount: Int = 0,
    val isReviewPrompted: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val premiumManager: PremiumManager,
    private val inAppReviewManager: InAppReviewManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsRepository.getLanguage(),
            settingsRepository.getThemeMode(),
            premiumManager.isPremium,
            settingsRepository.getDefaultFormat()
        ) { language, themeMode, isPremium, defaultFormat ->
            SettingsUiState(
                selectedLanguage = language,
                selectedThemeMode = themeMode,
                isPremium = isPremium,
                defaultFormat = defaultFormat
            )
        },
        settingsRepository.getCompletedScanCount(),
        settingsRepository.isReviewPrompted()
    ) { baseState, scanCount, reviewPrompted ->
        baseState.copy(
            completedScanCount = scanCount,
            isReviewPrompted = reviewPrompted
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(
                selectedLanguage = settingsRepository.getSystemDefaultLanguage()
            )
        )

    val subscriptionPlan: StateFlow<SubscriptionPlan> = premiumManager.subscriptionPlan
    val lifetimePlan: StateFlow<SubscriptionPlan> = premiumManager.lifetimePlan
    val purchaseState: StateFlow<PurchaseUiState> = premiumManager.purchaseState

    fun launchPurchase(activity: Activity, isLifetime: Boolean = false) {
        premiumManager.launchPurchaseFlow(activity, isLifetime)
    }

    fun restorePurchases() {
        premiumManager.restorePurchases()
    }

    fun resetPurchaseState() {
        premiumManager.resetPurchaseState()
    }

    fun setDefaultFormat(format: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultFormat(format)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(themeMode)
        }
    }
    
    fun togglePremium() {
        premiumManager.togglePremium()
    }

    fun triggerReviewFlow(activity: Activity) {
        viewModelScope.launch {
            inAppReviewManager.launchReviewFlow(activity)
        }
    }

    fun resetReviewState() {
        viewModelScope.launch {
            settingsRepository.resetReviewState()
        }
    }
}
