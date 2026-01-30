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
    private val repository: com.irah.galleria.domain.repository.MediaRepository,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(MediaViewerState())
    val state: StateFlow<MediaViewerState> = _state.asStateFlow()
    private val mediaId: Long = savedStateHandle[Screen.MediaViewer.MEDIA_ID_ARG] ?: -1L
    private val albumId: Long = savedStateHandle[Screen.MediaViewer.ALBUM_ID_ARG] ?: -1L
    private val sortType: String = savedStateHandle[Screen.MediaViewer.SORT_TYPE_ARG] ?: "Date"
    private val orderDesc: Boolean = savedStateHandle[Screen.MediaViewer.ORDER_DESC_ARG] ?: true
    private val filterTypeStr: String = savedStateHandle[Screen.MediaViewer.FILTER_TYPE_ARG] ?: "All"

    init {
        loadMedia()
    }
    private fun loadMedia() {
        viewModelScope.launch {
            val orderType = if (orderDesc) com.irah.galleria.domain.util.OrderType.Descending else com.irah.galleria.domain.util.OrderType.Ascending
            val mediaOrder = when(sortType) {
                "Date" -> com.irah.galleria.domain.util.MediaOrder.Date(orderType)
                "Name" -> com.irah.galleria.domain.util.MediaOrder.Name(orderType)
                "Size" -> com.irah.galleria.domain.util.MediaOrder.Size(orderType)
                else -> com.irah.galleria.domain.util.MediaOrder.Date(orderType)
            }
            val filterType = when(filterTypeStr) {
                 "Images" -> com.irah.galleria.domain.usecase.FilterType.Images
                 "Videos" -> com.irah.galleria.domain.usecase.FilterType.Videos
                 else -> com.irah.galleria.domain.usecase.FilterType.All
            }

            // GetMediaUseCase now handles album filtering and consistent sorting
            getMediaUseCase(
                albumId = albumId,
                mediaOrder = mediaOrder,
                filterType = filterType
            ).collect { mediaList ->
                val index = mediaList.indexOfFirst { it.id == mediaId }
                _state.value = MediaViewerState(
                    mediaList = mediaList,
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
    fun toggleFavorite(media: Media) {
        viewModelScope.launch {
            repository.toggleFavorite(media.id.toString())
        }
    }
}