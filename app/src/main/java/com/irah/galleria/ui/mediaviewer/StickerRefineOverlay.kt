package com.irah.galleria.ui.mediaviewer

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color as ComposeColor

// ─────────────────────────────────────────────────────────────────────────────
private enum class BrushMode { ADD, REMOVE, WAND }
private const val MAX_UNDO = 20
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StickerRefineOverlay(
    originalBitmap: Bitmap,
    initialMask: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val maskW = initialMask.width
    val maskH = initialMask.height
    val scope = rememberCoroutineScope()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // ── mask state ────────────────────────────────────────────────────────────
    val maskAlpha: IntArray = remember {
        val argb = initialMask.copy(Bitmap.Config.ARGB_8888, false)
        val px = IntArray(maskW * maskH)
        argb.getPixels(px, 0, maskW, 0, 0, maskW, maskH)
        argb.recycle()
        IntArray(maskW * maskH) { i -> if ((px[i] ushr 24) and 0xFF > 127) 1 else 0 }
    }
    val undoStack = remember { ArrayDeque<IntArray>() }

    // Pre-scaled source at mask resolution for color-aware operations (Wand, future)
    val scaledSourcePixels: IntArray = remember {
        val scaled = originalBitmap.scale(maskW, maskH)
        val px = IntArray(maskW * maskH)
        scaled.getPixels(px, 0, maskW, 0, 0, maskW, maskH)
        scaled.recycle()
        px
    }

    var redrawTrigger by remember { mutableIntStateOf(0) }
    var overlayBitmap by remember { mutableStateOf(buildOverlay(maskAlpha, maskW, maskH)) }
    LaunchedEffect(redrawTrigger) {
        if (redrawTrigger > 0)
            overlayBitmap = withContext(Dispatchers.Default) { buildOverlay(maskAlpha, maskW, maskH) }
    }

    // ── tool state ────────────────────────────────────────────────────────────
    var brushMode by remember { mutableStateOf(BrushMode.ADD) }
    var brushRadius by remember { mutableFloatStateOf(28f) }
    var wandTolerance by remember { mutableFloatStateOf(0.25f) }  // 0..1
    var isProcessingWand by remember { mutableStateOf(false) }

    // ── zoom / pan state (per image-area container) ───────────────────────────
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // ── cursor (screen coordinates, no zoom applied) ────────────────────────────────
    var cursorPos by remember { mutableStateOf<Offset?>(null) }

    // ── image-area container size (updated via onSizeChanged on the image Box) ─
    var imgContainerSize by remember { mutableStateOf(IntSize.Zero) }
    val imageRect = remember(imgContainerSize) {
        computeFitRect(imgContainerSize, originalBitmap.width, originalBitmap.height)
    }

    // ── animations ────────────────────────────────────────────────────────────
    // Tracks whether the user has made any manual refinement
    var hasInteracted by remember { mutableStateOf(false) }

    val inf = rememberInfiniteTransition(label = "edge")
    // Glow pulses always
    val glowAlpha by inf.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    // Marching ants: only while user hasn’t interacted yet
    val marchPhase by inf.animateFloat(
        0f, 40f,
        infiniteRepeatable(tween(800, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart),
        label = "march"
    )
    // Sweep: one-shot Animatable — runs once when overlay appears, then stops
    val sweepFraction = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        sweepFraction.animateTo(
            targetValue = 1f,
            animationSpec = tween(2000, easing = androidx.compose.animation.core.LinearEasing)
        )
        // stays at 1f — scan line disappears off the right edge
    }

    // ── debounced redraw ──────────────────────────────────────────────────────
    var pendingRedraw by remember { mutableStateOf<Job?>(null) }
    fun scheduleRedraw() {
        pendingRedraw?.cancel()
        pendingRedraw = scope.launch { delay(25); redrawTrigger++ }
    }

    // ── zoom-aware coordinate transform: screen → mask pixel ──────────────────
    fun screenToMask(screenPos: Offset): Pair<Int, Int>? {
        if (imageRect.width <= 0 || imageRect.height <= 0) return null
        // pivot = center of the image container
        val pivotX = imgContainerSize.width / 2f
        val pivotY = imgContainerSize.height / 2f
        // undo pan + zoom
        val localX = (screenPos.x - panOffset.x - pivotX) / zoom + pivotX
        val localY = (screenPos.y - panOffset.y - pivotY) / zoom + pivotY
        // convert to normalized image coordinates
        val relX = (localX - imageRect.x) / imageRect.width
        val relY = (localY - imageRect.y) / imageRect.height
        if (relX !in 0f..1f || relY !in 0f..1f) return null
        return (relX * maskW).roundToInt().coerceIn(0, maskW - 1) to
               (relY * maskH).roundToInt().coerceIn(0, maskH - 1)
    }

    fun paintAt(screenPos: Offset) {
        val (cx, cy) = screenToMask(screenPos) ?: return
        val br = ((brushRadius / (imageRect.width * zoom)) * maskW).roundToInt().coerceAtLeast(2)
        val fill = if (brushMode == BrushMode.ADD) 1 else 0
        for (dy in -br..br) {
            val ny = cy + dy; if (ny !in 0..<maskH) continue
            for (dx in -br..br) {
                if (dx * dx + dy * dy > br * br) continue
                val nx = cx + dx; if (nx !in 0..<maskW) continue
                maskAlpha[ny * maskW + nx] = fill
            }
        }
    }

    fun clampPan(offset: Offset, currentZoom: Float): Offset {
        val maxX = (imageRect.width * (currentZoom - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (imageRect.height * (currentZoom - 1f) / 2f).coerceAtLeast(0f)
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    // ── Magic Wand: color-connected flood fill ────────────────────────────────
    fun floodFillAt(screenPos: Offset, addMode: Boolean) {
        val (cx, cy) = screenToMask(screenPos) ?: return
        val targetVal = if (addMode) 0 else 1  // we change FROM this
        val fillVal   = if (addMode) 1 else 0  // TO this
        if (maskAlpha[cy * maskW + cx] == fillVal) return  // already desired

        val ref   = scaledSourcePixels[cy * maskW + cx]
        val refR  = (ref shr 16) and 0xFF
        val refG  = (ref shr 8) and 0xFF
        val refB  = ref and 0xFF
        val tolSq = (wandTolerance * 255f).let { it * it }.toInt()

        val queue   = ArrayDeque<Int>()
        val visited = BooleanArray(maskW * maskH)
        val start   = cy * maskW + cx
        queue.add(start); visited[start] = true

        val dx4 = intArrayOf(0, 0, -1, 1)
        val dy4 = intArrayOf(-1, 1, 0, 0)

        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            maskAlpha[idx] = fillVal
            val y = idx / maskW; val x = idx % maskW
            for (d in 0..3) {
                val nx = x + dx4[d]; val ny = y + dy4[d]
                if (nx !in 0 until maskW || ny !in 0 until maskH) continue
                val nIdx = ny * maskW + nx
                if (visited[nIdx] || maskAlpha[nIdx] != targetVal) continue
                val p  = scaledSourcePixels[nIdx]
                val dr = ((p shr 16) and 0xFF) - refR
                val dg = ((p shr 8)  and 0xFF) - refG
                val db = (p and 0xFF)           - refB
                if (dr * dr + dg * dg + db * db <= tolSq) { visited[nIdx] = true; queue.add(nIdx) }
            }
        }
    }

    fun growMask() {
        val result = maskAlpha.copyOf()
        val dx4 = intArrayOf(0, 0, -1, 1); val dy4 = intArrayOf(-1, 1, 0, 0)
        for (y in 0 until maskH) for (x in 0 until maskW) {
            if (maskAlpha[y * maskW + x] != 0) continue
            for (d in 0..3) {
                val nx = x + dx4[d]; val ny = y + dy4[d]
                if (nx in 0 until maskW && ny in 0 until maskH && maskAlpha[ny * maskW + nx] == 1) {
                    result[y * maskW + x] = 1; break
                }
            }
        }
        result.copyInto(maskAlpha)
    }

    fun shrinkMask() {
        val result = maskAlpha.copyOf()
        val dx4 = intArrayOf(0, 0, -1, 1); val dy4 = intArrayOf(-1, 1, 0, 0)
        for (y in 0 until maskH) for (x in 0 until maskW) {
            if (maskAlpha[y * maskW + x] != 1) continue
            for (d in 0..3) {
                val nx = x + dx4[d]; val ny = y + dy4[d]
                if (nx in 0 until maskW && ny in 0 until maskH && maskAlpha[ny * maskW + nx] == 0) {
                    result[y * maskW + x] = 0; break
                }
            }
        }
        result.copyInto(maskAlpha)
    }

    // ── Smooth: morphological close (dilate then erode) — fills small holes ───
    fun smoothMask() { growMask(); growMask(); shrinkMask(); shrinkMask() }

    // ── combined gesture: 1-finger = brush, 2-finger = zoom+pan ─────────────
    //
    // Key invariant: we NEVER paint until we have seen ≥2 consecutive pointer
    // events with exactly 1 active pointer.  This gives the second finger of a
    // pinch time to land (which is always ≤1 event after the first finger) and
    // set wasZooming=true before any mask modification occurs.
    val gestureModifier = Modifier.pointerInput(brushMode, imageRect, brushRadius) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            cursorPos = down.position

            // wasZooming: once true, the ENTIRE gesture is zoom-only (no paint ever)
            var wasZooming = false
            var isBrushing = false
            var undoSaved = false
            // Number of consecutive pointer-events with exactly 1 active pointer.
            // We commit a brush stroke only after this reaches ≥2.
            var singleFingerEventCount = 0

            var prevDist = -1f
            var prevMid = Offset.Zero

            while (true) {
                val event = awaitPointerEvent()
                val active = event.changes.filter { it.pressed }
                if (active.isEmpty()) break

                if (active.size >= 2) {
                    // ── Zoom / pan mode ──────────────────────────────────────
                    wasZooming = true
                    isBrushing = false
                    singleFingerEventCount = 0
                    cursorPos = null

                    val p1 = active[0].position
                    val p2 = active[1].position
                    val dist = hypot(p2.x - p1.x, p2.y - p1.y)
                    val mid  = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                    if (prevDist > 0f) {
                        val newZoom  = (zoom * (dist / prevDist)).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        panOffset    = clampPan(panOffset + (mid - prevMid), newZoom)
                        zoom         = newZoom
                    }
                    prevDist = dist; prevMid = mid
                    active.forEach { it.consume() }

                } else if (!wasZooming) {
                    // ── Potential brush stroke / wand tap ─────────────────────
                    prevDist = -1f
                    val pos = active[0].position
                    cursorPos = if (brushMode != BrushMode.WAND) pos else null
                    singleFingerEventCount++

                    if (brushMode == BrushMode.WAND) {
                        // Wand: fire on the very first confirmed single-finger event
                        if (singleFingerEventCount >= 1 && !undoSaved) {
                            if (undoStack.size >= MAX_UNDO) undoStack.removeFirst()
                            undoStack.addLast(maskAlpha.copyOf())
                            undoSaved = true
                            isProcessingWand = true
                            scope.launch(Dispatchers.Default) {
                                floodFillAt(pos, brushMode == BrushMode.ADD)
                                isProcessingWand = false
                                redrawTrigger++
                            }
                        }
                        active[0].consume()
                    } else {
                        // Confirm single-finger intent only after 2 consecutive events.
                        if (singleFingerEventCount >= 2 && !undoSaved) {
                            if (undoStack.size >= MAX_UNDO) undoStack.removeFirst()
                            undoStack.addLast(maskAlpha.copyOf())
                            undoSaved  = true
                            isBrushing = true
                            hasInteracted = true
                            paintAt(down.position)
                        }
                        if (isBrushing) {
                            paintAt(pos)
                            active[0].consume()
                            scheduleRedraw()
                        }
                    }
                }
            }
            cursorPos = null
            if (isBrushing) redrawTrigger++ // final flush after drag ends
        }
    }

    // ── root layout ───────────────────────────────────────────────────────────
    val imageAreaContent: @Composable (Modifier) -> Unit = { mod ->
        Box(
            modifier = mod
                .background(ComposeColor.Black)
                .onSizeChanged { imgContainerSize = it }
                .then(gestureModifier)
        ) {
            // Zoomed image
            Image(
                bitmap = originalBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom; scaleY = zoom
                        translationX = panOffset.x; translationY = panOffset.y
                    }
            )
            // Mask overlay (same zoom/pan as image)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom; scaleY = zoom
                        translationX = panOffset.x; translationY = panOffset.y
                    }
            ) {
                if (imageRect.width <= 0 || imageRect.height <= 0) return@Canvas
                drawImage(
                    image = overlayBitmap.asImageBitmap(),
                    dstOffset = IntOffset(imageRect.x.toInt(), imageRect.y.toInt()),
                    dstSize = IntSize(imageRect.width.toInt(), imageRect.height.toInt())
                )
                // Glowing frame with marching-ants dashes
                val gc = ComposeColor(0xFF00E5FF)
                // 1. Outer soft glow
                drawRect(
                    gc.copy(alpha = glowAlpha * 0.18f),
                    Offset(imageRect.x - 8f, imageRect.y - 8f),
                    Size(imageRect.width + 16f, imageRect.height + 16f),
                    style = Stroke(width = 16f)
                )
                // 2. Marching-ants dashed border — stops once user interacts
                if (!hasInteracted) {
                    val dashPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(imageRect.x, imageRect.y)
                        lineTo(imageRect.x + imageRect.width, imageRect.y)
                        lineTo(imageRect.x + imageRect.width, imageRect.y + imageRect.height)
                        lineTo(imageRect.x, imageRect.y + imageRect.height)
                        close()
                    }
                    drawPath(
                        path = dashPath,
                        color = gc.copy(alpha = glowAlpha),
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(14f, 8f), marchPhase
                            )
                        )
                    )
                } else {
                    // Static solid border after user takes over
                    drawRect(gc.copy(alpha = glowAlpha * 0.7f),
                        Offset(imageRect.x, imageRect.y), Size(imageRect.width, imageRect.height), style = Stroke(2f))
                }
                // 3. Sweep scan-line (one-shot, disappears after first pass)
                val sf = sweepFraction.value
                if (sf < 1f) {
                    val sweepX = imageRect.x + sf * imageRect.width
                    drawLine(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(ComposeColor.Transparent, gc.copy(alpha = 0.6f),
                                           gc.copy(alpha = 0.8f), gc.copy(alpha = 0.6f),
                                           ComposeColor.Transparent),
                            startY = imageRect.y, endY = imageRect.y + imageRect.height
                        ),
                        start = Offset(sweepX, imageRect.y), end = Offset(sweepX, imageRect.y + imageRect.height),
                        strokeWidth = 3f
                    )
                }
            }
            // Brush cursor (screen-space, NOT zoomed)
            Canvas(modifier = Modifier.fillMaxSize()) {
                cursorPos?.let { pos ->
                    val rc = if (brushMode == BrushMode.ADD) ComposeColor(0xFF00E5FF) else ComposeColor(0xFFFF4444)
                    drawCircle(rc.copy(alpha = 0.2f), brushRadius + 5f, pos, style = Stroke(7f))
                    drawCircle(rc, brushRadius, pos, style = Stroke(2f))
                    drawCircle(ComposeColor.White.copy(alpha = 0.9f), 3f, pos)
                }
            }
        }
    }

    val controlPanel: @Composable (Modifier) -> Unit = { mod ->
        Column(
            modifier = mod
                .background(ComposeColor(0xF0050514))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isLandscape) {
                Box(Modifier.align(Alignment.CenterHorizontally)
                    .size(36.dp, 4.dp).clip(CircleShape)
                    .background(ComposeColor.White.copy(alpha = 0.25f)))
            }
            // Title + Undo
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Refine Selection", style = MaterialTheme.typography.titleSmall,
                    color = ComposeColor.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${(zoom * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = ComposeColor(0xFF00E5FF))
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { zoom = 1f; panOffset = Offset.Zero },
                        enabled = zoom != 1f) {
                        Text("⟳", color = if (zoom != 1f) ComposeColor(0xFF00E5FF)
                            else ComposeColor.White.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.labelLarge)
                    }
                    IconButton(onClick = {
                        if (undoStack.isNotEmpty()) {
                            undoStack.removeLast().copyInto(maskAlpha); redrawTrigger++
                        }
                    }, enabled = undoStack.isNotEmpty()) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo, "Undo",
                            tint = if (undoStack.isNotEmpty()) ComposeColor(0xFF00E5FF)
                                   else ComposeColor.White.copy(alpha = 0.3f))
                    }
                }
            }
            // Mode chips (row 1: Add | Erase | Wand)
            Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                StickerModeChip("Add", Icons.Default.Add, brushMode == BrushMode.ADD) {
                    brushMode = BrushMode.ADD }
                Spacer(Modifier.width(10.dp))
                StickerModeChip("Erase", Icons.Default.Remove, brushMode == BrushMode.REMOVE) {
                    brushMode = BrushMode.REMOVE }
                Spacer(Modifier.width(10.dp))
                StickerModeChip("Wand", Icons.Default.AutoFixHigh, brushMode == BrushMode.WAND) {
                    brushMode = BrushMode.WAND }
            }
            // Brush size (hidden in Wand mode) / Tolerance (shown in Wand mode)
            if (brushMode == BrushMode.WAND) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tol.", color = ComposeColor.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(30.dp))
                    Slider(value = wandTolerance, onValueChange = { wandTolerance = it },
                        valueRange = 0.05f..0.6f, modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = ComposeColor(0xFFFFD700),
                            activeTrackColor = ComposeColor(0xFFFFD700),
                            inactiveTrackColor = ComposeColor.White.copy(alpha = 0.12f)))
                    Text("${(wandTolerance * 100).roundToInt()}%",
                        color = ComposeColor(0xFFFFD700),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(32.dp).padding(start = 6.dp))
                }
                // Processing indicator
                if (isProcessingWand) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = ComposeColor(0xFFFFD700)
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Size", color = ComposeColor.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(30.dp))
                    Slider(value = brushRadius, onValueChange = { brushRadius = it },
                        valueRange = 6f..72f, modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = ComposeColor(0xFF00E5FF),
                            activeTrackColor = ComposeColor(0xFF00E5FF),
                            inactiveTrackColor = ComposeColor.White.copy(alpha = 0.12f)))
                    val dotDp = ((brushRadius / 72f) * 22f + 6f).dp
                    Box(Modifier.padding(start = 8.dp).size(dotDp).clip(CircleShape)
                        .background(if (brushMode == BrushMode.ADD) ComposeColor(0x8800E5FF)
                                    else ComposeColor(0x88FF4444))
                        .border(1.dp, ComposeColor.White.copy(alpha = 0.3f), CircleShape))
                }
            }
            // Grow / Shrink / Smooth
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                val chipColor = ButtonDefaults.textButtonColors(contentColor = ComposeColor(0xFF00E5FF))
                TextButton(
                    onClick = { hasInteracted = true; undoStack.addLast(maskAlpha.copyOf()); growMask();   redrawTrigger++ },
                    colors = chipColor
                ) { Text("⊕ Grow", style = MaterialTheme.typography.labelMedium) }
                TextButton(
                    onClick = { hasInteracted = true; undoStack.addLast(maskAlpha.copyOf()); shrinkMask(); redrawTrigger++ },
                    colors = chipColor
                ) { Text("⊖ Shrink", style = MaterialTheme.typography.labelMedium) }
                TextButton(
                    onClick = { hasInteracted = true; undoStack.addLast(maskAlpha.copyOf()); smoothMask(); redrawTrigger++ },
                    colors = chipColor
                ) { Text("✦ Smooth", style = MaterialTheme.typography.labelMedium) }
            }
            // Hint
            Text(
                when (brushMode) {
                    BrushMode.WAND   -> "Tap area to auto-select by color"
                    BrushMode.ADD    -> "1-finger paint  •  2-finger zoom/pan"
                    BrushMode.REMOVE -> "1-finger erase  •  2-finger zoom/pan"
                },
                style = MaterialTheme.typography.labelSmall,
                color = ComposeColor.White.copy(alpha = 0.35f),
                modifier = Modifier.align(Alignment.CenterHorizontally))
            // Actions
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = ComposeColor.White.copy(alpha = 0.55f)) }
                Button(onClick = { onConfirm(buildAlpha8Mask(maskAlpha, maskW, maskH)) },
                    colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF00B4D8)),
                    shape = RoundedCornerShape(14.dp)) {
                    Text("✂  Copy Sticker", color = ComposeColor.White) }
            }
        }
    }

    if (isLandscape) {
        Row(Modifier.fillMaxSize().background(ComposeColor.Black)) {
            imageAreaContent(Modifier.weight(1f).fillMaxHeight())
            controlPanel(Modifier.width(300.dp).fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp)))
        }
    } else {
        Column(Modifier.fillMaxSize().background(ComposeColor.Black)) {
            imageAreaContent(Modifier.weight(1f).fillMaxWidth())
            controlPanel(Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StickerModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ComposeColor(0xFF00B4D8) else ComposeColor(0x20FFFFFF))
            .border(if (selected) 0.dp else 1.dp, ComposeColor.White.copy(alpha = 0.15f),
                RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, label, tint = ComposeColor.White, modifier = Modifier.size(15.dp))
        Text(label, color = ComposeColor.White, style = MaterialTheme.typography.labelLarge)
    }
}

