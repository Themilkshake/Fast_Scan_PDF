package com.superfastscan.domain.model

enum class FilterType(val displayNameResKey: String) {
    ORIGINAL("filter_original"),
    GRAYSCALE("filter_grayscale"),
    BLACK_WHITE("filter_black_white"),
    ENHANCED("filter_enhanced")
}
