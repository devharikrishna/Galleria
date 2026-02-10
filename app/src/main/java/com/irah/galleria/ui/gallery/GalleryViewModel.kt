package com.irah.galleria.ui.gallery
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.usecase.GetMediaUseCase
import com.irah.galleria.domain.usecase.FilterType
import com.irah.galleria.domain.util.MediaOrder
import com.irah.galleria.domain.util.OrderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri
data class GalleryState(
    val media: com.irah.galleria.ui.util.ImmutableList<Media> = com.irah.galleria.ui.util.ImmutableList(emptyList()),
    val albums: com.irah.galleria.ui.util.ImmutableList<com.irah.galleria.domain.model.Album> = com.irah.galleria.ui.util.ImmutableList(emptyList()),
    val isLoading: Boolean = false,
    val mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending),
    val filterType: FilterType = FilterType.All,
    val isSelectionMode: Boolean = false,
    val selectedMediaIds: com.irah.galleria.ui.util.ImmutableSet<Long> = com.irah.galleria.ui.util.ImmutableSet(emptySet())
)
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getMediaUseCase: GetMediaUseCase,
    private val getAlbumsUseCase: com.irah.galleria.domain.usecase.GetAlbumsUseCase,
    private val mediaRepository: com.irah.galleria.domain.repository.MediaRepository,
    private val deleteMediaUseCase: com.irah.galleria.domain.usecase.DeleteMediaUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(GalleryState())
    val state: StateFlow<GalleryState> = _state.asStateFlow()

    private val _uiEvent = Channel<GalleryUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadMedia()
        loadAlbums()
    }
    private fun loadAlbums() {
        viewModelScope.launch {
            getAlbumsUseCase().collect { albums ->
                _state.value = _state.value.copy(albums = com.irah.galleria.ui.util.ImmutableList(albums))
            }
        }
    }
    private fun loadMedia() {
        viewModelScope.launch {
            getMediaUseCase(mediaOrder = _state.value.mediaOrder, filterType = _state.value.filterType)
                .onStart { _state.value = _state.value.copy(isLoading = true) }
                .catch { _state.value = _state.value.copy(isLoading = false) }
                .collect { mediaList ->
                    _state.value = _state.value.copy(
                        media = com.irah.galleria.ui.util.ImmutableList(mediaList),
                        isLoading = false
                    )
                }
        }
    }
    fun onEvent(event: GalleryEvent) {
        when(event) {
            is GalleryEvent.OrderChange -> {
                if(state.value.mediaOrder::class == event.mediaOrder::class &&
                    state.value.mediaOrder.orderType == event.mediaOrder.orderType) {
                    return
                }
                _state.value = _state.value.copy(mediaOrder = event.mediaOrder)
                loadMedia()
            }
            is GalleryEvent.FilterChange -> {
                if(state.value.filterType == event.filterType) return
                _state.value = _state.value.copy(filterType = event.filterType)
                loadMedia()
            }
            is GalleryEvent.ToggleSelection -> {
                val currentIds = _state.value.selectedMediaIds.items.toMutableSet()
                if (currentIds.contains(event.mediaId)) {
                    currentIds.remove(event.mediaId)
                } else {
                    currentIds.add(event.mediaId)
                }
                val newIds = com.irah.galleria.ui.util.ImmutableSet(currentIds)
                _state.value = _state.value.copy(
                    selectedMediaIds = newIds,
                    isSelectionMode = newIds.isNotEmpty()
                )
            }
            is GalleryEvent.ClearSelection -> {
                _state.value = _state.value.copy(
                    selectedMediaIds = com.irah.galleria.ui.util.ImmutableSet(emptySet()),
                    isSelectionMode = false
                )
            }
            is GalleryEvent.UpdateSelection -> {
                 val newIds = com.irah.galleria.ui.util.ImmutableSet(event.selectedIds)
                _state.value = _state.value.copy(
                    selectedMediaIds = newIds,
                    isSelectionMode = newIds.isNotEmpty()
                )
            }
        }
    }
    fun deleteSelectedMedia(onIntentSender: (android.content.IntentSender) -> Unit) {
        viewModelScope.launch {
            val selectedMedia = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
            val intentSender = deleteMediaUseCase(selectedMedia)
            if (intentSender != null) {
                onIntentSender(intentSender)
            } else {
                 onEvent(GalleryEvent.ClearSelection)
            }
        }
    }
    fun moveSelectedMedia(targetPath: String, onIntentSender: (android.content.IntentSender) -> Unit) {
        viewModelScope.launch {
            val selectedMedia = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
            val intentSender = mediaRepository.moveMedia(selectedMedia, targetPath)
            if (intentSender != null) {
                onIntentSender(intentSender)
            } else {
                 onEvent(GalleryEvent.ClearSelection)
            }
        }
    }
    fun copySelectedMedia(targetPath: String) {
        viewModelScope.launch {
            val selectedMedia = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
            mediaRepository.copyMedia(selectedMedia, targetPath)
            onEvent(GalleryEvent.ClearSelection)
        }
    }
    fun shareSelectedMedia(context: android.content.Context) {
        viewModelScope.launch {
            val selectedMedia = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
            if (selectedMedia.isEmpty()) return@launch
            val uris = ArrayList(selectedMedia.map { it.uri.toUri() })
            val shareIntent = android.content.Intent().apply {
                if (uris.size == 1) {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_STREAM, uris[0])
                    type = selectedMedia[0].mimeType
                } else {
                    action = android.content.Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                    type = "*/*" 
                }
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Media"))
            } catch (e: Exception) {
                // Ignore if no app to handle share
            }
        }
    }

    fun favoriteSelectedMedia() {
        viewModelScope.launch {
             val selectedMedia = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
             val hasNonFavorite = selectedMedia.any { !it.isFavorite }
             selectedMedia.forEach { media ->
                 if (media.isFavorite != hasNonFavorite) {
                     mediaRepository.toggleFavorite(media.id.toString())
                 }
             }
             if (hasNonFavorite) {
                 _uiEvent.send(GalleryUiEvent.ShowSnackbar("Added to favorites"))
             } else {
                 _uiEvent.send(GalleryUiEvent.ShowSnackbar("Removed from favorites"))
             }
             onEvent(GalleryEvent.ClearSelection)
        }
    }
}

sealed class GalleryUiEvent {
    data class ShowSnackbar(val message: String): GalleryUiEvent()
}
sealed class GalleryEvent {
    data class OrderChange(val mediaOrder: MediaOrder): GalleryEvent()
    data class FilterChange(val filterType: FilterType): GalleryEvent()
    data class ToggleSelection(val mediaId: Long): GalleryEvent()
    data class UpdateSelection(val selectedIds: Set<Long>): GalleryEvent()
    object ClearSelection: GalleryEvent()
}