package com.irah.galleria.ui.gallery.components
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.irah.galleria.domain.model.Media
@Composable
fun MediaGridItem(
    media: Media,
    modifier: Modifier = Modifier,
    isStaggered: Boolean = false,
    cornerRadius: Int = 12,
    animationsEnabled: Boolean = true,
    isSelected: Boolean = false,
    itemSizePx: Int,
    onClick: () -> Unit
) {
    val containerModifier = if (!isStaggered) {
        modifier.aspectRatio(1f)
    } else {
        val rawRatio = if (media.height > 0) media.width.toFloat() / media.height.toFloat() else 1f
        val ratio = rawRatio.coerceIn(0.6f, 1.33f)
        modifier.aspectRatio(ratio)
    }

    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius.dp) }
    
    val context = LocalContext.current
    val imageRequest = remember(media.uri, itemSizePx) {
        ImageRequest.Builder(context)
            .data(media.uri)
            .size(itemSizePx)
            .memoryCacheKey("${media.id}_$itemSizePx")
            .diskCacheKey("${media.id}_$itemSizePx")
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }
    
    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        filterQuality = FilterQuality.Low
    )

    Box(
        modifier = containerModifier
            .padding(2.dp)
            .graphicsLayer {
                this.shape = shape
                this.clip = true
            }
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painter,
            contentDescription = media.name,
            contentScale = ContentScale.Crop, 
            modifier = Modifier.fillMaxSize()
        )
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
                    modifier = Modifier.size(30.dp)
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