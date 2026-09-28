package com.superfastscan.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.domain.model.FilterType
import com.superfastscan.domain.model.ScanDocument
import com.superfastscan.domain.model.ScanPage
import com.superfastscan.domain.repository.DocumentRepository
import com.superfastscan.domain.repository.SettingsRepository
import com.superfastscan.domain.usecase.ExportImageUseCase
import com.superfastscan.domain.usecase.ExportPdfUseCase
import com.superfastscan.domain.usecase.ProcessDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class EditDocumentUiState(
    val pages: List<ScanPage> = emptyList(),
    val currentPageIndex: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isProcessing: Boolean = false,
    val hasChanges: Boolean = false,
    val documentTitle: String = "",
    val selectedFormat: String = "PDF",
    val saveProgress: String = "",
    val error: String? = null
)

@HiltViewModel
class EditDocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val processDocumentUseCase: ProcessDocumentUseCase,
    private val exportPdfUseCase: ExportPdfUseCase,
    private val exportImageUseCase: ExportImageUseCase,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditDocumentUiState())
    val uiState: StateFlow<EditDocumentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getDefaultFormat().collect { format ->
                _uiState.update { it.copy(selectedFormat = format) }
            }
        }
    }

    fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val pages = documentRepository.getPagesList(documentId)
            val defaultFormat = runCatching { settingsRepository.getDefaultFormat().first() }.getOrDefault("PDF")
            val doc = runCatching { documentRepository.getDocumentById(documentId).first() }.getOrNull()
            val defaultPrefix = if (Locale.getDefault().language.lowercase().startsWith("tr")) "Tarama_" else "Scan_"
            val title = doc?.title?.ifBlank { null }
                ?: "${defaultPrefix}${SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())}"

            _uiState.update {
                it.copy(
                    pages = pages,
                    documentTitle = title,
                    selectedFormat = defaultFormat,
                    isLoading = false
                )
            }
        }
    }

    fun updateDocumentTitle(title: String) {
        _uiState.update { it.copy(documentTitle = title, hasChanges = true) }
    }

    fun selectFormat(format: String) {
        _uiState.update { it.copy(selectedFormat = format, hasChanges = true) }
    }

    fun setCurrentPage(index: Int) {
        _uiState.update { it.copy(currentPageIndex = index.coerceIn(0, (it.pages.size - 1).coerceAtLeast(0))) }
    }

    fun rotatePage(documentId: Long, degrees: Float) {
        val state = _uiState.value
        if (state.pages.isEmpty()) return
        val page = state.pages[state.currentPageIndex]
        val newRotation = (page.rotation + degrees) % 360f

        val updatedPage = page.copy(rotation = newRotation)
        val updatedPages = state.pages.toMutableList().apply {
            set(state.currentPageIndex, updatedPage)
        }
        _uiState.update { it.copy(pages = updatedPages, hasChanges = true) }

        // Save rotation to database
        viewModelScope.launch {
            documentRepository.updatePage(updatedPage)
        }
    }

    fun applyFilter(documentId: Long, filterType: FilterType) {
        val state = _uiState.value
        if (state.pages.isEmpty()) return
        val page = state.pages[state.currentPageIndex]
        if (page.filterType == filterType) return

        val updatedPage = page.copy(filterType = filterType)
        val updatedPages = state.pages.toMutableList().apply {
            set(state.currentPageIndex, updatedPage)
        }
        _uiState.update { it.copy(pages = updatedPages, hasChanges = true) }

        // Save filter to database
        viewModelScope.launch {
            documentRepository.updatePage(updatedPage)
        }
    }

    fun deletePage(documentId: Long) {
        val state = _uiState.value
        if (state.pages.isEmpty()) return
        val page = state.pages[state.currentPageIndex]

        viewModelScope.launch {
            documentRepository.deletePage(page.id)
            val remainingPages = documentRepository.getPagesList(documentId)
            val newIndex = state.currentPageIndex.coerceIn(0, (remainingPages.size - 1).coerceAtLeast(0))
            _uiState.update {
                it.copy(
                    pages = remainingPages,
                    currentPageIndex = newIndex,
                    hasChanges = true
                )
            }
        }
    }

    fun saveAndExport(documentId: Long, onComplete: (String?) -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return

        val defaultPrefix = if (Locale.getDefault().language.lowercase().startsWith("tr")) "Tarama_" else "Scan_"
        val rawTitle = state.documentTitle.ifBlank { "${defaultPrefix}${System.currentTimeMillis()}" }
        val sanitizedTitle = rawTitle.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { "${defaultPrefix}${System.currentTimeMillis()}" }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveProgress = "Sayfalar hazırlanıyor...", error = null) }
            try {
                // 1. Process all pages with rotation and filters
                processDocumentUseCase.processAllPages(documentId)

                // 2. Export to storage
                val outputPath: String? = if (state.selectedFormat.equals("JPG", ignoreCase = true)) {
                    _uiState.update { it.copy(saveProgress = "Görseller kaydediliyor...") }
                    val paths = exportImageUseCase(documentId, sanitizedTitle)
                    paths.firstOrNull()
                } else {
                    _uiState.update { it.copy(saveProgress = "PDF oluşturuluyor...") }
                    exportPdfUseCase(documentId, sanitizedTitle)
                }

                documentRepository.markDocumentReady(documentId)

                _uiState.update { it.copy(isSaving = false, saveProgress = "") }
                withContext(Dispatchers.Main) {
                    onComplete(outputPath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Kayıt başarısız", saveProgress = "")
                }
            }
        }
    }
}
