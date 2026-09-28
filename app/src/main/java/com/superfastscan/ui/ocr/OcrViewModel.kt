package com.superfastscan.ui.ocr

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.R
import com.superfastscan.domain.usecase.CreateSearchablePdfUseCase
import com.superfastscan.domain.usecase.PerformOcrUseCase
import com.superfastscan.domain.util.PdfPageRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow

@HiltViewModel
class OcrViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performOcrUseCase: PerformOcrUseCase,
    private val createSearchablePdfUseCase: CreateSearchablePdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    fun setMode(mode: OcrMode) {
        _uiState.update {
            it.copy(
                activeMode = mode,
                errorMessage = null
            )
        }
    }

    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedPdfUri = uri,
                    searchablePdfResult = null,
                    ocrResult = null,
                    errorMessage = null
                )
            }

            try {
                val name = getFileName(uri) ?: "Document.pdf"
                val isPdf = name.endsWith(".pdf", ignoreCase = true) ||
                        context.contentResolver.getType(uri) == "application/pdf"
                if (!isPdf) {
                    _uiState.update {
                        it.copy(errorMessage = context.getString(R.string.invalid_pdf_file))
                    }
                    return@launch
                }
                var count = 0
                var thumb: android.graphics.Bitmap? = null

                withContext(Dispatchers.IO) {
                    pdfPageRenderer.openPdf(uri) { total, renderPage, _ ->
                        count = total
                        if (total > 0) {
                            thumb = renderPage(0, 450)
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        pdfFileName = name,
                        pdfPageCount = count,
                        pdfFirstPageThumbnail = thumb
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to read PDF: ${e.message}")
                }
            }
        }
    }

    fun convertToSearchablePdf() {
        val uri = _uiState.value.selectedPdfUri ?: return
        val total = _uiState.value.pdfPageCount

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressCurrent = 0,
                    progressTotal = total,
                    errorMessage = null
                )
            }

            val result = createSearchablePdfUseCase(
                pdfUri = uri,
                onProgress = { current, max ->
                    _uiState.update {
                        it.copy(progressCurrent = current, progressTotal = max)
                    }
                }
            )

            result.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        searchablePdfResult = res
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = error.localizedMessage ?: "Failed to generate Searchable PDF"
                    )
                }
            }
        }
    }

    fun processImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedImageUri = uri,
                    isProcessing = true,
                    searchablePdfResult = null,
                    ocrResult = null,
                    savedFilePath = null,
                    errorMessage = null
                )
            }

            val result = performOcrUseCase.recognizeText(uri)
            result.onSuccess { ocrRes ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        ocrResult = ocrRes,
                        errorMessage = if (ocrRes.fullText.isBlank()) "No readable text detected" else null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = error.localizedMessage ?: "Failed to recognize text"
                    )
                }
            }
        }
    }

    fun saveAsTextFile(onSuccess: (String) -> Unit) {
        val text = _uiState.value.ocrResult?.fullText ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                val (path, _) = performOcrUseCase.saveTextToFile(text)
                _uiState.update { it.copy(savedFilePath = path) }
                onSuccess(path)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save text file: ${e.message}") }
            }
        }
    }

    fun reset() {
        val currentMode = _uiState.value.activeMode
        _uiState.update { OcrUiState(activeMode = currentMode) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return name
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
            val safeIndex = digitGroups.coerceIn(0, units.lastIndex)
            return DecimalFormat("#,##0.#").format(bytes / 1024.0.pow(safeIndex.toDouble())) + " " + units[safeIndex]
        }
    }
}
