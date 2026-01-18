package com.irah.galleria.ui.gallery.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.ui.graphics.painter.Painter
import coil.compose.AsyncImagePainter
import com.irah.galleria.domain.model.Media
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import com.irah.galleria.ui.common.shimmer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    media: Media,
    modifier: Modifier = Modifier,
    isStaggered: Boolean = false,
    cornerRadius: Int = 12,
    animationsEnabled: Boolean = true,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val containerModifier = if (!isStaggered) {
        modifier.aspectRatio(1f)
    } else {
        // Calculate aspect ratio but CLAMP it to prevent slivers or mega-tall items
        // 0.6f (~portrait) to 1.33f (4:3 landscape). Wide panoramas will be cropped to 4:3.
        val rawRatio = if (media.height > 0) media.width.toFloat() / media.height.toFloat() else 1f
        val ratio = rawRatio.coerceIn(0.6f, 1.33f)
        modifier.aspectRatio(ratio)
    }

    Box(
        modifier = containerModifier
            .padding(2.dp)
            .shadow(2.dp, RoundedCornerShape(cornerRadius.dp)) // Add subtle shadow
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(media.uri)
                .size(300) // Optimize for grid
                .crossfade(animationsEnabled)
                .build()
        )

        // Show Shimmer while loading
        if (painter.state is AsyncImagePainter.State.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmer(animationsEnabled)
            )
        }

        Image(
            painter = painter,
            contentDescription = media.name,
            contentScale = ContentScale.Crop, 
            modifier = Modifier.fillMaxSize()
        )

        // Selection Overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        if (media.isVideo && !isSelected) {
            Icon(
                imageVector = Icons.Default.PlayCircleFilled,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
            )
        }
    }
}
