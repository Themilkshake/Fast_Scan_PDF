package com.superfastscan.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.superfastscan.R

/**
 * Represents each PDF tool available in the app.
 * Each tool has a unique ID, title/description string resources, icon, and accent color.
 */
enum class PdfTool(
    val toolId: String,
    val titleResId: Int,
    val descResId: Int,
    val icon: ImageVector,
    val iconTint: Color
) {
    MERGE_PDF(
        toolId = "merge_pdf",
        titleResId = R.string.tool_merge_pdf,
        descResId = R.string.tool_merge_pdf_desc,
        icon = Icons.Default.CallMerge,
        iconTint = Color(0xFF2196F3) // Blue
    ),
    SPLIT_PDF(
        toolId = "split_pdf",
        titleResId = R.string.tool_split_pdf,
        descResId = R.string.tool_split_pdf_desc,
        icon = Icons.Default.CallSplit,
        iconTint = Color(0xFFFF9800) // Orange
    ),
    COMPRESS_PDF(
        toolId = "compress_pdf",
        titleResId = R.string.tool_compress_pdf,
        descResId = R.string.tool_compress_pdf_desc,
        icon = Icons.Default.Compress,
        iconTint = Color(0xFF9C27B0) // Purple
    ),
    PDF_TO_WORD(
        toolId = "pdf_to_word",
        titleResId = R.string.tool_pdf_to_word,
        descResId = R.string.tool_pdf_to_word_desc,
        icon = Icons.Default.Description,
        iconTint = Color(0xFF1976D2) // Dark Blue
    ),
    PDF_TO_PPT(
        toolId = "pdf_to_ppt",
        titleResId = R.string.tool_pdf_to_ppt,
        descResId = R.string.tool_pdf_to_ppt_desc,
        icon = Icons.Default.Slideshow,
        iconTint = Color(0xFFE64A19) // Orange-Red
    ),
    PDF_TO_EXCEL(
        toolId = "pdf_to_excel",
        titleResId = R.string.tool_pdf_to_excel,
        descResId = R.string.tool_pdf_to_excel_desc,
        icon = Icons.Default.TableChart,
        iconTint = Color(0xFF4CAF50) // Green
    ),
    WORD_TO_PDF(
        toolId = "word_to_pdf",
        titleResId = R.string.tool_word_to_pdf,
        descResId = R.string.tool_word_to_pdf_desc,
        icon = Icons.Default.Description,
        iconTint = Color(0xFF3F51B5) // Indigo
    ),
    PPT_TO_PDF(
        toolId = "ppt_to_pdf",
        titleResId = R.string.tool_ppt_to_pdf,
        descResId = R.string.tool_ppt_to_pdf_desc,
        icon = Icons.Default.Slideshow,
        iconTint = Color(0xFFD84315) // Deep Orange
    ),
    EXCEL_TO_PDF(
        toolId = "excel_to_pdf",
        titleResId = R.string.tool_excel_to_pdf,
        descResId = R.string.tool_excel_to_pdf_desc,
        icon = Icons.Default.TableChart,
        iconTint = Color(0xFF009688) // Teal
    ),
    PDF_TO_JPG(
        toolId = "pdf_to_jpg",
        titleResId = R.string.tool_pdf_to_jpg,
        descResId = R.string.tool_pdf_to_jpg_desc,
        icon = Icons.Default.Image,
        iconTint = Color(0xFF00BCD4) // Cyan
    ),
    JPG_TO_PDF(
        toolId = "jpg_to_pdf",
        titleResId = R.string.tool_jpg_to_pdf,
        descResId = R.string.tool_jpg_to_pdf_desc,
        icon = Icons.Default.AddPhotoAlternate,
        iconTint = Color(0xFFE91E63) // Pink
    ),
    SIGN_PDF(
        toolId = "sign_pdf",
        titleResId = R.string.tool_sign_pdf,
        descResId = R.string.tool_sign_pdf_desc,
        icon = Icons.Default.Draw,
        iconTint = Color(0xFF673AB7) // Deep Purple
    ),
    WATERMARK(
        toolId = "watermark",
        titleResId = R.string.tool_watermark,
        descResId = R.string.tool_watermark_desc,
        icon = Icons.Default.BrandingWatermark,
        iconTint = Color(0xFF26A69A) // Teal variant
    ),
    ROTATE_PDF(
        toolId = "rotate_pdf",
        titleResId = R.string.tool_rotate_pdf,
        descResId = R.string.tool_rotate_pdf_desc,
        icon = Icons.Default.RotateRight,
        iconTint = Color(0xFF5C6BC0) // Indigo variant
    ),
    ORGANIZE_PDF(
        toolId = "organize_pdf",
        titleResId = R.string.tool_organize_pdf,
        descResId = R.string.tool_organize_pdf_desc,
        icon = Icons.Default.SwapVert,
        iconTint = Color(0xFF8D6E63) // Brown
    ),
    OCR(
        toolId = "ocr",
        titleResId = R.string.tool_ocr,
        descResId = R.string.tool_ocr_desc,
        icon = Icons.Default.TextFields,
        iconTint = Color(0xFF00ACC1) // Teal / Cyan
    );

    companion object {
        /** Find a PdfTool by its string ID */
        fun fromId(toolId: String): PdfTool? = entries.find { it.toolId == toolId }
    }
}
