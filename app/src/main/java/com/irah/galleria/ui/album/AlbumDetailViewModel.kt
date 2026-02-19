package com.irah.galleria.ui.album
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.repository.MediaRepository
import com.irah.galleria.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri

data class AlbumDetailState(
    val media: List<Media> = emptyList(),
    val albumName: String = "",
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedMediaIds: Set<Long> = emptySet(),
    val isTrash: Boolean = false,
    val albumId: Long = -1L
)
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getMediaUseCase: com.irah.galleria.domain.usecase.GetMediaUseCase,
    private val repository: MediaRepository,
    private val deleteMediaUseCase: com.irah.galleria.domain.usecase.DeleteMediaUseCase,
    private val getMemoriesUseCase: com.irah.galleria.domain.usecase.GetMemoriesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumDetailState())
    val state: StateFlow<AlbumDetailState> = _state.asStateFlow()
    val operationState = repository.operationState
    private val albumId: Long = savedStateHandle[Screen.AlbumDetail.ALBUM_ID_ARG] ?: -1L
    private val albumName: String = savedStateHandle[Screen.AlbumDetail.ALBUM_NAME_ARG] ?: "Album"

    private val _uiEvent = Channel<AlbumDetailUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        _state.value = _state.value.copy(
            albumName = albumName,
            isTrash = albumId == -4L,
            albumId = albumId
        )
        loadMedia()
    }
    private fun loadMedia() {
        viewModelScope.launch {
             if (albumId != -1L) {
                getMediaUseCase(albumId = albumId).collect { mediaList ->
                     _state.value = _state.value.copy(media = mediaList)
                }
            }
        }
    }
    fun onEvent(event: AlbumDetailEvent) {
        when(event) {
            is AlbumDetailEvent.ToggleSelection -> {
                val currentSelection = _state.value.selectedMediaIds.toMutableSet()
                if (currentSelection.contains(event.mediaId)) {
                    currentSelection.remove(event.mediaId)
                } else {
                    currentSelection.add(event.mediaId)
                }
                _state.value = _state.value.copy(
                    selectedMediaIds = currentSelection,
                    isSelectionMode = currentSelection.isNotEmpty()
                )
            }
            is AlbumDetailEvent.ClearSelection -> {
                _state.value = _state.value.copy(
                    selectedMediaIds = emptySet(),
                    isSelectionMode = false
                )
            }
            is AlbumDetailEvent.UpdateSelection -> {
                _state.value = _state.value.copy(
                    selectedMediaIds = event.selectedIds,
                    isSelectionMode = event.selectedIds.isNotEmpty()
                )
            }
            is AlbumDetailEvent.SelectAll -> {
                val allMediaIds = _state.value.media.map { it.id }.toSet()
                val currentSelectedIds = _state.value.selectedMediaIds
                
                val newSelection = if (currentSelectedIds.containsAll(allMediaIds)) {
                     emptySet()
                } else {
                     allMediaIds
                }
                
                _state.value = _state.value.copy(
                    selectedMediaIds = newSelection,
                    isSelectionMode = newSelection.isNotEmpty()
                )
            }
        }
    }
    fun deleteSelectedMedia(intentSenderLauncher: (android.content.IntentSender) -> Unit) {
        viewModelScope.launch {
            val selectedMedia = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
            val intentSender = deleteMediaUseCase(selectedMedia)
            if (intentSender != null) {
                intentSenderLauncher(intentSender)
            } else {
                 onEvent(AlbumDetailEvent.ClearSelection)
            }
        }
    }
    fun moveSelectedMedia(targetPath: String, intentSenderLauncher: (android.content.IntentSender) -> Unit) {
        viewModelScope.launch {
             val selectedMedia = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
             val intentSender = repository.moveMedia(selectedMedia, targetPath)
             if (intentSender != null) {
                 intentSenderLauncher(intentSender)
             } else {
                 onEvent(AlbumDetailEvent.ClearSelection)
             }
        }
    }
    fun copySelectedMedia(targetPath: String) {
        viewModelScope.launch {
            val selectedMedia = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
            repository.copyMedia(selectedMedia, targetPath)
            onEvent(AlbumDetailEvent.ClearSelection)
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
             context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Media"))
        }
    }
    fun favoriteSelectedMedia() {
        viewModelScope.launch {
             val selectedMedia = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
             val hasNonFavorite = selectedMedia.any { !it.isFavorite }
             selectedMedia.forEach { media ->
                 if (media.isFavorite != hasNonFavorite) {
                     repository.toggleFavorite(media.id.toString())
                 }
             }
             if (hasNonFavorite) {
                 _uiEvent.send(AlbumDetailUiEvent.ShowSnackbar("Added to favorites"))
             } else {
                 _uiEvent.send(AlbumDetailUiEvent.ShowSnackbar("Removed from favorites"))
             }
             onEvent(AlbumDetailEvent.ClearSelection)
        }
    }

     fun restoreSelectedMedia(intentSenderLauncher: (android.content.IntentSender) -> Unit) {
        viewModelScope.launch {
            val selectedMedia = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
            val intentSender = repository.restoreMedia(selectedMedia)
            if (intentSender != null) {
                intentSenderLauncher(intentSender)
            } else {
                 onEvent(AlbumDetailEvent.ClearSelection)
            }
        }
    }

    val albums = repository.getAlbums()
}
sealed class AlbumDetailEvent {
    data class ToggleSelection(val mediaId: Long) : AlbumDetailEvent()
    data class UpdateSelection(val selectedIds: Set<Long>) : AlbumDetailEvent()
    object ClearSelection : AlbumDetailEvent()
    object SelectAll : AlbumDetailEvent()
}

sealed class AlbumDetailUiEvent {
    data class ShowSnackbar(val message: String): AlbumDetailUiEvent()
}