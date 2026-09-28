package com.superfastscan.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.superfastscan.data.local.datastore.SettingsDataStore
import com.superfastscan.domain.model.PurchaseUiState
import com.superfastscan.domain.model.SubscriptionPlan
import java.util.Locale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BillingManager"
const val SUBSCRIPTION_PRODUCT_ID = "monthly_premium_no_ads"
const val LIFETIME_PRODUCT_ID = "lifetimepremium"

private fun getFallbackFormattedPrice(isLifetime: Boolean = false): String {
    return if (Locale.getDefault().language.lowercase().startsWith("tr")) {
        if (isLifetime) "₺99,99" else "₺34,99"
    } else {
        if (isLifetime) "$4.99" else "$0.99"
    }
}

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : PurchasesUpdatedListener, BillingClientStateListener {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private var cachedProductDetails: ProductDetails? = null

    private val _isPremiumActive = MutableStateFlow(false)
    val isPremiumActive: StateFlow<Boolean> = _isPremiumActive.asStateFlow()

    private val _subscriptionPlan = MutableStateFlow(
        SubscriptionPlan(
            productId = SUBSCRIPTION_PRODUCT_ID,
            title = "Super Fast PDF Scanner Premium",
            description = "100% Ad-Free Experience",
            formattedPrice = getFallbackFormattedPrice(false),
            billingPeriod = "P1M"
        )
    )
    val subscriptionPlan: StateFlow<SubscriptionPlan> = _subscriptionPlan.asStateFlow()

    private val _lifetimePlan = MutableStateFlow(
        SubscriptionPlan(
            productId = LIFETIME_PRODUCT_ID,
            title = "Super Fast PDF Scanner Lifetime",
            description = "Lifetime Ad-Free Experience",
            formattedPrice = getFallbackFormattedPrice(true),
            billingPeriod = "LIFETIME"
        )
    )
    val lifetimePlan: StateFlow<SubscriptionPlan> = _lifetimePlan.asStateFlow()

    private var cachedLifetimeDetails: ProductDetails? = null

    private val _purchaseState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    val purchaseState: StateFlow<PurchaseUiState> = _purchaseState.asStateFlow()

    private var isConnected = false

    init {
        // Observe offline cached state first for immediate startup
        coroutineScope.launch {
            settingsDataStore.isPremiumUser.collect { isCachedPremium ->
                _isPremiumActive.value = isCachedPremium
                com.superfastscan.widget.QuickScanWidgetProvider.updateAllWidgets(context)
            }
        }
        startConnection()
    }

    fun startConnection() {
        if (!isConnected) {
            Log.d(TAG, "Connecting to Google Play Billing...")
            billingClient.startConnection(this)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Billing setup successful.")
            isConnected = true
            querySubscriptionDetails()
            refreshPurchases()
        } else {
            Log.w(TAG, "Billing setup failed with response code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            isConnected = false
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "Billing service disconnected. Will retry on demand.")
        isConnected = false
    }

    /**
     * Queries Google Play for the subscription product details and extracts localized price.
     */
    fun querySubscriptionDetails() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            val productDetailsList = queryProductDetailsResult.productDetailsList
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                val details = productDetailsList.first()
                cachedProductDetails = details

                val offerDetails = details.subscriptionOfferDetails?.firstOrNull()
                val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
                val formattedPrice = pricingPhase?.formattedPrice ?: getFallbackFormattedPrice(false)
                val offerToken = offerDetails?.offerToken ?: ""

                Log.d(TAG, "Subscription details retrieved: ${details.title} price: $formattedPrice")

                _subscriptionPlan.value = SubscriptionPlan(
                    productId = details.productId,
                    title = details.name,
                    description = details.description,
                    formattedPrice = formattedPrice,
                    billingPeriod = pricingPhase?.billingPeriod ?: "P1M",
                    offerToken = offerToken
                )
            } else {
                Log.w(TAG, "Product details query returned empty or error: ${billingResult.debugMessage}")
            }
        }

        val inappList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(LIFETIME_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val inappParams = QueryProductDetailsParams.newBuilder().setProductList(inappList).build()
        billingClient.queryProductDetailsAsync(inappParams) { billingResult, queryProductDetailsResult ->
            val productDetailsList = queryProductDetailsResult.productDetailsList
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                val details = productDetailsList.first()
                cachedLifetimeDetails = details
                val formattedPrice = details.oneTimePurchaseOfferDetails?.formattedPrice ?: getFallbackFormattedPrice(true)
                
                Log.d(TAG, "Lifetime details retrieved: ${details.title} price: $formattedPrice")
                
                _lifetimePlan.value = SubscriptionPlan(
                    productId = details.productId,
                    title = details.name,
                    description = details.description,
                    formattedPrice = formattedPrice,
                    billingPeriod = "LIFETIME",
                    offerToken = ""
                )
            }
        }
    }

    /**
     * Launches the Google Play billing purchase flow.
     */
    fun launchBillingFlow(activity: Activity, isLifetime: Boolean = false) {
        if (!billingClient.isReady) {
            _purchaseState.value = PurchaseUiState.Error("Google Play store connection unavailable. Retrying...")
            startConnection()
            return
        }

        val productDetails = if (isLifetime) cachedLifetimeDetails else cachedProductDetails
        if (productDetails == null) {
            // If product details not yet fetched from Google Play Console, retry query
            querySubscriptionDetails()
            _purchaseState.value = PurchaseUiState.Error("Product details loading, please try again in a moment.")
            return
        }

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        if (!isLifetime) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }

        val productDetailsParamsList = listOf(productDetailsParamsBuilder.build())

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _purchaseState.value = PurchaseUiState.Loading
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${result.debugMessage}")
            _purchaseState.value = PurchaseUiState.Error(result.debugMessage)
        }
    }

    /**
     * Callback for purchase updates from Google Play.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    handlePurchases(purchases)
                } else {
                    _purchaseState.value = PurchaseUiState.Idle
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase flow.")
                _purchaseState.value = PurchaseUiState.Idle
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned. Refreshing entitlement...")
                coroutineScope.launch {
                    settingsDataStore.setPremiumUser(true)
                    _isPremiumActive.value = true
                    _purchaseState.value = PurchaseUiState.Success("Subscription active!")
                }
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                _purchaseState.value = PurchaseUiState.Error(billingResult.debugMessage)
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.products.contains(SUBSCRIPTION_PRODUCT_ID) || purchase.products.contains(LIFETIME_PRODUCT_ID)) {
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> {
                        if (!purchase.isAcknowledged) {
                            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                            billingClient.acknowledgePurchase(acknowledgeParams) { ackResult ->
                                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                    Log.d(TAG, "Purchase acknowledged successfully.")
                                } else {
                                    Log.e(TAG, "Failed to acknowledge purchase: ${ackResult.debugMessage}")
                                }
                            }
                        }
                        coroutineScope.launch {
                            settingsDataStore.setPremiumUser(true)
                            _isPremiumActive.value = true
                            _purchaseState.value = PurchaseUiState.Success()
                        }
                    }
                    Purchase.PurchaseState.PENDING -> {
                        Log.d(TAG, "Purchase is pending confirmation.")
                        _purchaseState.value = PurchaseUiState.Error("Purchase pending approval.")
                    }
                    else -> {
                        Log.w(TAG, "Unhandled purchase state: ${purchase.purchaseState}")
                    }
                }
            }
        }
    }

    /**
     * Checks existing purchases and updates subscription status.
     * Called on startup and on resume.
     */
    fun refreshPurchases() {
        if (!billingClient.isReady) return

        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
            if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                billingClient.queryPurchasesAsync(inappParams) { inappResult, inappPurchases ->
                    if (inappResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val allPurchases = subsPurchases + inappPurchases
                        val hasActiveSubscription = allPurchases.any { purchase ->
                            (purchase.products.contains(SUBSCRIPTION_PRODUCT_ID) || purchase.products.contains(LIFETIME_PRODUCT_ID)) &&
                                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        }
                        coroutineScope.launch {
                            settingsDataStore.setPremiumUser(hasActiveSubscription)
                            _isPremiumActive.value = hasActiveSubscription
                            Log.d(TAG, "Subscription status refreshed. Active: $hasActiveSubscription")
                        }
                    }
                }
            }
        }
    }

    /**
     * Restores purchases on user request.
     */
    fun restorePurchases(onResult: (Boolean) -> Unit) {
        _purchaseState.value = PurchaseUiState.Loading

        if (!billingClient.isReady) {
            startConnection()
            _purchaseState.value = PurchaseUiState.Error("Store service connecting. Please try again.")
            onResult(false)
            return
        }

        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
            if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                billingClient.queryPurchasesAsync(inappParams) { inappResult, inappPurchases ->
                    if (inappResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val allPurchases = subsPurchases + inappPurchases
                        val hasActiveSubscription = allPurchases.any { purchase ->
                            (purchase.products.contains(SUBSCRIPTION_PRODUCT_ID) || purchase.products.contains(LIFETIME_PRODUCT_ID)) &&
                                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        }
                        coroutineScope.launch {
                            settingsDataStore.setPremiumUser(hasActiveSubscription)
                            _isPremiumActive.value = hasActiveSubscription
                            _purchaseState.value = PurchaseUiState.Restored(
                                hasActiveSubscription = hasActiveSubscription,
                                message = if (hasActiveSubscription) "Premium restored!" else "No active premium found."
                            )
                            onResult(hasActiveSubscription)
                        }
                    } else {
                        _purchaseState.value = PurchaseUiState.Error("Failed to check INAPP purchases.")
                        onResult(false)
                    }
                }
            } else {
                _purchaseState.value = PurchaseUiState.Error("Failed to check SUBS purchases: ${subsResult.debugMessage}")
                onResult(false)
            }
        }
    }

    /**
     * Reset UI purchase state back to Idle after handling.
     */
    fun resetPurchaseState() {
        _purchaseState.value = PurchaseUiState.Idle
    }

    /**
     * Test helper for development & testing verification without Google Play account.
     */
    fun setTestPremium(isPremium: Boolean) {
        coroutineScope.launch {
            settingsDataStore.setPremiumUser(isPremium)
            _isPremiumActive.value = isPremium
            Log.d(TAG, "Developer test mode toggled. isPremium=$isPremium")
        }
    }
}
