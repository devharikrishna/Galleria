package com.irah.galleria.ui.album
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irah.galleria.domain.model.Album
import com.irah.galleria.domain.usecase.GetAlbumsUseCase
import com.irah.galleria.domain.util.MediaOrder
import com.irah.galleria.domain.util.OrderType
import com.irah.galleria.domain.repository.MediaRepository
import kotlinx.coroutines.flow.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject
data class AlbumState(
    val albums: List<Album> = emptyList(),
    val smartAlbums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumState())
    val state: StateFlow<AlbumState> = _state.asStateFlow()
    init {
        loadAlbums()
    }
    private fun loadAlbums() {
        viewModelScope.launch {
            combine(
                getAlbumsUseCase(mediaOrder = _state.value.mediaOrder),
                mediaRepository.getFavorites(),
                mediaRepository.getTrashedMedia(),
                mediaRepository.getScreenshots()
            ) { albums, favorites, trash, screenshots ->
                val smartAlbums = mutableListOf<Album>()
                
                // 1. Favorites (-2)
                smartAlbums.add(Album(
                    id = -2L,
                    name = "Favorites",
                    relativePath = null,
                    uri = favorites.firstOrNull()?.uri ?: "",
                    count = favorites.size,
                    timestamp = favorites.maxByOrNull { it.timestamp }?.timestamp ?: 0L
                ))

                // 2. Screenshots (-3)
                smartAlbums.add(Album(
                    id = -3L,
                    name = "Screenshots",
                    relativePath = null,
                    uri = screenshots.firstOrNull()?.uri ?: "",
                    count = screenshots.size,
                    timestamp = screenshots.maxByOrNull { it.timestamp }?.timestamp ?: 0L
                ))

                // 3. Recycle Bin (-4)
                smartAlbums.add(Album(
                    id = -4L,
                    name = "Recycle Bin",
                    relativePath = null,
                    uri = trash.firstOrNull()?.uri ?: "", // Cover
                    count = trash.size,
                    timestamp = System.currentTimeMillis() 
                ))
                
                // Filter out "Screenshots" from regular albums to avoid duplicates
                val filteredAlbums = albums.filter { !it.name.equals("Screenshots", ignoreCase = true) }
                
                Pair(smartAlbums, filteredAlbums)
            }
            .onStart { _state.value = _state.value.copy(isLoading = true) }
            .catch { 
                it.printStackTrace()
                _state.value = _state.value.copy(isLoading = false) 
            }
            .collect { (smartAlbums, regularAlbums) ->
                 _state.value = _state.value.copy(
                    albums = regularAlbums,
                    smartAlbums = smartAlbums,
                    isLoading = false
                 )
            }
        }
    }
}