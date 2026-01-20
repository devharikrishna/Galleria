package com.irah.galleria.ui.common
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import coil.compose.AsyncImagePainter
import kotlin.math.abs
@Composable
fun ZoomableImage(
    painter: AsyncImagePainter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = { onTap() }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.fastAny { it.isConsumed }
                        if (canceled) break
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        if (!pastTouchSlop) {
                            zoom *= zoomChange
                            pan += panChange
                            val centroidSize = event.calculateCentroidSize(useCurrent = false)
                            val zoomMotion = abs(1 - zoom) * centroidSize
                            val panMotion = pan.getDistance()
                            if (zoomMotion > touchSlop ||
                                panMotion > touchSlop
                            ) {
                                pastTouchSlop = true
                            }
                        }
                        if (pastTouchSlop) {
                            val isMultiTouch = event.changes.size > 1
                            if (scale == 1f && !isMultiTouch) {
                            } else {
                                event.changes.fastForEach { 
                                    if (it.positionChanged()) {
                                        it.consume()
                                    }
                                }
                                scale = (scale * zoomChange).coerceIn(1f, 5f)
                                val extraWidth = (scale - 1) * screenWidth
                                val extraHeight = (scale - 1) * screenHeight
                                val maxX = extraWidth / 2
                                val maxY = extraHeight / 2
                                offset = Offset(
                                    x = (offset.x + panChange.x * scale).coerceIn(-maxX, maxX),
                                    y = (offset.y + panChange.y * scale).coerceIn(-maxY, maxY)
                                )
                            }
                        }
                    } while (event.changes.fastAny { it.pressed })
                }
            }
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}