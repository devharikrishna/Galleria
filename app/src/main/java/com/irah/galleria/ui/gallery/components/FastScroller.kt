package com.irah.galleria.ui.gallery.components
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
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
        
        val trackHeight = maxHeight
        val thumbHeight = 48.dp
        val thumbHeightPx = with(density) { thumbHeight.toPx() }
        val trackHeightPx = with(density) { trackHeight.toPx() }
        val scrollableRange = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
        
        var isDragging by remember { mutableStateOf(false) }
        var isVisible by remember { mutableStateOf(false) }
        var dragTargetOffset by remember { mutableFloatStateOf(0f) }
        var pendingScrollIndex by remember { mutableIntStateOf(-1) }
        
        // Calculate scroll progress from grid state
        val scrollProgress by remember(gridState, staggeredGridState, itemCount) {
            derivedStateOf {
                if (itemCount == 0) {
                    0f
                } else {
                    val canScrollForward = gridState?.canScrollForward ?: staggeredGridState?.canScrollForward ?: false
                    
                    if (!canScrollForward) {
                        1f
                    } else {
                        val firstVisibleIndex = (gridState?.firstVisibleItemIndex ?: staggeredGridState?.firstVisibleItemIndex ?: 0).toFloat()
                        
                        val visibleCount = (gridState?.layoutInfo?.visibleItemsInfo?.size 
                            ?: staggeredGridState?.layoutInfo?.visibleItemsInfo?.size 
                            ?: 0)
                        
                        val maxScrollIndex = (itemCount - visibleCount).coerceAtLeast(1).toFloat()
                        (firstVisibleIndex / maxScrollIndex).coerceIn(0f, 1f)
                    }
                }
            }
        }
        
        // Animated thumb position for smooth tracking when not dragging
        val animatedThumbOffset by animateFloatAsState(
            targetValue = if (isDragging) dragTargetOffset else scrollProgress * scrollableRange,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = if (isDragging) Spring.StiffnessHigh else Spring.StiffnessLow
            ),
            label = "Thumb Position"
        )
        
        // Visibility animation
        val alpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "Thumb Alpha"
        )
        
        val isScrolling = gridState?.isScrollInProgress == true || staggeredGridState?.isScrollInProgress == true
        
        LaunchedEffect(isScrolling, isDragging) {
            if (isScrolling || isDragging) {
                isVisible = true
            } else {
                kotlinx.coroutines.delay(1500)
                isVisible = false
            }
        }
        
        // Debounced scroll execution for smoother performance
        LaunchedEffect(Unit) {
            snapshotFlow { pendingScrollIndex }
                .distinctUntilChanged()
                .collect { index ->
                    if (index >= 0 && isDragging) {
                        gridState?.scrollToItem(index)
                        staggeredGridState?.scrollToItem(index)
                    }
                }
        }
        
        val thumbWidth by animateDpAsState(
            targetValue = if (isDragging) 20.dp else 6.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "Thumb Width"
        )
        
        val thumbElevation by animateDpAsState(
            targetValue = if (isDragging) 6.dp else 2.dp,
            animationSpec = tween(durationMillis = 150),
            label = "Thumb Elevation"
        )
        
        val draggableState = rememberDraggableState { delta ->
            if (itemCount > 0) {
                dragTargetOffset = (dragTargetOffset + delta).coerceIn(0f, scrollableRange)
                val newProgress = dragTargetOffset / scrollableRange
                val targetIndex = (newProgress * itemCount).roundToInt().coerceIn(0, itemCount - 1)
                pendingScrollIndex = targetIndex
            }
        }
        
        if (itemCount > 0) {
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
                            dragTargetOffset = scrollProgress * scrollableRange
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStopped = { 
                            isDragging = false
                            pendingScrollIndex = -1
                        }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .offset { IntOffset(0, animatedThumbOffset.roundToInt()) }
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