private fun buildOverlay(maskAlpha: IntArray, w: Int, h: Int): Bitmap {
    val out = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val idx = y * w + x
            val fg = maskAlpha[idx] > 0
            var edgeFg = false; var edgeBg = false
            for (ky in -1..1) {
                val ny = y + ky; if (ny !in 0..<h) continue
                for (kx in -1..1) {
                    if (ky == 0 && kx == 0) continue
                    val nx = x + kx; if (nx !in 0..<w) continue
                    val nb = maskAlpha[ny * w + nx] > 0
                    if (fg && !nb) edgeFg = true
                    if (!fg && nb) edgeBg = true
                }
            }
            out[idx] = when {
                edgeFg -> 0xFF_00E5FF.toInt()
                edgeBg -> 0x77_FFFFFF
                fg     -> 0x48_00B4D8
                else   -> 0x00_000000
            }
        }
    }
    return createBitmap(w, h).also { it.setPixels(out, 0, w, 0, 0, w, h) }
}

private fun buildAlpha8Mask(maskAlpha: IntArray, w: Int, h: Int): Bitmap {
    val buf = java.nio.ByteBuffer.allocateDirect(w * h)
    maskAlpha.forEach { buf.put(if (it > 0) 255.toByte() else 0.toByte()) }
    buf.rewind()
    return createBitmap(w, h, Bitmap.Config.ALPHA_8).also { it.copyPixelsFromBuffer(buf) }
}

private data class FitRect(val x: Float, val y: Float, val width: Float, val height: Float)

private fun computeFitRect(container: IntSize, imgW: Int, imgH: Int): FitRect {
    if (container.width <= 0 || container.height <= 0 || imgW <= 0 || imgH <= 0)
        return FitRect(0f, 0f, 0f, 0f)
    val scale = min(container.width.toFloat() / imgW, container.height.toFloat() / imgH)
    val dw = imgW * scale; val dh = imgH * scale
    return FitRect((container.width - dw) / 2f, (container.height - dh) / 2f, dw, dh)
}
