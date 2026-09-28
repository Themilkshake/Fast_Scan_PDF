package com.superfastscan.ui.organizepdf

import android.net.Uri
import com.superfastscan.domain.model.OrganizePageItem
import com.superfastscan.domain.usecase.OrganizePdfResult

/**
 * UI State for Organize PDF Screen.
 */
data class OrganizePdfUiState(
    val initialPdfUri: Uri? = null,
    val fileName: String = "",
    val pages: List<OrganizePageItem> = emptyList(),
    val isLoadingPages: Boolean = false,
    val isSaving: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val organizeResult: OrganizePdfResult? = null,
    val error: String? = null
)
