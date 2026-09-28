package com.superfastscan.ui.pdftooffice

import androidx.compose.ui.graphics.Color
import com.superfastscan.R
import com.superfastscan.domain.usecase.OfficeTargetFormat
import com.superfastscan.ui.tools.PdfTool

enum class PdfToOfficeType(
    val tool: PdfTool,
    val titleResId: Int,
    val targetFormat: OfficeTargetFormat,
    val actionBtnResId: Int,
    val accentColor: Color,
    val outputExtension: String,
    val selectSubtitleResId: Int
) {
    WORD(
        tool = PdfTool.PDF_TO_WORD,
        titleResId = R.string.tool_pdf_to_word,
        targetFormat = OfficeTargetFormat.DOCX,
        actionBtnResId = R.string.tool_pdf_to_word,
        accentColor = Color(0xFF1976D2),
        outputExtension = "docx",
        selectSubtitleResId = R.string.pdf_to_word_select_sub
    ),
    EXCEL(
        tool = PdfTool.PDF_TO_EXCEL,
        titleResId = R.string.tool_pdf_to_excel,
        targetFormat = OfficeTargetFormat.EXCEL,
        actionBtnResId = R.string.tool_pdf_to_excel,
        accentColor = Color(0xFF4CAF50),
        outputExtension = "xlsx",
        selectSubtitleResId = R.string.pdf_to_excel_select_sub
    ),
    PPT(
        tool = PdfTool.PDF_TO_PPT,
        titleResId = R.string.tool_pdf_to_ppt,
        targetFormat = OfficeTargetFormat.PPT,
        actionBtnResId = R.string.tool_pdf_to_ppt,
        accentColor = Color(0xFFE64A19),
        outputExtension = "pptx",
        selectSubtitleResId = R.string.pdf_to_ppt_select_sub
    );

    companion object {
        fun fromTool(tool: PdfTool): PdfToOfficeType {
            return when (tool) {
                PdfTool.PDF_TO_EXCEL -> EXCEL
                PdfTool.PDF_TO_PPT -> PPT
                else -> WORD
            }
        }
    }
}
