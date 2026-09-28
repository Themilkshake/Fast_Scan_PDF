package com.superfastscan.ui.wordtopdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.R
import com.superfastscan.domain.usecase.WordToPdfUseCase
import com.superfastscan.domain.util.NetworkUtils
import com.superfastscan.domain.util.PdfPageRenderer
import com.superfastscan.ui.officetopdf.OfficeDocType
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
import java.util.Locale
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow

@HiltViewModel
class WordToPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wordToPdfUseCase: WordToPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordToPdfUiState())
    val uiState: StateFlow<WordToPdfUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    fun loadDocument(uri: Uri, docType: OfficeDocType? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    documentUri = uri,
                    isLoadingInfo = true,
                    conversionResult = null,
                    pdfThumbnail = null,
                    error = null
                )
            }

            try {
                val defaultFallback = when (docType) {
                    OfficeDocType.PPT -> "Presentation.pptx"
                    OfficeDocType.EXCEL -> "Spreadsheet.xlsx"
                    else -> "Document.docx"
                }
                val fileName = wordToPdfUseCase.getFileName(uri) ?: defaultFallback

                // Validate file extension against expected OfficeDocType if provided
                if (docType != null) {
                    val extension = fileName.substringAfterLast(".", "").lowercase()
                    if (extension.isNotEmpty() && extension !in docType.allowedExtensions) {
                        val errorMsg = when (docType) {
                            OfficeDocType.WORD -> context.getString(R.string.word_to_pdf_invalid_format)
                            OfficeDocType.PPT -> context.getString(R.string.ppt_to_pdf_invalid_format)
                            OfficeDocType.EXCEL -> context.getString(R.string.excel_to_pdf_invalid_format)
                        }
                        _uiState.update {
                            it.copy(
                                documentUri = null,
                                isLoadingInfo = false,
                                error = errorMsg
                            )
                        }
                        return@launch
                    }
                }

                val fileSize = wordToPdfUseCase.getFileSize(uri)
                val defaultName = fileName.substringBeforeLast(".")

                _uiState.update {
                    it.copy(
                        fileName = fileName,
                        fileSizeBytes = fileSize,
                        customOutputName = defaultName,
                        isLoadingInfo = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingInfo = false,
                        error = "Failed to load document: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateCustomOutputName(name: String) {
        _uiState.update { it.copy(customOutputName = name) }
    }

    fun convertToPdf() {
        val uri = _uiState.value.documentUri ?: return
        val customName = _uiState.value.customOutputName.ifBlank { null }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            _uiState.update {
                it.copy(
                    isConverting = false,
                    error = context.getString(R.string.error_no_internet)
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    error = null
                )
            }

            try {
                val result = wordToPdfUseCase(
                    documentUri = uri,
                    customFileName = customName
                )

                // Render first page thumbnail from generated PDF
                var thumb: android.graphics.Bitmap? = null
                val pdfUri = result.outputUri ?: Uri.parse(result.outputPath)
                try {
                    withContext(Dispatchers.IO) {
                        pdfPageRenderer.openPdf(pdfUri) { total, renderPage, _ ->
                            if (total > 0) {
                                thumb = renderPage(0, 450)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                _uiState.update {
                    it.copy(
                        isConverting = false,
                        conversionResult = result,
                        pdfThumbnail = thumb
                    )
                }
            } catch (e: Exception) {
                val errorMsg = if (!NetworkUtils.isNetworkAvailable(context)) {
                    context.getString(R.string.error_no_internet)
                } else {
                    context.getString(R.string.error_server_connection)
                }
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        error = errorMsg
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.update {
            WordToPdfUiState()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
