package com.superfastscan.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.domain.manager.PremiumManager
import com.superfastscan.domain.model.ScanDocument
import com.superfastscan.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedScansUiState(
    val documents: List<ScanDocument> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val isPremium: Boolean = false
)

@HiltViewModel
class SavedScansViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    val uiState: StateFlow<SavedScansUiState> = combine(
        documentRepository.getAllDocuments(),
        premiumManager.isPremium
    ) { documents, isPremium ->
        SavedScansUiState(
            documents = documents,
            isLoading = false,
            isEmpty = documents.isEmpty(),
            isPremium = isPremium
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SavedScansUiState(isLoading = true, isEmpty = false, isPremium = false)
        )

    fun deleteDocument(documentId: Long, deleteExportedFiles: Boolean = false) {
        viewModelScope.launch {
            documentRepository.deleteDocument(documentId, deleteExportedFiles)
        }
    }
}
