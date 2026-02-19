package com.irah.galleria.ui.album.components

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
        Text(
            text = "Memories",
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier.padding(top = 8.dp)
        )

        HorizontalMultiBrowseCarousel(
            state = state,
            preferredItemWidth = 120.dp,
            itemSpacing = 8.dp,
            modifier =  modifier
                .padding(vertical = 8.dp)
                .height(180.dp) // Constrain height of the carousel itself
        ) { i ->
            MemoryItem(
                media = memories[i],
                onClick = { onMemoryClick(i) },
                modifier = Modifier.maskClip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            )
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
    // Just the image, sized by the carousel
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
