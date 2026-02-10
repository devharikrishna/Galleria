package com.irah.galleria.data.repository
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.irah.galleria.domain.model.AppSettings
import com.irah.galleria.domain.model.GalleryViewType
import com.irah.galleria.domain.model.ThemeMode
import com.irah.galleria.domain.model.UiMode
import com.irah.galleria.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SettingsRepository {
    private val dataStore = context.dataStore
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val VIEW_TYPE = stringPreferencesKey("view_type")
        val GALLERY_GRID_COUNT = intPreferencesKey("gallery_grid_count")
        val ALBUM_GRID_COUNT = intPreferencesKey("album_grid_count")
        val SHOW_MEDIA_COUNT = booleanPreferencesKey("show_media_count")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val GALLERY_CORNER_RADIUS = intPreferencesKey("gallery_corner_radius")
        val ALBUM_CORNER_RADIUS = intPreferencesKey("album_corner_radius")
        val ACCENT_COLOR = androidx.datastore.preferences.core.longPreferencesKey("accent_color")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val ALBUM_DETAIL_VIEW_TYPE = stringPreferencesKey("album_detail_view_type")
        val ALBUM_DETAIL_GRID_COUNT = intPreferencesKey("album_detail_grid_count")
        val ALBUM_DETAIL_CORNER_RADIUS = intPreferencesKey("album_detail_corner_radius")
        val MAX_BRIGHTNESS = booleanPreferencesKey("max_brightness")
        val VIDEO_AUTOPLAY = booleanPreferencesKey("video_autoplay")
        val UI_MODE = stringPreferencesKey("ui_mode_v2")
        val TRASH_ENABLED = booleanPreferencesKey("trash_enabled")
        val BLOB_ANIMATION = stringPreferencesKey("blob_animation")
    }
    override val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            themeMode = try {
                ThemeMode.valueOf(preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) { ThemeMode.SYSTEM },
            uiMode = try {
                UiMode.valueOf(preferences[Keys.UI_MODE] ?: UiMode.LIQUID_GLASS.name)
            } catch (e: Exception) { UiMode.LIQUID_GLASS },
            galleryViewType = try {
                GalleryViewType.valueOf(preferences[Keys.VIEW_TYPE] ?: GalleryViewType.GRID.name)
            } catch (e: Exception) { GalleryViewType.STAGGERED },
            galleryGridCount = preferences[Keys.GALLERY_GRID_COUNT] ?: 3,
            albumGridCount = preferences[Keys.ALBUM_GRID_COUNT] ?: 3,
            showMediaCount = preferences[Keys.SHOW_MEDIA_COUNT] ?: true,
            animationsEnabled = preferences[Keys.ANIMATIONS_ENABLED] ?: true,
            galleryCornerRadius = preferences[Keys.GALLERY_CORNER_RADIUS] ?: 12,
            albumCornerRadius = preferences[Keys.ALBUM_CORNER_RADIUS] ?: 12,
            accentColor = preferences[Keys.ACCENT_COLOR] ?: 0xFF6650a4L,
            useDynamicColor = preferences[Keys.USE_DYNAMIC_COLOR] ?: true,
            albumDetailViewType = try {
                GalleryViewType.valueOf(preferences[Keys.ALBUM_DETAIL_VIEW_TYPE] ?: GalleryViewType.STAGGERED.name)
            } catch (e: Exception) { GalleryViewType.STAGGERED },
            albumDetailGridCount = preferences[Keys.ALBUM_DETAIL_GRID_COUNT] ?: 2,
            albumDetailCornerRadius = preferences[Keys.ALBUM_DETAIL_CORNER_RADIUS] ?: 12,
            maxBrightness = preferences[Keys.MAX_BRIGHTNESS] ?: false,
            trashEnabled = preferences[Keys.TRASH_ENABLED] ?: true,
            blobAnimation = try {
                com.irah.galleria.domain.model.BackgroundAnimationType.valueOf(preferences[Keys.BLOB_ANIMATION] ?: com.irah.galleria.domain.model.BackgroundAnimationType.WAVE.name)
            } catch (e: Exception) { com.irah.galleria.domain.model.BackgroundAnimationType.WAVE }
        )
    }
    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
    override suspend fun setGalleryViewType(type: GalleryViewType) {
        dataStore.edit { it[Keys.VIEW_TYPE] = type.name }
    }
    override suspend fun setGalleryGridCount(count: Int) {
        dataStore.edit { it[Keys.GALLERY_GRID_COUNT] = count }
    }
    override suspend fun setAlbumGridCount(count: Int) {
        dataStore.edit { it[Keys.ALBUM_GRID_COUNT] = count }
    }
    override suspend fun setShowMediaCount(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_MEDIA_COUNT] = show }
    }
    override suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
    }
    override suspend fun setGalleryCornerRadius(radius: Int) {
        dataStore.edit { it[Keys.GALLERY_CORNER_RADIUS] = radius }
    }
    override suspend fun setAlbumCornerRadius(radius: Int) {
        dataStore.edit { it[Keys.ALBUM_CORNER_RADIUS] = radius }
    }
    override suspend fun setAccentColor(colorValue: Long) {
        dataStore.edit { it[Keys.ACCENT_COLOR] = colorValue }
    }
    override suspend fun setUseDynamicColor(useDynamic: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = useDynamic }
    }
    override suspend fun setAlbumDetailViewType(type: GalleryViewType) {
        dataStore.edit { it[Keys.ALBUM_DETAIL_VIEW_TYPE] = type.name }
    }
    override suspend fun setAlbumDetailGridCount(count: Int) {
        dataStore.edit { it[Keys.ALBUM_DETAIL_GRID_COUNT] = count }
    }
    override suspend fun setAlbumDetailCornerRadius(radius: Int) {
        dataStore.edit { it[Keys.ALBUM_DETAIL_CORNER_RADIUS] = radius }
    }
    override suspend fun setMaxBrightness(enabled: Boolean) {
        dataStore.edit { it[Keys.MAX_BRIGHTNESS] = enabled }
    }
    override suspend fun setVideoAutoplay(enabled: Boolean) {
        dataStore.edit { it[Keys.VIDEO_AUTOPLAY] = enabled }
    }
    override suspend fun setUiMode(mode: com.irah.galleria.domain.model.UiMode) {
        dataStore.edit { it[Keys.UI_MODE] = mode.name }
    }
    override suspend fun setTrashEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.TRASH_ENABLED] = enabled }
    }
    override suspend fun setBlobAnimation(type: com.irah.galleria.domain.model.BackgroundAnimationType) {
        dataStore.edit { it[Keys.BLOB_ANIMATION] = type.name }
    }
}