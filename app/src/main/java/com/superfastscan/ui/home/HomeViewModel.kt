package com.superfastscan.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.domain.model.ScanDocument
import com.superfastscan.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

import android.app.Activity
import com.superfastscan.ads.AdManager
import com.superfastscan.data.review.InAppReviewManager
import com.superfastscan.domain.model.PurchaseUiState
import com.superfastscan.domain.model.SubscriptionPlan
import com.superfastscan.domain.manager.PremiumManager
import com.superfastscan.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

data class HomeUiState(
    val recentScans: List<ScanDocument> = emptyList(),
    val totalScanCount: Int = 0,
    val isPremium: Boolean = false,
    val isInitialSetupCompleted: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val premiumManager: PremiumManager,
    private val settingsRepository: SettingsRepository,
    private val inAppReviewManager: InAppReviewManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _showReviewDialog = MutableStateFlow(false)
    val showReviewDialog: StateFlow<Boolean> = _showReviewDialog.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        documentRepository.getAllDocuments(),
        premiumManager.isPremium,
        settingsRepository.isInitialSetupCompleted()
    ) { documents, isPremium, isSetupCompleted ->
        HomeUiState(
            recentScans = documents.take(3),
            totalScanCount = documents.size,
            isPremium = isPremium,
            isInitialSetupCompleted = isSetupCompleted
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
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

    fun completeInitialSetup(format: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultFormat(format)
            settingsRepository.setInitialSetupCompleted(true)
        }
    }

    fun onScanCompleted(activity: Activity?, adManager: AdManager) {
        viewModelScope.launch {
            val count = settingsRepository.incrementCompletedScanCount()
            val isPrompted = settingsRepository.isReviewPrompted().first()
            val isMilestone = (count == 10 && !isPrompted)

            if (activity != null) {
                adManager.trackScanAndShowAdIfNeeded(activity, isReviewMilestone = isMilestone)
            }

            if (isMilestone) {
                _showReviewDialog.value = true
            }
        }
    }

    fun dismissReviewDialog() {
        _showReviewDialog.value = false
        viewModelScope.launch {
            settingsRepository.setReviewPrompted(true)
        }
    }

    fun launchReview(activity: Activity) {
        _showReviewDialog.value = false
        viewModelScope.launch {
            settingsRepository.setReviewPrompted(true)
            inAppReviewManager.launchReviewFlow(activity)
        }
    }

    fun saveScanResult(
        pdfUri: String?,
        imageUris: List<String>,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefix = if (Locale.getDefault().language.lowercase().startsWith("tr")) "Tarama_" else "Scan_"
            val title = "${prefix}${SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())}"
            val docId = documentRepository.createDraftDocument(title)

            val scanDir = File(context.filesDir, "scans/$docId")
            if (!scanDir.exists()) {
                scanDir.mkdirs()
            }

            imageUris.forEachIndexed { index, uriStr ->
                val destFile = File(scanDir, "page_$index.jpg")
                try {
                    if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                        val uri = Uri.parse(uriStr)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } else {
                        val srcFile = File(uriStr)
                        if (srcFile.exists()) {
                            srcFile.copyTo(destFile, overwrite = true)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val savedPath = if (destFile.exists()) destFile.absolutePath else uriStr
                val scanPage = com.superfastscan.domain.model.ScanPage(
                    documentId = docId,
                    pageIndex = index,
                    originalImagePath = savedPath,
                    editedImagePath = savedPath
                )
                documentRepository.insertPage(scanPage)
            }

            documentRepository.updateDocument(
                ScanDocument(
                    id = docId,
                    title = title,
                    isDraft = false,
                    exportedFilePath = pdfUri
                )
            )

            documentRepository.markDocumentReady(docId)

            withContext(Dispatchers.Main) {
                onComplete(docId)
            }
        }
    }
}
