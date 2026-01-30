package com.irah.galleria.ui.recyclebin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
data class RecycleBinState(
    val media: List<Media> = emptyList(),
    val isLoading: Boolean = false,
    val selectedMediaIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)
sealed class RecycleBinEvent {
    data class ToggleSelection(val mediaId: Long): RecycleBinEvent()
    data class UpdateSelection(val selectedIds: Set<Long>): RecycleBinEvent()
    object ClearSelection: RecycleBinEvent()
    object RestoreSelected: RecycleBinEvent()
    object DeleteSelectedForever: RecycleBinEvent()
    object EmptyBin: RecycleBinEvent()
}
@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val getMediaUseCase: com.irah.galleria.domain.usecase.GetMediaUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(RecycleBinState())
    val state: StateFlow<RecycleBinState> = _state.asStateFlow()
    init {
        loadTrashedMedia()
    }
    private fun loadTrashedMedia() {
        viewModelScope.launch {
            getMediaUseCase(albumId = -4L).collect { mediaList ->
                _state.value = _state.value.copy(media = mediaList)
            }
        }
    }
    fun onEvent(event: RecycleBinEvent) {
        when(event) {
            is RecycleBinEvent.ToggleSelection -> {
                val current = _state.value.selectedMediaIds.toMutableSet()
                if (current.contains(event.mediaId)) current.remove(event.mediaId)
                else current.add(event.mediaId)
                _state.value = _state.value.copy(
                    selectedMediaIds = current,
                    isSelectionMode = current.isNotEmpty()
                )
            }
            is RecycleBinEvent.UpdateSelection -> {
                _state.value = _state.value.copy(
                    selectedMediaIds = event.selectedIds,
                    isSelectionMode = event.selectedIds.isNotEmpty()
                )
            }
            RecycleBinEvent.ClearSelection -> {
                _state.value = _state.value.copy(
                    selectedMediaIds = emptySet(),
                    isSelectionMode = false
                )
            }
            RecycleBinEvent.RestoreSelected -> {
            }
            RecycleBinEvent.DeleteSelectedForever -> {
            }
            RecycleBinEvent.EmptyBin -> {
            }
        }
    }
    fun restoreSelected(onIntentSender: (android.content.IntentSender) -> Unit) {
        viewModelScope.launch {
            val selected = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
            val intentSender = repository.restoreMedia(selected)
            if (intentSender != null) {
                onIntentSender(intentSender)
            } else {
                onEvent(RecycleBinEvent.ClearSelection)
            }
        }
    }
    fun deleteForever(onIntentSender: (android.content.IntentSender) -> Unit) {
        viewModelScope.launch {
            val selected = _state.value.media.filter { _state.value.selectedMediaIds.contains(it.id) }
            val intentSender = repository.deleteForever(selected) 
            if (intentSender != null) {
                onIntentSender(intentSender)
            } else {
                onEvent(RecycleBinEvent.ClearSelection)
            }
        }
    }
    fun emptyBin(onIntentSender: (android.content.IntentSender) -> Unit) {
        viewModelScope.launch {
            val allTrashed = _state.value.media
            if (allTrashed.isNotEmpty()) {
                val intentSender = repository.deleteForever(allTrashed)
                if (intentSender != null) {
                    onIntentSender(intentSender)
                }
            }
        }
    }
}