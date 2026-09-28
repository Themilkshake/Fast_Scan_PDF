package com.superfastscan.domain.manager

import android.app.Activity
import com.superfastscan.data.billing.BillingManager
import com.superfastscan.domain.model.PurchaseUiState
import com.superfastscan.domain.model.SubscriptionPlan
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the user's subscription tier (Free vs Premium).
 * Synchronized with Google Play Billing and secure local DataStore caching.
 */
@Singleton
class PremiumManager @Inject constructor(
    private val billingManager: BillingManager
) {
    /**
     * Reactive state indicating whether the user has an active ad-free subscription.
     */
    val isPremium: StateFlow<Boolean> = billingManager.isPremiumActive

    val subscriptionPlan: StateFlow<SubscriptionPlan> = billingManager.subscriptionPlan

    val lifetimePlan: StateFlow<SubscriptionPlan> = billingManager.lifetimePlan

    val purchaseState: StateFlow<PurchaseUiState> = billingManager.purchaseState

    /**
     * Launches Google Play subscription purchase sheet.
     */
    fun launchPurchaseFlow(activity: Activity, isLifetime: Boolean = false) {
        billingManager.launchBillingFlow(activity, isLifetime)
    }

    /**
     * Restores purchases from Google Play.
     */
    fun restorePurchases(onResult: (Boolean) -> Unit = {}) {
        billingManager.restorePurchases(onResult)
    }

    /**
     * Refreshes active entitlement from Google Play.
     */
    fun refreshPurchases() {
        billingManager.refreshPurchases()
    }

    fun resetPurchaseState() {
        billingManager.resetPurchaseState()
    }

    /**
     * Toggles the premium state for developer testing purposes.
     */
    fun togglePremium() {
        billingManager.setTestPremium(!isPremium.value)
    }

    /**
     * Directly set premium state for testing.
     */
    fun setPremium(isPremium: Boolean) {
        billingManager.setTestPremium(isPremium)
    }
}
