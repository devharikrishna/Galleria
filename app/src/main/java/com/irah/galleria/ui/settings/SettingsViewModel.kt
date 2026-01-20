package com.irah.galleria.ui.settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irah.galleria.domain.model.AppSettings
import com.irah.galleria.domain.model.GalleryViewType
import com.irah.galleria.domain.model.ThemeMode
import com.irah.galleria.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings()
    )
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
    fun setUiMode(mode: com.irah.galleria.domain.model.UiMode) {
        viewModelScope.launch { settingsRepository.setUiMode(mode) }
    }
    fun setGalleryViewType(type: GalleryViewType) {
        viewModelScope.launch { settingsRepository.setGalleryViewType(type) }
    }
    fun setGalleryGridCount(count: Int) {
        viewModelScope.launch { settingsRepository.setGalleryGridCount(count) }
    }
    fun setAlbumGridCount(count: Int) {
        viewModelScope.launch { settingsRepository.setAlbumGridCount(count) }
    }
    fun setShowMediaCount(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowMediaCount(show) }
    }
    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAnimationsEnabled(enabled) }
    }
    fun setGalleryCornerRadius(radius: Int) {
        viewModelScope.launch { settingsRepository.setGalleryCornerRadius(radius) }
    }
    fun setAlbumCornerRadius(radius: Int) {
        viewModelScope.launch { settingsRepository.setAlbumCornerRadius(radius) }
    }
    fun setAccentColor(color: Long) {
        viewModelScope.launch { settingsRepository.setAccentColor(color) }
    }
    fun setUseDynamicColor(useDynamic: Boolean) {
        viewModelScope.launch { settingsRepository.setUseDynamicColor(useDynamic) }
    }
    fun setAlbumDetailViewType(type: GalleryViewType) {
        viewModelScope.launch { settingsRepository.setAlbumDetailViewType(type) }
    }
    fun setAlbumDetailGridCount(count: Int) {
        viewModelScope.launch { settingsRepository.setAlbumDetailGridCount(count) }
    }
    fun setAlbumDetailCornerRadius(radius: Int) {
        viewModelScope.launch { settingsRepository.setAlbumDetailCornerRadius(radius) }
    }
    fun setMaxBrightness(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMaxBrightness(enabled) }
    }
    fun setVideoAutoplay(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVideoAutoplay(enabled) }
    }
    fun setTrashEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTrashEnabled(enabled) }
    }
}