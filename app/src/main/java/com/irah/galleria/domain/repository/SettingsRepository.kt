package com.irah.galleria.domain.repository

import com.irah.galleria.domain.model.AppSettings
import com.irah.galleria.domain.model.GalleryViewType
import com.irah.galleria.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setUiMode(mode: com.irah.galleria.domain.model.UiMode)
    suspend fun setGalleryViewType(type: GalleryViewType)
    suspend fun setGalleryGridCount(count: Int)
    suspend fun setAlbumGridCount(count: Int)
    suspend fun setShowMediaCount(show: Boolean)
    suspend fun setAnimationsEnabled(enabled: Boolean)
    suspend fun setGalleryCornerRadius(radius: Int)
    suspend fun setAlbumCornerRadius(radius: Int)
    suspend fun setAccentColor(colorValue: Long)
    suspend fun setUseDynamicColor(useDynamic: Boolean)
    suspend fun setAlbumDetailViewType(type: GalleryViewType)
    suspend fun setAlbumDetailGridCount(count: Int)
    suspend fun setAlbumDetailCornerRadius(radius: Int)
    suspend fun setMaxBrightness(enabled: Boolean)
    suspend fun setVideoAutoplay(enabled: Boolean)
    suspend fun setTrashEnabled(enabled: Boolean)
}
