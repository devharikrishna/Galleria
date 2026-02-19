package com.irah.galleria.domain.usecase

import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.model.Memory
import com.irah.galleria.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import kotlin.random.Random

class GetMemoriesUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<List<Memory>> {
        return repository.getMedia().map { allMedia ->
            val mediaList = allMedia.filter { !it.isVideo }
            if (mediaList.isEmpty()) return@map emptyList<Memory>()
            val memories = mutableListOf<Memory>()
            val calendar = Calendar.getInstance()
            val todayMonth = calendar.get(Calendar.MONTH)
            val todayDay = calendar.get(Calendar.DAY_OF_MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            // 1. On This Day
            val onThisDayItems = mediaList.filter { media ->
                calendar.timeInMillis = media.timestamp * 1000L
                val mediaMonth = calendar.get(Calendar.MONTH)
                val mediaDay = calendar.get(Calendar.DAY_OF_MONTH)
                val mediaYear = calendar.get(Calendar.YEAR)
                
                mediaMonth == todayMonth && mediaDay == todayDay && mediaYear < currentYear
            }

            if (onThisDayItems.isNotEmpty()) {
                memories.add(
                    Memory(
                        id = -100L,
                        title = "On This Day",
                        subtitle = "${onThisDayItems.size} memories",
                        cover = onThisDayItems.random(),
                        items = onThisDayItems.shuffled()
                    )
                )
            }

            // 2. Recent Highlights (Random selection from last 30 days)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val recentItems = mediaList.filter { it.timestamp * 1000L > thirtyDaysAgo }
            
            if (recentItems.size > 5) {
                val highlights = recentItems.shuffled().take(15)
                memories.add(
                    Memory(
                        id = -101L,
                        title = "Recent Highlights",
                        subtitle = "Best of last 30 days",
                        cover = highlights.first(),
                        items = highlights
                    )
                )
            }

            // 3. Throwback (Random selection from > 1 year ago)
            val oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
            val oldItems = mediaList.filter { it.timestamp * 1000L < oneYearAgo }

            if (oldItems.size > 10) {
                 val throwbackItems = oldItems.shuffled().take(20)
                 memories.add(
                    Memory(
                        id = -102L,
                        title = "Throwback",
                        subtitle = "Rediscover old moments",
                        cover = throwbackItems.random(),
                        items = throwbackItems
                    )
                )
            }

            memories
        }
    }

    fun getRandomMemories(limit: Int): Flow<List<Media>> {
        return repository.getMedia().map { allMedia ->
            val mediaList = allMedia.filter { !it.isVideo }
            if (mediaList.isEmpty()) return@map emptyList<Media>()
            mediaList.shuffled().take(limit)
        }
    }
}
