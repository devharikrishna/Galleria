package com.irah.galleria.ui.gallery.components
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
@Composable
fun FastScroller(
    modifier: Modifier = Modifier,
    gridState: LazyGridState? = null,
    staggeredGridState: LazyStaggeredGridState? = null,
    itemCount: Int,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val density = LocalDensity.current
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
        val scrollInfo by remember(gridState, staggeredGridState, itemCount) {
            derivedStateOf {
                var firstVisibleItemIndex = 0
                if (gridState != null) {
                    firstVisibleItemIndex = gridState.firstVisibleItemIndex
                } else if (staggeredGridState != null) {
                    firstVisibleItemIndex = staggeredGridState.firstVisibleItemIndex
                }
                if (itemCount == 0) 0f to 0 else {
                    val progress = firstVisibleItemIndex.toFloat() / itemCount.toFloat()
                    progress to itemCount
                }
            }
        }
        val (scrollProgress, totalItems) = scrollInfo
        val trackHeight = maxHeight
        val thumbHeight = 48.dp
        val thumbHeightPx = with(density) { thumbHeight.toPx() }
        val trackHeightPx = with(density) { trackHeight.toPx() }
        var isDragging by remember { mutableStateOf(false) }
        var dragOffset by remember { mutableFloatStateOf(0f) }
        var isVisible by remember { mutableStateOf(false) }
        val alpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "Thumb Alpha"
        )
        LaunchedEffect(scrollProgress, isDragging) {
            if (isDragging) {
                isVisible = true
            } else {
                isVisible = true
                kotlinx.coroutines.delay(1500)
                isVisible = false
            }
        }
        LaunchedEffect(scrollProgress, isDragging, trackHeightPx) {
            if (!isDragging && totalItems > 0) {
                dragOffset = scrollProgress * (trackHeightPx - thumbHeightPx)
            }
        }
        val thumbWidth by animateDpAsState(
            targetValue = if (isDragging) 20.dp else 6.dp,
            animationSpec = tween(durationMillis = 200), 
            label = "Thumb Width"
        )
        val thumbElevation by animateDpAsState(
            targetValue = if (isDragging) 6.dp else 2.dp,
            label = "Thumb Elevation"
        )
        val draggableState = rememberDraggableState { delta ->
            if (totalItems > 0) {
                isDragging = true
                dragOffset = (dragOffset + delta).coerceIn(0f, trackHeightPx - thumbHeightPx)
                val newProgress = dragOffset / (trackHeightPx - thumbHeightPx)
                val targetIndex = (newProgress * totalItems).roundToInt()
                    .coerceIn(0, totalItems - 1)
                scope.launch {
                    gridState?.scrollToItem(targetIndex)
                    staggeredGridState?.scrollToItem(targetIndex)
                }
            }
        }
        if (totalItems > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .width(40.dp)  
                    .fillMaxHeight()
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStarted = { 
                            isDragging = true 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStopped = { isDragging = false }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .offset { IntOffset(0, dragOffset.roundToInt()) }
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp)
                        .alpha(alpha)
                ) {
                    Surface(
                        modifier = Modifier
                            .height(thumbHeight)
                            .width(thumbWidth),
                        shape = RoundedCornerShape(24.dp),  
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = thumbElevation,
                        tonalElevation = thumbElevation
                    ) {
                        if (isDragging) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.UnfoldMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}