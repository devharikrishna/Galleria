package com.irah.galleria.domain.usecase

import com.irah.galleria.domain.model.Album
import com.irah.galleria.domain.repository.MediaRepository
import com.irah.galleria.domain.util.MediaOrder
import com.irah.galleria.domain.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(
        mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
    ): Flow<List<Album>> {
        return repository.getAlbums().map { albums ->
            when(mediaOrder.orderType) {
                is OrderType.Ascending -> {
                    when(mediaOrder) {
                        is MediaOrder.Date -> albums.sortedBy { it.timestamp }
                        is MediaOrder.Name -> albums.sortedBy { it.name.lowercase() }
                        is MediaOrder.Size -> albums.sortedBy { it.count } // Sort by count for "Size" in albums context? Or just ignore? User said "Sort methods : date, Name, File Size". File size for album could be total size or count. Let's use count for now as "Size".
                    }
                }
                is OrderType.Descending -> {
                    when(mediaOrder) {
                        is MediaOrder.Date -> albums.sortedByDescending { it.timestamp }
                        is MediaOrder.Name -> albums.sortedByDescending { it.name.lowercase() }
                        is MediaOrder.Size -> albums.sortedByDescending { it.count }
                    }
                }
            }
        }
    }
}
