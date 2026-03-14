package com.irah.galleria.ui.story

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.irah.galleria.domain.model.Media
import com.irah.galleria.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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

    val storyDuration = 5000L

    LaunchedEffect(currentIndex, isPaused, showInfoSheet) {
        if (!isPaused && !showInfoSheet) {
            progress = 0f
            val steps = 100
            val delayTime = storyDuration / steps
            for (i in 0..steps) {
                progress = i / steps.toFloat()
                delay(delayTime)
            }
            if (currentIndex < mediaList.size - 1) {
                currentIndex++
            } else {
                navController.popBackStack()
            }
        }
    }

    var exifAspectRatio by remember(currentMedia.id) { mutableStateOf<Float?>(null) }
    LaunchedEffect(currentMedia.id) {
        exifAspectRatio = withContext(Dispatchers.IO) {
            try {
                val exif = androidx.exifinterface.media.ExifInterface(currentMedia.path)
                val orientation = exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                )
                val isRotated = orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 ||
                    orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE ||
                    orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE
                val w = currentMedia.width
                val h = currentMedia.height
                if (w > 0 && h > 0) {
                    if (isRotated) h.toFloat() / w.toFloat()  // swap for 90°/270°
                    else w.toFloat() / h.toFloat()
                } else null
            } catch (_: Exception) { null }
        }
    }

    // Always force white status bar icons — the background is always dark here.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = com.irah.galleria.ui.util.findActivity(context)?.window
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val wasLight = insetsController?.isAppearanceLightStatusBars ?: true
        insetsController?.isAppearanceLightStatusBars = false
        onDispose { insetsController?.isAppearanceLightStatusBars = wasLight }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val cardPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(currentMedia.uri)
            .crossfade(true)
            .build()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentMedia) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        if (scale == 1f) {
                            val width = size.width
                            if (tapOffset.x < width / 3) {
                                if (currentIndex > 0) {
                                    currentIndex--
                                    progress = 0f
                                }
                            } else {
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
                        val extraWidth = (scale - 1) * size.width
                        val extraHeight = (scale - 1) * size.height
                        val maxX = extraWidth / 2
                        val maxY = extraHeight / 2
                        offset = Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                    }
                }
            }
    ) {
        // ── Layer 1: Blurred background (full-screen, cropped, heavily blurred) ──
        Crossfade(targetState = currentMedia, animationSpec = tween(700), label = "backgroundCrossfade") { media ->
            val bgPainter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(media.uri)
                    .size(400)
                    .crossfade(true)
                    .build()
            )
            Image(
                painter = bgPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 20.dp)
                    .graphicsLayer { alpha = 0.85f }
            )
        }

        // Darkening overlay to desaturate-blur further and ensure chrome contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
        )

        // key() forces Compose to fully recreate the Card and Image whenever the
        // displayed image changes, clearing any stale layout/painter state.
        key(currentMedia.id) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Image(
                    painter = cardPainter,
                    contentDescription = currentMedia.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (exifAspectRatio != null)
                                Modifier.aspectRatio(exifAspectRatio!!)
                            else
                                Modifier
                        )
                        .clip(RoundedCornerShape(20.dp))
                )
            }
        }

        // ── Layer 3: UI chrome (hidden when zoomed) ───────────────────────────
        AnimatedVisibility(
            visible = scale == 1f,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Top gradient scrim — ensures progress bars & date are readable
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                )

                // Story progress bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp, start = 10.dp, end = 10.dp)
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
                                .height(2.5.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f),
                        )
                    }
                }

                // Date pill below progress bars
                val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                val dateTakenMs = if (currentMedia.dateTaken > 0)
                    currentMedia.dateTaken
                else
                    currentMedia.timestamp * 1000L
                val dateString = dateFormat.format(java.util.Date(dateTakenMs))

                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 26.dp)
                        .background(
                            Color.Black.copy(alpha = 0.45f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                )

                // Bottom gradient scrim — ensures action icons are readable
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                )

                // Bottom action bar (favourite, info, edit, share, delete)
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
                            navController.navigate(Screen.Editor.route + "/${currentMedia.id}")
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
                            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }
                    },
                    // scrim is already drawn above — no need for double gradient
                    useGradient = false,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }
        }

        // Info bottom sheet
        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = androidx.compose.material3.rememberModalBottomSheetState()
            ) {
                com.irah.galleria.ui.mediaviewer.MediaInfoContent(media = currentMedia)
            }
        }
    }
}
