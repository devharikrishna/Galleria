package com.irah.galleria.ui.mediaviewer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.usecase.DeleteMediaUseCase
import com.irah.galleria.domain.usecase.GetMediaUseCase
import com.irah.galleria.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import android.graphics.Bitmap
import com.irah.galleria.ui.editor.BitmapUtils
import com.irah.galleria.ui.editor.SegmentationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri

data class MediaViewerState(
    val mediaList: List<Media> = emptyList(),
    val initialIndex: Int = 0,
    val isLoading: Boolean = true,
    val isLiftingSubject: Boolean = false,
    val liftMask: Bitmap? = null,
    /** Non-null when the sticker refine overlay should be shown. */
    val stickerSourceBitmap: Bitmap? = null,
    val stickerMaskBitmap: Bitmap? = null
)

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val getMediaUseCase: GetMediaUseCase,
    private val repository: com.irah.galleria.domain.repository.MediaRepository,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val segmentationHelper = SegmentationHelper(context)

    private val _state = MutableStateFlow(MediaViewerState())
    val state: StateFlow<MediaViewerState> = _state.asStateFlow()
    private val mediaId: Long = savedStateHandle[Screen.MediaViewer.MEDIA_ID_ARG] ?: -1L
    private val albumId: Long = savedStateHandle[Screen.MediaViewer.ALBUM_ID_ARG] ?: -1L
    private val sortType: String = savedStateHandle[Screen.MediaViewer.SORT_TYPE_ARG] ?: "Date"
    private val orderDesc: Boolean = savedStateHandle[Screen.MediaViewer.ORDER_DESC_ARG] ?: true
    private val filterTypeStr: String = savedStateHandle[Screen.MediaViewer.FILTER_TYPE_ARG] ?: "All"

    init { loadMedia() }

    private fun loadMedia() {
        viewModelScope.launch {
            val orderType = if (orderDesc) com.irah.galleria.domain.util.OrderType.Descending
                            else com.irah.galleria.domain.util.OrderType.Ascending
            val mediaOrder = when (sortType) {
                "Date" -> com.irah.galleria.domain.util.MediaOrder.Date(orderType)
                "Name" -> com.irah.galleria.domain.util.MediaOrder.Name(orderType)
                "Size" -> com.irah.galleria.domain.util.MediaOrder.Size(orderType)
                else   -> com.irah.galleria.domain.util.MediaOrder.Date(orderType)
            }
            val filterType = when (filterTypeStr) {
                "Images" -> com.irah.galleria.domain.usecase.FilterType.Images
                "Videos" -> com.irah.galleria.domain.usecase.FilterType.Videos
                else     -> com.irah.galleria.domain.usecase.FilterType.All
            }
            getMediaUseCase(albumId = albumId, mediaOrder = mediaOrder, filterType = filterType)
                .collect { mediaList ->
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
            if (intentSender != null) intentSenderLauncher(intentSender)
        }
    }

    fun toggleFavorite(media: Media) {
        viewModelScope.launch { repository.toggleFavorite(media.id.toString()) }
    }

    /**
     * Runs ML segmentation and, on success, opens the interactive sticker editor by storing
     * the source bitmap + raw mask in state. The caller provides [onFailed] for error toasts.
     */
    fun liftSubject(media: Media, onFailed: () -> Unit) {
        if (_state.value.isLiftingSubject) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLiftingSubject = true)
            try {
                val original = BitmapUtils.loadCorrectlyOrientedBitmap(context, media.uri.toUri())
                if (original == null) { onFailed(); return@launch }

                val mask = segmentationHelper.segmentImage(original)

                val isValid = mask?.let { m ->
                    val pixels = IntArray(m.width * m.height)
                    m.getPixels(pixels, 0, m.width, 0, 0, m.width, m.height)
                    pixels.count { (it ushr 24) > 128 } > (m.width * m.height * 0.01)
                } ?: false

                if (mask != null && isValid) {
                    _state.value = _state.value.copy(
                        isLiftingSubject = false,
                        liftMask = mask,
                        stickerSourceBitmap = original,
                        stickerMaskBitmap = mask
                    )
                } else {
                    original.recycle()
                    mask?.recycle()
                    _state.value = _state.value.copy(isLiftingSubject = false)
                    onFailed()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(isLiftingSubject = false)
                onFailed()
            }
        }
    }

    /** Called when the user confirms in the sticker editor. Runs isolateSubject on the refined mask. */
    fun confirmSticker(refinedMask: Bitmap, onResult: (Bitmap?) -> Unit) {
        val src = _state.value.stickerSourceBitmap ?: return
        viewModelScope.launch {
            val isolated = BitmapUtils.isolateSubject(src, refinedMask)
            onResult(isolated)
        }
    }

    /** Dismisses the sticker editor overlay and frees bitmaps. */
    fun dismissSticker() {
        val s = _state.value
        s.stickerSourceBitmap?.recycle()
        s.stickerMaskBitmap?.recycle()
        _state.value = _state.value.copy(
            liftMask = null,
            stickerSourceBitmap = null,
            stickerMaskBitmap = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        segmentationHelper.close()
        dismissSticker()
    }
}
