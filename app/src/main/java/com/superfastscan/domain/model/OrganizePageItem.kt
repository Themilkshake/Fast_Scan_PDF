package com.superfastscan.domain.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

/**
 * Represents a single page in the Organize PDF workflow.
 * Supports pages from original source PDFs as well as dynamically inserted blank pages or appended PDFs.
 */
data class OrganizePageItem(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri? = null,
    val sourcePageIndex: Int = 0,
    val isBlankPage: Boolean = false,
    val thumbnail: Bitmap? = null,
    val sourceFileName: String = ""
)
