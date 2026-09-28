package com.superfastscan.domain.model

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    TURKISH("tr", "Türkçe");

    companion object {
        fun fromCode(code: String?, fallback: AppLanguage = ENGLISH): AppLanguage =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: fallback
    }
}
