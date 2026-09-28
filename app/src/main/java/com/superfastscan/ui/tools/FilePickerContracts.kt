package com.superfastscan.ui.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/**
 * Activity result contract specifically for picking a single PDF document.
 * Unlike AndroidX's default OpenDocument contract which hardcodes type = "* / *",
 * this contract sets intent.type = "application/pdf" and EXTRA_MIME_TYPES,
 * strictly restricting system and OEM file choosers to PDF files only.
 */
class OpenPdfContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf"))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
    }
}

/**
 * Activity result contract specifically for picking multiple PDF documents (e.g. for Merge PDF).
 * Strictly restricts the file picker to PDF files only with multiple selection enabled.
 */
class OpenMultiplePdfsContract : ActivityResultContract<Unit, List<Uri>>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()
        val clipData = intent.clipData
        if (clipData != null) {
            val list = ArrayList<Uri>(clipData.itemCount)
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i).uri?.let { list.add(it) }
            }
            return list
        }
        return intent.data?.let { listOf(it) } ?: emptyList()
    }
}

/**
 * Activity result contract for picking office documents (Word, PPT, Excel)
 * with a tailored primary MIME type and strict MIME types list (excluding wildcards).
 */
class OpenOfficeDocumentContract(
    private val primaryMimeType: String,
    private val mimeTypes: Array<String>
) : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = primaryMimeType
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
    }
}
