package com.irah.galleria.ui.gallery.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun DragSelectReceiver(
    items: List<Long>,
    selectedIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    getItemIndexAtPosition: (Offset) -> Int?,
    scrollBy: suspend (Float) -> Float,
    viewportHeight: () -> Int,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val currentItems by rememberUpdatedState(items)
    val currentSelectedIds by rememberUpdatedState(selectedIds)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val currentGetItemIndexAtPosition by rememberUpdatedState(getItemIndexAtPosition)
    val currentScrollBy by rememberUpdatedState(scrollBy)
    val currentViewportHeight by rememberUpdatedState(viewportHeight)
    
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    var initialSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var dragStartItemIndex by remember { mutableStateOf<Int?>(null) }
    var currentDragPosition by remember { mutableStateOf<Offset?>(null) }
    
    // Auto-scroll logic
    LaunchedEffect(currentDragPosition) {
        val position = currentDragPosition ?: return@LaunchedEffect
        while (isActive) {
            val vHeight = currentViewportHeight()
            if (vHeight <= 0) {
                 delay(100)
                 continue
            }
            
            val scrollThreshold = with(density) { 60.dp.toPx() }
            val scrollSpeed = 20f

            val couldScroll = if (position.y < scrollThreshold) {
                 currentScrollBy(-scrollSpeed)
            } else if (position.y > vHeight - scrollThreshold) {
                currentScrollBy(scrollSpeed)
            } else {
                0f
            }
            
            if (couldScroll != 0f) {
                 updateSelection(
                     getItemIndexAtPosition = currentGetItemIndexAtPosition,
                     items = currentItems,
                     dragStartItemIndex = dragStartItemIndex,
                     currentDragPosition = position,
                     initialSelection = initialSelection,
                     onSelectionChange = currentOnSelectionChange
                 )
            }
            delay(10)
        }
    }

    val dragHandlerModifier = Modifier.pointerInput(Unit) {
        detectDragGesturesAfterLongPressStealing(
            onDragStart = { offset ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                val itemIndex = currentGetItemIndexAtPosition(offset)

                if (itemIndex != null && itemIndex in currentItems.indices) {
                    dragStartItemIndex = itemIndex
                    initialSelection = currentSelectedIds
                    
                    val startId = currentItems[itemIndex]
                    // If starting new selection or toggling? 
                    // To follow standard pattern: Add the initial item immediately.
                    // If we want toggle behavior on start item, we can check logic.
                    // For simply drag select: Ensure start item is selected.
                    if (startId !in initialSelection) {
                         onSelectionChange(initialSelection + startId)
                    }
                }
                currentDragPosition = offset
            },
            onDrag = { change, _ ->
                change.consume()
                currentDragPosition = change.position
                updateSelection(
                     getItemIndexAtPosition = currentGetItemIndexAtPosition,
                    items = currentItems,
                    dragStartItemIndex = dragStartItemIndex,
                    currentDragPosition = change.position,
                    initialSelection = initialSelection,
                    onSelectionChange = currentOnSelectionChange
                )
            },
            onDragEnd = {
                currentDragPosition = null
                dragStartItemIndex = null
                initialSelection = emptySet()
            },
            onDragCancel = {
                currentDragPosition = null
                dragStartItemIndex = null
                initialSelection = emptySet()
            }
        )
    }

   content(dragHandlerModifier)
}

private fun updateSelection(
    getItemIndexAtPosition: (Offset) -> Int?,
    items: List<Long>,
    dragStartItemIndex: Int?,
    currentDragPosition: Offset,
    initialSelection: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit
) {
    if (dragStartItemIndex == null) return

    val currentItemIndex = getItemIndexAtPosition(currentDragPosition) ?: return

    val start = minOf(dragStartItemIndex, currentItemIndex)
    val end = maxOf(dragStartItemIndex, currentItemIndex)
    
    val newSelection = items.subList(start, end + 1).toSet()
    
    onSelectionChange(initialSelection + newSelection)
}

suspend fun PointerInputScope.detectDragGesturesAfterLongPressStealing(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        try {
            // We wait for first down, allowing it to be already consumed by children (e.g. clickable)
            val down = awaitFirstDown(requireUnconsumed = false)
            
            // Wait for long press. This tracks the pointer ID.
            // If the pointer moves beyond slop before timeout, it returns null (scroll/tap).
            val longPress = awaitLongPressOrCancellation(down.id)
            
            if (longPress != null) {
                // Long press happened!
                // We shouldn't care if it's consumed or not, we are taking over.
                
                onDragStart(longPress.position)

                // Drag loop
                if (
                    drag(longPress.id) { change ->
                        val dragAmount = change.position - change.previousPosition
                        change.consume() // Consume to prevent others from seeing it
                        onDrag(change, dragAmount)
                    }
                ) {
                    onDragEnd()
                } else {
                    onDragCancel()
                }
            }
        } catch (e: CancellationException) {
            onDragCancel()
            throw e
        }
    }
}
