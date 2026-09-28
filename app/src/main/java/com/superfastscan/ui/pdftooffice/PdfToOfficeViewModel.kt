package com.superfastscan.ui.pdftooffice

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.R
import com.superfastscan.domain.usecase.PdfToOfficeUseCase
import com.superfastscan.domain.util.NetworkUtils
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
class PdfToOfficeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfToOfficeUseCase: PdfToOfficeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfToOfficeUiState())
    val uiState: StateFlow<PdfToOfficeUiState> = _uiState.asStateFlow()

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
                val name = getFileName(uri) ?: "Document.pdf"
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
                val defaultName = name.substringBeforeLast(".")
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
                        customFileName = defaultName,
                        pageCount = count,
                        firstPageThumbnail = thumb
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to load PDF: ${e.message}")
                }
            }
        }
    }

    fun updateCustomFileName(name: String) {
        _uiState.update { it.copy(customFileName = name) }
    }

    fun convert(type: PdfToOfficeType) {
        val uri = _uiState.value.pdfUri ?: return
        val customName = _uiState.value.customFileName.ifBlank { null }

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
                it.copy(isConverting = true, error = null)
            }

            try {
                val result = pdfToOfficeUseCase(
                    pdfUri = uri,
                    targetFormat = type.targetFormat,
                    outputBaseName = customName
                )

                _uiState.update {
                    it.copy(
                        isConverting = false,
                        result = result
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
        _uiState.update { PdfToOfficeUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
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
