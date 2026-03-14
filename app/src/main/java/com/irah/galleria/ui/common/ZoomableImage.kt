package com.irah.galleria.ui.common
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import coil.compose.AsyncImagePainter
import kotlin.math.abs
@Composable
fun ZoomableImage(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    liftMask: android.graphics.Bitmap? = null,
    animationTime: Float = 0f,
    onTap: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onZoomChange: (Boolean) -> Unit = {}
) {
    // Add isSpecified check for intrinsicSize
    val intrinsicSize = painter.intrinsicSize
    val isSizeSpecified = intrinsicSize != androidx.compose.ui.geometry.Size.Unspecified
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()

    val renderEffect = remember(liftMask, animationTime, boxSize, intrinsicSize) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && 
            liftMask != null && boxSize.width > 0 && isSizeSpecified) {
            
            val bw = intrinsicSize.width
            val bh = intrinsicSize.height
            val vw = boxSize.width.toFloat()
            val vh = boxSize.height.toFloat()
            
            // Calculate ContentScale.Fit bounds
            val s = kotlin.math.min(vw / bw, vh / bh)
            val dw = bw * s
            val dh = bh * s
            val left = (vw - dw) / 2f
            val top = (vh - dh) / 2f
            
            com.irah.galleria.ui.editor.AgslShader.buildOutlineRenderEffect(
                liftMask, animationTime, vw, vh, left, top, left + dw, top + dh
            )
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                            onZoomChange(false)
                        } else {
                            scale = 2.5f
                            onZoomChange(true)
                        }
                    },
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
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
                                val previousScale = scale
                                scale = (scale * zoomChange).coerceIn(1f, 5f)
                                if (previousScale == 1f && scale > 1f) {
                                    onZoomChange(true)
                                } else if (previousScale > 1f && scale == 1f) {
                                    onZoomChange(false)
                                }
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
                    this.renderEffect = renderEffect
                }
        )
    }
}
