package com.superfastscan.ui.officetopdf

import androidx.compose.ui.graphics.Color
import com.superfastscan.R
import com.superfastscan.ui.tools.PdfTool

enum class OfficeDocType(
    val tool: PdfTool,
    val titleResId: Int,
    val selectTitleResId: Int,
    val selectSubtitleResId: Int,
    val selectBtnResId: Int,
    val fileInfoResId: Int,
    val primaryMimeType: String,
    val mimeTypes: Array<String>,
    val allowedExtensions: List<String>,
    val accentColor: Color,
    val defaultPrefix: String
) {
    WORD(
        tool = PdfTool.WORD_TO_PDF,
        titleResId = R.string.tool_word_to_pdf,
        selectTitleResId = R.string.word_to_pdf_select_title,
        selectSubtitleResId = R.string.word_to_pdf_select_subtitle,
        selectBtnResId = R.string.word_to_pdf_select_btn,
        fileInfoResId = R.string.word_to_pdf_file_info,
        primaryMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        mimeTypes = arrayOf(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
            "application/rtf",
            "text/rtf",
            "application/vnd.oasis.opendocument.text",
            "text/plain"
        ),
        allowedExtensions = listOf("docx", "doc", "dotx", "dot", "rtf", "odt", "txt"),
        accentColor = Color(0xFF2B579A),
        defaultPrefix = "Word"
    ),
    PPT(
        tool = PdfTool.PPT_TO_PDF,
        titleResId = R.string.tool_ppt_to_pdf,
        selectTitleResId = R.string.ppt_to_pdf_select_title,
        selectSubtitleResId = R.string.ppt_to_pdf_select_subtitle,
        selectBtnResId = R.string.ppt_to_pdf_select_btn,
        fileInfoResId = R.string.ppt_to_pdf_file_info,
        primaryMimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        mimeTypes = arrayOf(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
            "application/vnd.openxmlformats-officedocument.presentationml.template",
            "application/vnd.oasis.opendocument.presentation",
            "application/powerpoint",
            "application/x-mspowerpoint",
            "application/vnd.ms-pps"
        ),
        allowedExtensions = listOf("pptx", "ppt", "ppsx", "pps", "potx", "pot", "odp"),
        accentColor = Color(0xFFD84315),
        defaultPrefix = "Presentation"
    ),
    EXCEL(
        tool = PdfTool.EXCEL_TO_PDF,
        titleResId = R.string.tool_excel_to_pdf,
        selectTitleResId = R.string.excel_to_pdf_select_title,
        selectSubtitleResId = R.string.excel_to_pdf_select_subtitle,
        selectBtnResId = R.string.excel_to_pdf_select_btn,
        fileInfoResId = R.string.excel_to_pdf_file_info,
        primaryMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        mimeTypes = arrayOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
            "application/vnd.ms-excel.sheet.macroEnabled.12",
            "application/vnd.oasis.opendocument.spreadsheet",
            "text/csv",
            "text/comma-separated-values"
        ),
        allowedExtensions = listOf("xlsx", "xls", "xltx", "xlsm", "xlsb", "ods", "csv"),
        accentColor = Color(0xFF009688),
        defaultPrefix = "Spreadsheet"
    );

    companion object {
        fun fromTool(tool: PdfTool): OfficeDocType {
            return when (tool) {
                PdfTool.PPT_TO_PDF -> PPT
                PdfTool.EXCEL_TO_PDF -> EXCEL
                else -> WORD
            }
        }
    }
}
