package com.superfastscan.ui.jpgtopdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.domain.usecase.JpgToPdfUseCase
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
class JpgToPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jpgToPdfUseCase: JpgToPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JpgToPdfUiState())
    val uiState: StateFlow<JpgToPdfUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val validUris = uris.filter { uri ->
            val mimeType = context.contentResolver.getType(uri)
            val name = try {
                var fileName: String? = null
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) fileName = cursor.getString(index)
                    }
                }
                fileName
            } catch (e: Exception) {
                null
            } ?: uri.lastPathSegment ?: ""

            mimeType?.startsWith("image/") == true ||
                listOf(".jpg", ".jpeg", ".png", ".webp", ".bmp", ".heic").any { name.endsWith(it, ignoreCase = true) }
        }

        if (validUris.isEmpty()) {
            _uiState.update { it.copy(error = context.getString(com.superfastscan.R.string.invalid_image_file)) }
            return
        }

        _uiState.update { current ->
            current.copy(
                selectedImages = current.selectedImages + validUris,
                conversionResult = null,
                pdfThumbnail = null,
                error = null
            )
        }
    }

    fun removeImage(index: Int) {
        _uiState.update { current ->
            val list = current.selectedImages.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
                current.copy(selectedImages = list)
            } else {
                current
            }
        }
    }

    fun moveImage(from: Int, to: Int) {
        _uiState.update { current ->
            val list = current.selectedImages.toMutableList()
            if (from in list.indices && to in list.indices) {
                val item = list.removeAt(from)
                list.add(to, item)
                current.copy(selectedImages = list)
            } else {
                current
            }
        }
    }

    fun updateCustomFileName(name: String) {
        _uiState.update { it.copy(customFileName = name) }
    }

    fun convertToPdf() {
        val images = _uiState.value.selectedImages
        if (images.isEmpty()) return

        val customName = _uiState.value.customFileName.ifBlank { null }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    progressCurrent = 0,
                    progressTotal = images.size,
                    error = null
                )
            }

            try {
                val result = jpgToPdfUseCase(
                    imageUris = images,
                    customFileName = customName,
                    onProgress = { current, total ->
                        _uiState.update {
                            it.copy(progressCurrent = current, progressTotal = total)
                        }
                    }
                )

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
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        error = e.localizedMessage ?: "Failed to create PDF from images"
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.update { JpgToPdfUiState() }
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
