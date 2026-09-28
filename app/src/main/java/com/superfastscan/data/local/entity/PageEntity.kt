package com.superfastscan.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long = 0,
    val pageIndex: Int = 0,
    val originalImagePath: String = "",
    val editedImagePath: String? = null,
    val rotation: Float = 0f,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val filterType: String = "ORIGINAL"
)
