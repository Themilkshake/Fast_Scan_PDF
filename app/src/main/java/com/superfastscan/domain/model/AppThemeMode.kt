package com.superfastscan.domain.model

/**
 * Supported app theme modes:
 * - SYSTEM: Follows system dark/light mode
 * - LIGHT: Always light theme
 * - DARK: Always dark theme
 */
enum class AppThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageKey(key: String?): AppThemeMode {
            return entries.firstOrNull { it.storageKey == key } ?: SYSTEM
        }
    }
}
