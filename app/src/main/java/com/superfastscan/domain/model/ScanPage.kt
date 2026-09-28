package com.superfastscan.domain.model

data class ScanPage(
    val id: Long = 0,
    val documentId: Long = 0,
    val pageIndex: Int = 0,
    val originalImagePath: String = "",
    val editedImagePath: String? = null,
    val rotation: Float = 0f,
    val cropRect: CropRect? = null,
    val filterType: FilterType = FilterType.ORIGINAL
)

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)
