package com.irah.galleria.ui.album
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irah.galleria.domain.model.Album
import com.irah.galleria.domain.usecase.GetAlbumsUseCase
import com.irah.galleria.domain.util.MediaOrder
import com.irah.galleria.domain.util.OrderType
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
    val isLoading: Boolean = false,
    val mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
)
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val getAlbumsUseCase: GetAlbumsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumState())
    val state: StateFlow<AlbumState> = _state.asStateFlow()
    init {
        loadAlbums()
    }
    private fun loadAlbums() {
        viewModelScope.launch {
            getAlbumsUseCase(mediaOrder = _state.value.mediaOrder)
                .onStart { _state.value = _state.value.copy(isLoading = true) }
                .catch { _state.value = _state.value.copy(isLoading = false) }
                .collect { albums ->
                    _state.value = _state.value.copy(
                        albums = albums,
                        isLoading = false
                    )
                }
        }
    }
}