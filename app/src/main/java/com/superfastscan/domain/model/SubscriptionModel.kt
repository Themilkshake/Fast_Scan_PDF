package com.superfastscan.domain.model

/**
 * Represents the monthly auto-renewable subscription plan.
 */
data class SubscriptionPlan(
    val productId: String = "monthly_premium_no_ads",
    val title: String = "Super Fast PDF Scanner Premium",
    val description: String = "100% Ad-Free Experience",
    val formattedPrice: String = "₺29,99",
    val billingPeriod: String = "P1M",
    val offerToken: String = ""
)

/**
 * UI State for the purchase / paywall flow.
 */
sealed interface PurchaseUiState {
    data object Idle : PurchaseUiState
    data object Loading : PurchaseUiState
    data class Success(val message: String = "") : PurchaseUiState
    data class Error(val message: String) : PurchaseUiState
    data class Restored(val hasActiveSubscription: Boolean, val message: String) : PurchaseUiState
}
