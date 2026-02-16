package com.irah.galleria.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class CropHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP, BOTTOM, LEFT, RIGHT,
    CENTER, NONE
}

@Composable
fun CropOverlay(
    modifier: Modifier = Modifier,
    imageBounds: Rect, // The screen coordinates of the image
    adjustments: BitmapUtils.Adjustments,
    onCropChange: (Rect?) -> Unit,
    onCommit: () -> Unit,
    fixedAspectRatio: Float? = null
) {

    var uiCropRect by remember(adjustments.cropRect) {
        mutableStateOf(adjustments.cropRect ?: Rect(0f, 0f, 1f, 1f))
    }
    

    var activeHandle by remember { mutableStateOf(CropHandle.NONE) }


    val density = LocalDensity.current
    val touchSlop = remember { with(density) { 48.dp.toPx() } } // Large touch target

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(imageBounds, fixedAspectRatio) {
                    detectTapGestures(
                        onPress = { offset ->

                            
                            val l = imageBounds.left + uiCropRect.left * imageBounds.width
                            val t = imageBounds.top + uiCropRect.top * imageBounds.height
                            val r = imageBounds.left + uiCropRect.right * imageBounds.width
                            val b = imageBounds.top + uiCropRect.bottom * imageBounds.height
                            

                            activeHandle = when {
                                (Offset(l, t) - offset).getDistance() < touchSlop -> CropHandle.TOP_LEFT
                                (Offset(r, t) - offset).getDistance() < touchSlop -> CropHandle.TOP_RIGHT
                                (Offset(l, b) - offset).getDistance() < touchSlop -> CropHandle.BOTTOM_LEFT
                                (Offset(r, b) - offset).getDistance() < touchSlop -> CropHandle.BOTTOM_RIGHT
                                

                                abs(offset.x - l) < touchSlop && offset.y in t..b -> CropHandle.LEFT
                                abs(offset.x - r) < touchSlop && offset.y in t..b -> CropHandle.RIGHT
                                abs(offset.y - t) < touchSlop && offset.x in l..r -> CropHandle.TOP
                                abs(offset.y - b) < touchSlop && offset.x in l..r -> CropHandle.BOTTOM
                                

                                offset.x in l..r && offset.y in t..b -> CropHandle.CENTER
                                else -> CropHandle.NONE
                            }
                        }
                    )
                }
                .pointerInput(imageBounds, fixedAspectRatio) {
                    detectDragGestures(
                        onDragEnd = {
                            if (activeHandle != CropHandle.NONE) {
                                onCropChange(uiCropRect)
                                activeHandle = CropHandle.NONE
                            }
                        },
                        onDragCancel = { activeHandle = CropHandle.NONE },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeHandle != CropHandle.NONE) {
                                uiCropRect = updateCropRect(
                                    current = uiCropRect,
                                    handle = activeHandle,
                                    drag = dragAmount,
                                    bounds = imageBounds,
                                    aspectRatio = fixedAspectRatio
                                )
                            }
                        }
                    )
                }
        ) {

            val l = imageBounds.left + uiCropRect.left * imageBounds.width
            val t = imageBounds.top + uiCropRect.top * imageBounds.height
            val r = imageBounds.left + uiCropRect.right * imageBounds.width
            val b = imageBounds.top + uiCropRect.bottom * imageBounds.height
            val w = size.width
            val h = size.height
            
            val dimColor = Color.Black.copy(alpha = 0.7f)
            

            drawRect(dimColor, topLeft = Offset(0f, 0f), size = Size(w, t))
            drawRect(dimColor, topLeft = Offset(0f, b), size = Size(w, h - b))
            drawRect(dimColor, topLeft = Offset(0f, t), size = Size(l, b - t))
            drawRect(dimColor, topLeft = Offset(r, t), size = Size(w - r, b - t))
            

            val gridColor = Color.White.copy(alpha = 0.3f)
            val cw = r - l
            val ch = b - t
            
            if (activeHandle != CropHandle.NONE) {
                drawLine(gridColor, Offset(l + cw/3, t), Offset(l + cw/3, b))
                drawLine(gridColor, Offset(l + 2*cw/3, t), Offset(l + 2*cw/3, b))
                drawLine(gridColor, Offset(l, t + ch/3), Offset(r, t + ch/3))
                drawLine(gridColor, Offset(l, t + 2*ch/3), Offset(r, t + 2*ch/3))
            }
            

            drawRect(Color.White, topLeft = Offset(l, t), size = Size(cw, ch), style = Stroke(2.dp.toPx()))
            

            val cornerLen = 20.dp.toPx()
            val cornerThick = 4.dp.toPx()
            

            drawLine(Color.White, Offset(l, t), Offset(l + cornerLen, t), cornerThick)
            drawLine(Color.White, Offset(l, t), Offset(l, t + cornerLen), cornerThick)

            drawLine(Color.White, Offset(r, t), Offset(r - cornerLen, t), cornerThick)
            drawLine(Color.White, Offset(r, t), Offset(r, t + cornerLen), cornerThick)

            drawLine(Color.White, Offset(l, b), Offset(l + cornerLen, b), cornerThick)
            drawLine(Color.White, Offset(l, b), Offset(l, b - cornerLen), cornerThick)

            drawLine(Color.White, Offset(r, b), Offset(r - cornerLen, b), cornerThick)
            drawLine(Color.White, Offset(r, b), Offset(r, b - cornerLen), cornerThick)
            

            val pillLen = 24.dp.toPx()
            val pillThick = 4.dp.toPx()
            

            drawLine(Color.White, Offset(l + cw/2 - pillLen/2, t), Offset(l + cw/2 + pillLen/2, t), pillThick)

            drawLine(Color.White, Offset(l + cw/2 - pillLen/2, b), Offset(l + cw/2 + pillLen/2, b), pillThick)

            drawLine(Color.White, Offset(l, t + ch/2 - pillLen/2), Offset(l, t + ch/2 + pillLen/2), pillThick)

            drawLine(Color.White, Offset(r, t + ch/2 - pillLen/2), Offset(r, t + ch/2 + pillLen/2), pillThick)
        }
    }
}


