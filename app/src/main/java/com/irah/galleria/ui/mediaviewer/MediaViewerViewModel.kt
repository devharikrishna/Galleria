package com.irah.galleria.ui.mediaviewer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.usecase.DeleteMediaUseCase
import com.irah.galleria.domain.usecase.GetMediaUseCase
import com.irah.galleria.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
data class MediaViewerState(
    val mediaList: List<Media> = emptyList(),
    val initialIndex: Int = 0,
    val isLoading: Boolean = true
)
@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val getMediaUseCase: GetMediaUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(MediaViewerState())
    val state: StateFlow<MediaViewerState> = _state.asStateFlow()
    private val mediaId: Long = savedStateHandle[Screen.MediaViewer.MEDIA_ID_ARG] ?: -1L
    private val albumId: Long = savedStateHandle[Screen.MediaViewer.ALBUM_ID_ARG] ?: -1L
    init {
        loadMedia()
    }
    private fun loadMedia() {
        viewModelScope.launch {
            getMediaUseCase().collect { allMedia ->
                val filteredMedia = if (albumId != -1L) {
                    allMedia.filter { it.bucketId == albumId }
                } else {
                    allMedia
                }
                val index = filteredMedia.indexOfFirst { it.id == mediaId }
                _state.value = MediaViewerState(
                    mediaList = filteredMedia,
                    initialIndex = if (index != -1) index else 0,
                    isLoading = false
                )
            }
        }
    }
    fun deleteMedia(media: Media, intentSenderLauncher: (android.content.IntentSender) -> Unit) {
         viewModelScope.launch {
             val intentSender = deleteMediaUseCase(listOf(media))
             if (intentSender != null) {
                 intentSenderLauncher(intentSender)
             } else {
             }
         }
    }
}