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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.getValue
import androidx.core.net.toUri

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

    val targetScale = if (isSelected) 0.85f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = if (isSelected) Spring.DampingRatioMediumBouncy else Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "selection_scale"
    )

    val targetCornerRadius = if (isSelected) 32.dp else cornerRadius.dp
    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "selection_corner_radius"
    )

    val shape = remember(animatedCornerRadius) { RoundedCornerShape(animatedCornerRadius) }
    
    val context = LocalContext.current
    val imageRequest = remember(media.uri, itemSizePx) {
        // Optimization: Use MediaStore THUMBNAIL URI if possible instead of loading the full image and downscaling
        val uriToLoad = if (media.id > 0) {
            android.provider.MediaStore.Files.getContentUri("external").buildUpon()
                .appendPath(media.id.toString())
                .build()
        } else {
            media.uri.toUri() // fallback to original string URI
        }

        ImageRequest.Builder(context)
            .data(uriToLoad)
            .size(itemSizePx)
            .scale(coil.size.Scale.FILL)
            .precision(coil.size.Precision.EXACT)
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(true) 
            .dispatcher(kotlinx.coroutines.Dispatchers.IO)
            .build()
    }

    Box(
        modifier = containerModifier
            .padding(2.dp)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.shape = shape
                this.clip = true
            }
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        coil.compose.AsyncImage(
            model = imageRequest,
            contentDescription = media.name,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Low,
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
                    modifier = Modifier.size(36.dp)
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