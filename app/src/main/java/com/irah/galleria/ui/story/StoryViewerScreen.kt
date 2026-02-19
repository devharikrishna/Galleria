package com.irah.galleria.ui.story

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.irah.galleria.domain.model.Media
import com.irah.galleria.ui.navigation.Screen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    navController: NavController,
    mediaList: List<Media>,
    startIndex: Int,
    viewModel: StoryViewModel = hiltViewModel()
) {
    if (mediaList.isEmpty()) {
        navController.popBackStack()
        return
    }

    var currentIndex by remember { mutableIntStateOf(startIndex) }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    
    val currentMedia = mediaList.getOrElse(currentIndex) { mediaList[0] }

    val context = LocalContext.current
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
             viewModel.onMediaDeleted(currentMedia.id)
        } else {
             Toast.makeText(context, "Deletion cancelled", Toast.LENGTH_SHORT).show()
        }
    }


    var showInfoSheet by remember { mutableStateOf(false) }

    val storyDuration = 5000L // 5 seconds per slide

    LaunchedEffect(currentIndex, isPaused, showInfoSheet) {
        if (!isPaused && !showInfoSheet) {
            progress = 0f
            val steps = 100
            val delayTime = storyDuration / steps
            for (i in 0..steps) {
                progress = i / steps.toFloat()
                delay(delayTime)
            }
            // Advance to next
            if (currentIndex < mediaList.size - 1) {
                currentIndex++
            } else {
                navController.popBackStack()
            }
        }
    }

    com.irah.galleria.ui.util.ForceSystemNavigationColor(Color.Black)
    
    // Media Content & Gestures
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Zoomable Image
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(currentMedia.uri)
                .crossfade(true)
                .build()
        )
        
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                // We need to detect gestures *before* ZoomableImage for simple taps/holds
                // BUT pointerInput consumes... 
                // Let's put gesture detector on TOP but use `awaitPointerEvent` or pass through?
                // Compose gesture handling is tricky for "Pinch overrides Click".
                .pointerInput(currentMedia) {
                    // Detect taps and long presses only when not zoomed
                    detectTapGestures(
                        onTap = { tapOffset ->
                             if (scale == 1f) {
                                val width = size.width
                                if (tapOffset.x < width / 3) {
                                    // Previous
                                    if (currentIndex > 0) {
                                        currentIndex--
                                        progress = 0f
                                    }
                                } else {
                                    // Next
                                    if (currentIndex < mediaList.size - 1) {
                                        currentIndex++
                                        progress = 0f
                                    } else {
                                        navController.popBackStack()
                                    }
                                }
                             }
                        },
                        onLongPress = {
                            if (scale == 1f) isPaused = true
                        },
                        onPress = {
                            if (scale == 1f) {
                                tryAwaitRelease()
                                isPaused = false
                            }
                        }
                    )
                }
                .pointerInput(currentMedia) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 3f)
                        if (scale == 1f) {
                            offset = Offset.Zero
                        } else {
                            // Allow panning when zoomed
                            val extraWidth = (scale - 1) * size.width
                            val extraHeight = (scale - 1) * size.height
                            val maxX = extraWidth / 2
                            val maxY = extraHeight / 2
                            
                            val x = offset.x + pan.x
                            val y = offset.y + pan.y
                            
                            offset = Offset(
                                x.coerceIn(-maxX, maxX),
                                y.coerceIn(-maxY, maxY)
                            )
                        }
                    }
                }
        ) {
             Image(
                painter = painter,
                contentDescription = null,
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
        
        // Progress Bars (Only show when not zoomed for cleaner look, or always? standard is always/fade)
        // We'll keep them visible but maybe fade out if zoomed? User didn't ask.
        // But for better UX, let's hide controls when zoomed.
        
        AnimatedVisibility(
            visible = scale == 1f,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
             Box(modifier = Modifier.fillMaxSize()) {
                // Top Shade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                )

                // Progress Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 8.dp, end = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    mediaList.forEachIndexed { index, _ ->
                        val barProgress = when {
                            index < currentIndex -> 1f
                            index == currentIndex -> progress
                            else -> 0f
                        }
                        
                        LinearProgressIndicator(
                            progress = { barProgress },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .height(2.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                    }
                }
                
                // Date Display
                val dateFormat = java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.getDefault())
                val dateString = dateFormat.format(java.util.Date(currentMedia.dateTaken))
                
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp) // Below progress bars
                        .background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )


        // Bottom Bar
        com.irah.galleria.ui.common.MediaBottomBar(
            isFavorite = currentMedia.isFavorite,
            onFavoriteClick = { viewModel.toggleFavorite(currentMedia) },
            onInfoClick = { showInfoSheet = true },
            onEditClick = {
                isPaused = true
                if (currentMedia.isVideo) {
                    try {
                        val editIntent = android.content.Intent(android.content.Intent.ACTION_EDIT).apply {
                            setDataAndType(currentMedia.uri.toUri(), currentMedia.mimeType)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(editIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "No video editor app found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    navController.navigate(
                        Screen.Editor.route + "/${currentMedia.id}"
                    )
                }
            },
            onShareClick = {
                isPaused = true
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = currentMedia.mimeType
                    putExtra(android.content.Intent.EXTRA_STREAM, currentMedia.uri.toUri())
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Media"))
            },
            onDeleteClick = {
                isPaused = true
                viewModel.deleteMedia(currentMedia) { intentSender ->
                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
                 if (showInfoSheet) {
                     ModalBottomSheet(
                         onDismissRequest = { showInfoSheet = false },
                         sheetState = androidx.compose.material3.rememberModalBottomSheetState()
                     ) {
                         com.irah.galleria.ui.mediaviewer.MediaInfoContent(media = currentMedia)
                     }
                 }
        }
}}}

