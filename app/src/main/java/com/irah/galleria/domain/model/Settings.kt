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
enum class BackgroundAnimationType {
     WAVE, BLOB, GRADIENT, PARTICLES, MESH
}
@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val uiMode: UiMode = UiMode.LIQUID_GLASS,
    val galleryViewType: GalleryViewType = GalleryViewType.STAGGERED,
    val galleryGridCount: Int = 2,  
    val albumGridCount: Int = 3,
    val showMediaCount: Boolean = true,
    val animationsEnabled: Boolean = true,
    val galleryCornerRadius: Int = 12,
    val albumCornerRadius: Int = 12,
    val accentColor: Long = 0xFF6650a4L,  
    val useDynamicColor: Boolean = true,
    val albumDetailViewType: GalleryViewType = GalleryViewType.STAGGERED,
    val albumDetailGridCount: Int = 2,
    val albumDetailCornerRadius: Int = 12,
    val maxBrightness: Boolean = false,
    val videoAutoplay: Boolean = false,
    val trashEnabled: Boolean = true,
    val blobAnimation: BackgroundAnimationType = BackgroundAnimationType.WAVE
)