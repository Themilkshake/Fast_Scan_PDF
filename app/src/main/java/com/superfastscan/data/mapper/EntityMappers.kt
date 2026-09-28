package com.superfastscan.data.mapper

import com.superfastscan.data.local.entity.DocumentEntity
import com.superfastscan.data.local.entity.PageEntity
import com.superfastscan.domain.model.CropRect
import com.superfastscan.domain.model.FilterType
import com.superfastscan.domain.model.ScanDocument
import com.superfastscan.domain.model.ScanPage

fun DocumentEntity.toDomain(pageCount: Int = 0, thumbnailPath: String? = null): ScanDocument =
    ScanDocument(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDraft = isDraft,
        pageCount = pageCount,
        thumbnailPath = thumbnailPath,
        exportedFilePath = exportedFilePath
    )

fun ScanDocument.toEntity(): DocumentEntity =
    DocumentEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDraft = isDraft,
        exportedFilePath = exportedFilePath
    )

fun PageEntity.toDomain(): ScanPage =
    ScanPage(
        id = id,
        documentId = documentId,
        pageIndex = pageIndex,
        originalImagePath = originalImagePath,
        editedImagePath = editedImagePath,
        rotation = rotation,
        cropRect = CropRect(
            left = cropLeft,
            top = cropTop,
            right = cropRight,
            bottom = cropBottom
        ),
        filterType = try {
            FilterType.valueOf(filterType)
        } catch (e: IllegalArgumentException) {
            FilterType.ORIGINAL
        }
    )

fun ScanPage.toEntity(): PageEntity =
    PageEntity(
        id = id,
        documentId = documentId,
        pageIndex = pageIndex,
        originalImagePath = originalImagePath,
        editedImagePath = editedImagePath,
        rotation = rotation,
        cropLeft = cropRect?.left ?: 0f,
        cropTop = cropRect?.top ?: 0f,
        cropRight = cropRect?.right ?: 1f,
        cropBottom = cropRect?.bottom ?: 1f,
        filterType = filterType.name
    )
