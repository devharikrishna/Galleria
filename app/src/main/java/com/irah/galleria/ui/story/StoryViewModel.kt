package com.irah.galleria.ui.story

import androidx.lifecycle.SavedStateHandle
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

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _mediaList = MutableStateFlow<List<Media>>(emptyList())
    val mediaList: StateFlow<List<Media>> = _mediaList.asStateFlow()

    init {
        val mediaIdsString = savedStateHandle.get<String>("mediaIds") ?: ""
        loadMedia(mediaIdsString)
    }

    private fun loadMedia(mediaIdsString: String) {
        if (mediaIdsString.isBlank()) return

        val ids = mediaIdsString.split(",").mapNotNull { it.trim().toLongOrNull() }
        
        viewModelScope.launch {
            val loadedMedia = ids.mapNotNull { id ->
                repository.getMediaById(id)
            }
            _mediaList.value = loadedMedia
        }
    }
    fun toggleFavorite(media: Media) {
        viewModelScope.launch {
            repository.toggleFavorite(media.id.toString())
            // Optimistic update locally if needed, but since we observe from flow in other screens, 
            // here we might need to update the _mediaList manually or re-fetch.
            // Since _mediaList is a static snapshot from IDs, we should update the specific item in the list.
             _mediaList.value = _mediaList.value.map {
                if (it.id == media.id) it.copy(isFavorite = !it.isFavorite) else it
            }
        }
    }

    fun deleteMedia(media: Media, onIntentSender: (android.content.IntentSender) -> Unit) {
        viewModelScope.launch {
            val intentSender = repository.deleteMedia(listOf(media))
            if (intentSender != null) {
                onIntentSender(intentSender)
            }
        }
    }

    // Call this if delete was successful to remove from list
    fun onMediaDeleted(mediaId: Long) {
        _mediaList.value = _mediaList.value.filter { it.id != mediaId }
    }
}
