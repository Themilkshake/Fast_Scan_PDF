package com.superfastscan.ui.pdftojpg

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.R
import com.superfastscan.domain.usecase.PdfToJpgUseCase
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
import javax.inject.Inject

@HiltViewModel
class PdfToJpgViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfToJpgUseCase: PdfToJpgUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfToJpgUiState())
    val uiState: StateFlow<PdfToJpgUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pdfUri = uri,
                    result = null,
                    error = null
                )
            }

            try {
                val name = pdfToJpgUseCase.getFileName(uri) ?: "Document.pdf"
                val isPdf = name.endsWith(".pdf", ignoreCase = true) ||
                        context.contentResolver.getType(uri) == "application/pdf"
                if (!isPdf) {
                    _uiState.update {
                        it.copy(
                            error = context.getString(R.string.invalid_pdf_file)
                        )
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
                        fileName = name,
                        pageCount = count,
                        firstPageThumbnail = thumb
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to open PDF: ${e.message}")
                }
            }
        }
    }

    fun extractJpgs() {
        val uri = _uiState.value.pdfUri ?: return
        val total = _uiState.value.pageCount
        if (total == 0) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtracting = true,
                    progressCurrent = 0,
                    progressTotal = total,
                    error = null
                )
            }

            try {
                val result = pdfToJpgUseCase(
                    pdfUri = uri,
                    onProgress = { current, max ->
                        _uiState.update {
                            it.copy(progressCurrent = current, progressTotal = max)
                        }
                    }
                )

                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        result = result
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        error = e.localizedMessage ?: "Failed to extract images"
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.update { PdfToJpgUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
