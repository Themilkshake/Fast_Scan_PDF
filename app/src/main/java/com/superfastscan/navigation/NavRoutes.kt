package com.superfastscan.navigation

import kotlinx.serialization.Serializable

@Serializable
object Home



@Serializable
data class Export(val documentId: Long)

@Serializable
data class EditDocument(val documentId: Long)

@Serializable
object SavedScans

@Serializable
object Settings

@Serializable
data class PdfToolRoute(val toolId: String)
