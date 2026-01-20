package com.irah.galleria.ui.common

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.sin

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    controllerVisible: Boolean = true,
    onControllerVisibilityChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            setSeekBackIncrementMs(5000)
            setSeekForwardIncrementMs(5000)
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    
    // Speed Control
    var speed by remember { mutableFloatStateOf(1f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    
    // Audio Control
    var isMuted by remember { mutableStateOf(false) }

    // Smooth Progress for UI
    val animatedProgress by animateFloatAsState(
        targetValue = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "Progress"
    )

    LaunchedEffect(Unit) {
        while(true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            isPlaying = exoPlayer.isPlaying
            playbackState = exoPlayer.playbackState
            delay(200) // Faster polling for smoother updates (UI interpolation handles the rest)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_IDLE || state == Player.STATE_ENDED) {
                    onControllerVisibilityChanged(true)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    
    LaunchedEffect(speed) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }
    
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Lifecycle Management
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (isSelected) {
                    exoPlayer.play()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    // Play/Pause based on Selection (Swipe)
    LaunchedEffect(isSelected) {
        if (isSelected) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Auto-hide Controls
    LaunchedEffect(controllerVisible, isPlaying) {
        if (controllerVisible && isPlaying) {
            delay(3000)
            onControllerVisibilityChanged(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onControllerVisibilityChanged(!controllerVisible)
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = controllerVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Bottom Controls Container
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 80.dp) // Lift up to avoid system bars/other buttons
                ) {
                    // Time Labels
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDuration(currentPosition), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text(formatDuration(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }

                    // Wavy Progress Bar
                    WavyProgressIndicator(
                        progress = animatedProgress,
                        isPlaying = isPlaying,
                        onSeek = { fraction ->
                            val seekPos = (fraction * duration).toLong()
                            exoPlayer.seekTo(seekPos)
                            currentPosition = seekPos
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly, // Distribute evenly
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Speed
                        Box {
                            IconButton(onClick = { showSpeedMenu = true }) {
                                Icon(Icons.Default.Speed, "Speed", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s}x") },
                                        onClick = { 
                                            speed = s
                                            showSpeedMenu = false 
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Rewind
                        IconButton(onClick = { exoPlayer.seekBack() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.KeyboardDoubleArrowLeft, "Rewind", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        // 3. Play/Pause
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .clickable {
                                    if (playbackState == Player.STATE_ENDED) {
                                        exoPlayer.seekTo(0)
                                        exoPlayer.play()
                                    } else if (isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying && playbackState != Player.STATE_ENDED) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        // 4. Forward
                        IconButton(onClick = { exoPlayer.seekForward() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.KeyboardDoubleArrowRight, "Forward", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        
                        // 5. Mute
                        IconButton(onClick = { isMuted = !isMuted }) {
                            Icon(
                                if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                "Audio", 
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun WavyProgressIndicator(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit
) {
    // Infinite transition for wave phase (ripple effect)
    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )
    
    // Only animate phase if playing
    val currentPhase = if (isPlaying) phase else 0f
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val midY = size.height / 2
        val amplitude = 6.dp.toPx()
        val frequency = 2 * Math.PI / 50.dp.toPx() // One wave every 50dp
        
        // Draw Track (Inactive)
        val trackPath = Path().apply {
            moveTo(0f, midY)
            var x = 0f
            while (x <= size.width) {
                // Shift phase slightly for dynamic look or keep static? User asked for buttery smooth.
                // Let's animate inactive track too for cool effect? OR just static.
                // Let's keep inactive static or slow shift.
                val y = midY + amplitude * sin((x * frequency + currentPhase).toFloat())
                if (x == 0f) moveTo(x, y) else lineTo(x, y)
                x += 2
            }
        }
        drawPath(
            path = trackPath,
            color = Color.White.copy(alpha = 0.3f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Progress (Active)
        val progressX = progress * size.width
        
        val activePath = Path().apply {
            var x = 0f
            var first = true
            while (x <= progressX) {
                 val y = midY + amplitude * sin((x * frequency + currentPhase).toFloat()) // Match phase
                 if (first) {
                     moveTo(x, y)
                     first = false
                 } else {
                     lineTo(x, y)
                 }
                 x += 2
            }
        }
        
        drawPath(
            path = activePath,
            color = Color.White,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Draw Knob
        // Knob position calculates Y based on sine wave at current X
        val knobY = midY + amplitude * sin((progressX * frequency + currentPhase).toFloat())
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(progressX, knobY)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