fun updateCropRect(
    current: Rect,
    handle: CropHandle,
    drag: Offset,
    bounds: Rect,
    aspectRatio: Float?
): Rect {

    val dx = drag.x / bounds.width
    val dy = drag.y / bounds.height
    
    var l = current.left
    var t = current.top
    var r = current.right
    var b = current.bottom
    
    val minSize = 0.05f
    
    if (handle == CropHandle.CENTER) {
        if (l + dx >= 0 && r + dx <= 1) { l += dx; r += dx }
        if (t + dy >= 0 && b + dy <= 1) { t += dy; b += dy }
        return Rect(l, t, r, b)
    }
    

    when (handle) {
         CropHandle.TOP_LEFT -> { l += dx; t += dy }
         CropHandle.TOP_RIGHT -> { r += dx; t += dy }
         CropHandle.BOTTOM_LEFT -> { l += dx; b += dy }
         CropHandle.BOTTOM_RIGHT -> { r += dx; b += dy }
         CropHandle.TOP -> t += dy
         CropHandle.BOTTOM -> b += dy
         CropHandle.LEFT -> l += dx
         CropHandle.RIGHT -> r += dx
         else -> {}
    }
    

    if (aspectRatio != null) {
        val imageRatio = bounds.width / bounds.height
        val targetRectRatio = BitmapUtils.ratioToRectRatio(aspectRatio, imageRatio)
        
        val newW = r - l
        val newH = b - t
        
        when (handle) {
            CropHandle.LEFT, CropHandle.RIGHT -> {
                 val reqH = newW / targetRectRatio
                 val deltaH = reqH - newH
                 t -= deltaH / 2
                 b += deltaH / 2
            }
            CropHandle.TOP, CropHandle.BOTTOM -> {
                val reqW = newH * targetRectRatio
                val deltaW = reqW - newW
                l -= deltaW / 2
                r += deltaW / 2
            }
            else -> {
                 val reqH = newW / targetRectRatio

                 if (handle == CropHandle.TOP_LEFT || handle == CropHandle.TOP_RIGHT) {
                     t = b - reqH
                 } else {
                     b = t + reqH
                 }
            }
        }
    }
    

    l = l.coerceIn(0f, r - minSize)
    t = t.coerceIn(0f, b - minSize)
    r = r.coerceIn(l + minSize, 1f)
    b = b.coerceIn(t + minSize, 1f)
    
    return Rect(l, t, r, b)
}

@Composable
fun CropRatioButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
    }
}

