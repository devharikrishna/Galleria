package com.irah.galleria.domain.model

import androidx.compose.runtime.Immutable

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class GalleryViewType {
    GRID, STAGGERED
}

enum class UiMode {
    MATERIAL, LIQUID_GLASS
}

@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val uiMode: UiMode = UiMode.MATERIAL,
    val galleryViewType: GalleryViewType = GalleryViewType.GRID,
    val galleryGridCount: Int = 2, // Bigger items by default
    val albumGridCount: Int = 2,
    val showMediaCount: Boolean = true,
    val animationsEnabled: Boolean = true,
    val galleryCornerRadius: Int = 12,
    val albumCornerRadius: Int = 12,
    val accentColor: Long = 0xFF6650a4L, // Default Purple40/80 equivalent
    val useDynamicColor: Boolean = true,
    val albumDetailViewType: GalleryViewType = GalleryViewType.GRID,
    val albumDetailGridCount: Int = 3,
    val albumDetailCornerRadius: Int = 12,
    val maxBrightness: Boolean = false,
    val videoAutoplay: Boolean = false,
    val trashEnabled: Boolean = true
)
