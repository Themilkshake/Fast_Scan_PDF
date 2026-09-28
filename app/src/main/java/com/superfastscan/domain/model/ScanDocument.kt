package com.superfastscan.domain.model

data class ScanDocument(
    val id: Long = 0,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDraft: Boolean = true,
    val pageCount: Int = 0,
    val thumbnailPath: String? = null,
    val exportedFilePath: String? = null
)
