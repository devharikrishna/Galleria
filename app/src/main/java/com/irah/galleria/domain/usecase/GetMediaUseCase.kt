package com.irah.galleria.domain.usecase
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.repository.MediaRepository
import com.irah.galleria.domain.util.MediaOrder
import com.irah.galleria.domain.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
class GetMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(
        mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending),
        filterType: FilterType = FilterType.All
    ): Flow<List<Media>> {
        return repository.getMedia().map { mediaList ->
            val filteredList = when(filterType) {
                FilterType.All -> mediaList
                FilterType.Images -> mediaList.filter { !it.isVideo }
                FilterType.Videos -> mediaList.filter { it.isVideo }
            }
            when(mediaOrder.orderType) {
                is OrderType.Ascending -> {
                    when(mediaOrder) {
                        is MediaOrder.Date -> filteredList.sortedBy { 
                            if (it.dateTaken > 0) it.dateTaken else it.timestamp * 1000 
                        }
                        is MediaOrder.Name -> filteredList.sortedBy { it.name.lowercase() }
                        is MediaOrder.Size -> filteredList.sortedBy { it.size }
                    }
                }
                is OrderType.Descending -> {
                    when(mediaOrder) {
                        is MediaOrder.Date -> filteredList.sortedByDescending { 
                            if (it.dateTaken > 0) it.dateTaken else it.timestamp * 1000 
                        }
                        is MediaOrder.Name -> filteredList.sortedByDescending { it.name.lowercase() }
                        is MediaOrder.Size -> filteredList.sortedByDescending { it.size }
                    }
                }
            }
        }
    }
}
enum class FilterType {
    All, Images, Videos
}