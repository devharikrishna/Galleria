package com.irah.galleria.ui.album.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.draw.rotate
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.irah.galleria.domain.model.Media

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesCarousel(
    memories: List<Media>,
    onMemoryClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (memories.isEmpty()) return
    Column(modifier = modifier) {
        val state = rememberCarouselState { memories.count() }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "MEMORIES",
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.height, placeable.width) {
                            val xOffset = (placeable.height - placeable.width) / 2
                            val yOffset = (placeable.width - placeable.height) / 2
                            placeable.place(x = xOffset, y = yOffset)
                        }
                    }
                    .rotate(-90f)
                    .padding(8.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = 8.sp
                ),
                color = MaterialTheme.colorScheme.primary,
            )

            HorizontalMultiBrowseCarousel(
                state = state,
                preferredItemWidth = 150.dp,
                itemSpacing = 10.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(225.dp)
            ) { i ->
                MemoryItem(
                    media = memories[i],
                    onClick = { onMemoryClick(i) },
                    modifier = Modifier.maskClip(RoundedCornerShape(16.dp))
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
fun MemoryItem(
    media: Media,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(media.uri)
            .crossfade(true)
            .build(),
        contentDescription = media.name,
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}
