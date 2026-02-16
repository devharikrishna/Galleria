package com.irah.galleria.ui.gallery.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import com.irah.galleria.domain.model.AppSettings
import com.irah.galleria.domain.model.GalleryViewType
import com.irah.galleria.domain.model.Media
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun GalleryGridContent(
    media: com.irah.galleria.ui.util.ImmutableList<Media>,
    selectedIds: com.irah.galleria.ui.util.ImmutableSet<Long>,
    isSelectionMode: Boolean,
    settings: AppSettings,
    contentPadding: PaddingValues,
    gridState: LazyGridState,
    staggeredGridState: LazyStaggeredGridState,
    onMediaClick: (Media) -> Unit,
    onSelectionChange: (Set<Long>) -> Unit,
    onToggleSelection: (Long) -> Unit,
    imageLoader: ImageLoader,
    itemSizePx: Int
) {
    val context = LocalContext.current
    if (settings.galleryViewType == GalleryViewType.GRID) {
        
        // Smart Pre-loading for Grid
        val preloadedIds = remember { mutableSetOf<Long>() }
        
        LaunchedEffect(media, itemSizePx) {
            snapshotFlow {
                gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            }
            .map { it / settings.galleryGridCount } // Map to row index to reduce updates
            .distinctUntilChanged()
            .collectLatest { _ ->
                val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItems = media.size
                val startIndex = lastVisibleIndex + 1
                val endIndex = (startIndex + 20).coerceAtMost(totalItems)
                
                if (startIndex < endIndex) {
                    val itemsToPreload = media.subList(startIndex, endIndex)
                    for (item in itemsToPreload) {
                        if (preloadedIds.add(item.id)) {
                             val request = ImageRequest.Builder(context)
                                .data(item.uri)
                                .size(itemSizePx)
                                .scale(coil.size.Scale.FILL)
                                .precision(coil.size.Precision.EXACT)
                                .build()
                            imageLoader.enqueue(request)
                        }
                    }
                }
            }
        }
        
        val mediaIds = remember(media) { media.map { it.id } }

        DragSelectReceiver(
            items = mediaIds,
            selectedIds = selectedIds.items,
            onSelectionChange = onSelectionChange,
            getItemIndexAtPosition = { offset ->
                gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    val itemOffset = item.offset
                    val itemSize = item.size
                    offset.x >= itemOffset.x - 50 && offset.x <= itemOffset.x + itemSize.width + 50 &&
                    offset.y >= itemOffset.y - 50 && offset.y <= itemOffset.y + itemSize.height + 50
                }?.index
            },
            scrollBy = { gridState.scrollBy(it) },
            viewportHeight = { gridState.layoutInfo.viewportSize.height }
        ) { dragModifier ->
            FastScroller(
                gridState = gridState,
                itemCount = media.size,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(settings.galleryGridCount),
                    modifier = Modifier.fillMaxSize().then(dragModifier),
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = media.items,
                        key = { it.id },
                        contentType = { "media" }
                    ) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        MediaGridItem(
                            media = item,
                            isStaggered = false,
                            cornerRadius = settings.galleryCornerRadius,
                            animationsEnabled = settings.animationsEnabled,
                            isSelected = isSelected,
                            itemSizePx = itemSizePx,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelection(item.id)
                                } else {
                                    onMediaClick(item)
                                }
                            }
                        )
                    }
                }
            }
        }
    } else {
        // Staggered Grid
         val preloadedIds = remember { mutableSetOf<Long>() }

        LaunchedEffect(media, itemSizePx) {
            snapshotFlow {
                staggeredGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            }
            .map { it / settings.galleryGridCount } 
            .distinctUntilChanged()
            .collectLatest { _ ->
                val lastVisibleIndex = staggeredGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItems = media.size
                val startIndex = lastVisibleIndex + 1
                val endIndex = (startIndex + 20).coerceAtMost(totalItems)
                
                if (startIndex < endIndex) {
                    val itemsToPreload = media.subList(startIndex, endIndex)
                    for (item in itemsToPreload) {
                        if (preloadedIds.add(item.id)) {
                             val request = ImageRequest.Builder(context)
                                .data(item.uri)
                                .size(itemSizePx)
                                .scale(coil.size.Scale.FILL)
                                .precision(coil.size.Precision.EXACT)
                                .build()
                            imageLoader.enqueue(request)
                        }
                    }
                }
            }
        }
        
        val mediaIds = remember(media) { media.map { it.id } }

        DragSelectReceiver(
            items = mediaIds,
            selectedIds = selectedIds.items,
            onSelectionChange = onSelectionChange,
            getItemIndexAtPosition = { offset ->
                staggeredGridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    val itemOffset = item.offset
                    val itemSize = item.size
                    offset.x >= itemOffset.x - 50 && offset.x <= itemOffset.x + itemSize.width + 50 &&
                    offset.y >= itemOffset.y - 50 && offset.y <= itemOffset.y + itemSize.height + 50
                }?.index
            },
            scrollBy = { staggeredGridState.scrollBy(it) },
            viewportHeight = { staggeredGridState.layoutInfo.viewportSize.height }
        ) { dragModifier ->
            FastScroller(
                staggeredGridState = staggeredGridState,
                itemCount = media.size,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyVerticalStaggeredGrid(
                    state = staggeredGridState,
                    columns = StaggeredGridCells.Fixed(settings.galleryGridCount),
                    modifier = Modifier.fillMaxSize().then(dragModifier),
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalItemSpacing = 2.dp
                ) {
                    items(
                        items = media.items,
                        key = { it.id },
                        contentType = { "media" }
                    ) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        MediaGridItem(
                            media = item,
                            modifier = Modifier.fillMaxWidth(),
                            isStaggered = true,
                            cornerRadius = settings.galleryCornerRadius,
                            animationsEnabled = settings.animationsEnabled,
                            isSelected = isSelected,
                            itemSizePx = itemSizePx,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelection(item.id)
                                } else {
                                    onMediaClick(item)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
