package com.superfastscan.ui.splitpdf

import android.graphics.Bitmap
import android.net.Uri

/**
 * Split PDF Operation Mode / Tab.
 */
enum class SplitTab {
    /** Split every single page of the document into separate individual PDFs */
    SPLIT_ALL,
    /** Select specific pages (via interactive grid or range) to extract */
    SELECT_PAGES
}

/**
 * Represents a single page in the visual PDF grid.
 */
data class PdfPageItem(
    val pageIndex: Int,       // 0-based index
    val pageNumber: Int,      // 1-based display number
    val thumbnail: Bitmap? = null
)

/**
 * UI State for the Split / Extract PDF tool.
 */
data class SplitPdfUiState(
    val pdfUri: Uri? = null,
    val fileName: String = "",
    val totalPages: Int = 0,
    val pages: List<PdfPageItem> = emptyList(),
    val selectedTab: SplitTab = SplitTab.SPLIT_ALL,
    val selectedPages: Set<Int> = emptySet(), // Set of selected 0-based page indices
    val rangeInput: String = "",
    val rangeError: String? = null,
    val isLoadingPages: Boolean = false,
    val showExtractOptions: Boolean = false,
    val isExtracting: Boolean = false,
    val extractProgressCurrent: Int = 0,
    val extractProgressTotal: Int = 0,
    val extractComplete: Boolean = false,
    val extractedFilePaths: List<String> = emptyList(),
    val error: String? = null
) {
    val isAllSelected: Boolean
        get() = pages.isNotEmpty() && selectedPages.size == pages.size

    val selectedCount: Int
        get() = selectedPages.size
}
