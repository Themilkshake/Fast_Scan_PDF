package com.superfastscan.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.domain.repository.DocumentRepository
import com.superfastscan.domain.usecase.ExportImageUseCase
import com.superfastscan.domain.usecase.ExportPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class ExportFormat { PDF, JPG }

data class ExportUiState(
    val fileName: String = "",
    val selectedFormat: ExportFormat = ExportFormat.PDF,
    val isExporting: Boolean = false,
    val exportProgress: String = "",
    val exportComplete: Boolean = false,
    val exportedPath: String? = null,
    val error: String? = null,
    val pageCount: Int = 0,
    val thumbnailPath: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportPdfUseCase: ExportPdfUseCase,
    private val exportImageUseCase: ExportImageUseCase,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            val pages = documentRepository.getPagesList(documentId)
            val thumbnail = pages.firstOrNull()?.let { it.editedImagePath ?: it.originalImagePath }
            val defaultPrefix = if (Locale.getDefault().language.lowercase().startsWith("tr")) "Tarama_" else "Scan_"
            val defaultName = "${defaultPrefix}${SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())}"
            _uiState.update { current ->
                current.copy(
                    fileName = current.fileName.ifBlank { defaultName },
                    pageCount = pages.size,
                    thumbnailPath = thumbnail
                )
            }
        }
    }

    fun updateFileName(name: String) {
        _uiState.update { it.copy(fileName = name) }
    }

    fun selectFormat(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun export(documentId: Long) {
        val state = _uiState.value
        if (state.isExporting) return

        val defaultPrefix = if (Locale.getDefault().language.lowercase().startsWith("tr")) "Tarama_" else "Scan_"
        val name = state.fileName.ifBlank { "${defaultPrefix}${System.currentTimeMillis()}" }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isExporting = true, exportProgress = "Sayfalar hazırlanıyor...", error = null)
            }

            try {
                val path = when (state.selectedFormat) {
                    ExportFormat.PDF -> {
                        _uiState.update { it.copy(exportProgress = "PDF oluşturuluyor...") }
                        exportPdfUseCase(documentId, name)
                    }
                    ExportFormat.JPG -> {
                        _uiState.update { it.copy(exportProgress = "Görseller kaydediliyor...") }
                        val paths = exportImageUseCase(documentId, name)
                        paths.firstOrNull() ?: ""
                    }
                }

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportComplete = true,
                        exportedPath = path,
                        exportProgress = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        error = e.message ?: "Dışa aktarma başarısız",
                        exportProgress = ""
                    )
                }
            }
        }
    }
}
