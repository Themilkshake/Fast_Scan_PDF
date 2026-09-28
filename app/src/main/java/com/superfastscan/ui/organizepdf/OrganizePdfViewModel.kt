package com.superfastscan.ui.organizepdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.R
import com.superfastscan.domain.model.OrganizePageItem
import com.superfastscan.domain.usecase.OrganizePdfUseCase
import com.superfastscan.domain.util.PdfPageRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OrganizePdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val organizePdfUseCase: OrganizePdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganizePdfUiState())
    val uiState: StateFlow<OrganizePdfUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    // ────────────────────────────────────────────────────────
    // PDF Loading & Page Extraction
    // ────────────────────────────────────────────────────────

    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    initialPdfUri = uri,
                    isLoadingPages = true,
                    pages = emptyList(),
                    organizeResult = null,
                    error = null
                )
            }

            try {
                val fileName = getFileName(uri) ?: "Document.pdf"
                val isPdf = fileName.endsWith(".pdf", ignoreCase = true) ||
                        context.contentResolver.getType(uri) == "application/pdf"
                if (!isPdf) {
                    _uiState.update {
                        it.copy(
                            isLoadingPages = false,
                            error = context.getString(R.string.invalid_pdf_file)
                        )
                    }
                    return@launch
                }

                val pages = withContext(Dispatchers.IO) {
                    val pageList = mutableListOf<OrganizePageItem>()
                    pdfPageRenderer.openPdf(uri) { total, renderPage, _ ->
                        for (pageIndex in 0 until total) {
                            // 450px width is ideal for crisp grid cards while keeping memory minimal
                            val thumb = renderPage(pageIndex, 450)
                            pageList.add(
                                OrganizePageItem(
                                    sourceUri = uri,
                                    sourcePageIndex = pageIndex,
                                    isBlankPage = false,
                                    thumbnail = thumb,
                                    sourceFileName = fileName
                                )
                            )
                        }
                    }
                    pageList
                }

                _uiState.update {
                    it.copy(
                        fileName = fileName,
                        pages = pages,
                        isLoadingPages = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingPages = false,
                        error = "Failed to load PDF: ${e.message}"
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Reorder, Delete, Add Blank, Append PDF
    // ────────────────────────────────────────────────────────

    fun onReorder(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.pages.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _uiState.update { it.copy(pages = currentList) }
        }
    }

    fun onDeletePage(pageId: String) {
        val updated = _uiState.value.pages.filterNot { it.id == pageId }
        _uiState.update { it.copy(pages = updated) }
    }

    fun onAddBlankPage() {
        // Generate a crisp blank white page bitmap preview
        val blankBitmap = Bitmap.createBitmap(300, 424, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
            val canvas = Canvas(this)
            val paint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }
            canvas.drawRect(4f, 4f, 296f, 420f, paint)
        }

        val blankItem = OrganizePageItem(
            sourceUri = null,
            sourcePageIndex = 0,
            isBlankPage = true,
            thumbnail = blankBitmap,
            sourceFileName = "Blank Page"
        )

        _uiState.update {
            it.copy(pages = it.pages + blankItem)
        }
    }

    fun onAppendPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPages = true) }
            try {
                val fileName = getFileName(uri) ?: "Appended.pdf"
                val isPdf = fileName.endsWith(".pdf", ignoreCase = true) ||
                        context.contentResolver.getType(uri) == "application/pdf"
                if (!isPdf) {
                    _uiState.update {
                        it.copy(
                            isLoadingPages = false,
                            error = context.getString(R.string.invalid_pdf_file)
                        )
                    }
                    return@launch
                }
                val newPages = withContext(Dispatchers.IO) {
                    val pageList = mutableListOf<OrganizePageItem>()
                    pdfPageRenderer.openPdf(uri) { total, renderPage, _ ->
                        for (pageIndex in 0 until total) {
                            val thumb = renderPage(pageIndex, 450)
                            pageList.add(
                                OrganizePageItem(
                                    sourceUri = uri,
                                    sourcePageIndex = pageIndex,
                                    isBlankPage = false,
                                    thumbnail = thumb,
                                    sourceFileName = fileName
                                )
                            )
                        }
                    }
                    pageList
                }

                _uiState.update {
                    it.copy(
                        pages = it.pages + newPages,
                        isLoadingPages = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingPages = false,
                        error = "Failed to append PDF: ${e.message}"
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Save Organized PDF
    // ────────────────────────────────────────────────────────

    fun saveOrganizedPdf() {
        val pages = _uiState.value.pages
        if (pages.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    progressCurrent = 0,
                    progressTotal = pages.size,
                    error = null
                )
            }

            try {
                val baseName = _uiState.value.fileName.removeSuffix(".pdf")
                val result = organizePdfUseCase(
                    pages = pages,
                    baseFileName = baseName,
                    onProgress = { current, total ->
                        _uiState.update {
                            it.copy(progressCurrent = current, progressTotal = total)
                        }
                    }
                )

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        organizeResult = result
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to organize PDF: ${e.message}"
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.update { OrganizePdfUiState() }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
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
}
