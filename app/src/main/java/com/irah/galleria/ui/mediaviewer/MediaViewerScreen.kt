package com.irah.galleria.ui.mediaviewer
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.irah.galleria.domain.model.Media
import com.irah.galleria.ui.common.VideoPlayer
import com.irah.galleria.ui.settings.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    navController: NavController,
    viewModel: MediaViewerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Media deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Media delete failed", Toast.LENGTH_SHORT).show()
        }
    }

    if (!state.isLoading && state.mediaList.isNotEmpty()) {
        val pagerState = rememberPagerState(
            initialPage = state.initialIndex,
            pageCount = { state.mediaList.size }
        )
        val currentMedia = state.mediaList[pagerState.currentPage]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val media = state.mediaList[page]
                Box(modifier = Modifier
                    .fillMaxSize()
                ) {
                    MediaPage(
                        media = media,
                        isSelected = pagerState.currentPage == page,
                        showControls = showControls,
                        onTap = { showControls = !showControls },
                        onVideoControlVisibilityChange = { showControls = it }
                    )
                }
            }
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .systemBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    if (isLandscape) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.toggleFavorite(currentMedia) }) {
                                Icon(
                                    if (currentMedia.isFavorite) Icons.Default.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (currentMedia.isFavorite) Color.Red else Color.White
                                )
                            }
                            IconButton(onClick = { showInfoSheet = true }) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                            }
                            IconButton(onClick = {
                                if (currentMedia.isVideo) {
                                    try {
                                        val editIntent = android.content.Intent(android.content.Intent.ACTION_EDIT).apply {
                                            setDataAndType(currentMedia.uri.toUri(), currentMedia.mimeType)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(editIntent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "No video editor app found", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    navController.navigate(
                                        com.irah.galleria.ui.navigation.Screen.Editor.route + "/${currentMedia.id}"
                                    )
                                }
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                            }
                            IconButton(onClick = {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = currentMedia.mimeType
                                    putExtra(android.content.Intent.EXTRA_STREAM, currentMedia.uri.toUri())
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Media"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                            IconButton(onClick = {
                                viewModel.deleteMedia(currentMedia) { intentSender ->
                                    deleteLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        }
                    }
                }
            }

            if (!isLandscape) {
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { viewModel.toggleFavorite(currentMedia) }) {
                            Icon(
                                if (currentMedia.isFavorite) Icons.Default.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (currentMedia.isFavorite) Color.Red else Color.White
                            )
                        }
                        IconButton(onClick = {
                            showInfoSheet = true
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                        }
                        IconButton(onClick = {
                            if (currentMedia.isVideo) {
                                try {
                                    val editIntent = android.content.Intent(android.content.Intent.ACTION_EDIT).apply {
                                        setDataAndType(currentMedia.uri.toUri(), currentMedia.mimeType)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(editIntent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No video editor app found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                navController.navigate(
                                    com.irah.galleria.ui.navigation.Screen.Editor.route + "/${currentMedia.id}"
                                )
                            }
                        }) {
                            Icon(Icons.Default.Tune, contentDescription = "Edit", tint = Color.White)
                        }
                        IconButton(onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = currentMedia.mimeType
                                putExtra(android.content.Intent.EXTRA_STREAM, currentMedia.uri.toUri())
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Media"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = {
                            viewModel.deleteMedia(currentMedia) { intentSender ->
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                }
            }
        }
        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                MediaInfoContent(media = currentMedia)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LinearProgressIndicator()
        }
    }
}
@Composable
fun MediaPage(
    media: Media,
    isSelected: Boolean,
    showControls: Boolean,
    onTap: () -> Unit,
    onVideoControlVisibilityChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (media.isVideo) {
            if (isSelected) {
                VideoPlayer(
                    uri = media.uri.toUri(),
                    isSelected = true,
                    controllerVisible = showControls,
                    onControllerVisibilityChanged = onVideoControlVisibilityChange
                )
            } else {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(media.uri)
                        .crossfade(true)
                        .build()
                )
                com.irah.galleria.ui.common.ZoomableImage(
                    painter = painter,
                    contentDescription = media.name,
                    onTap = onTap
                )
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(64.dp)
                )
            }
        } else {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(media.uri)
                    .crossfade(true)
                    .build()
            )
            com.irah.galleria.ui.common.ZoomableImage(
                painter = painter,
                contentDescription = media.name,
                onTap = onTap
            )
        }
    }
}
@Composable
fun MediaInfoContent(media: Media) {
    Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
        Text("Details", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
        InfoRow(label = "Name", value = media.name)
        InfoRow(label = "Path", value = media.path)
        InfoRow(label = "Size", value = formatSize(media.size))
        InfoRow(label = "Date", value = formatDate(media.dateTaken))
        InfoRow(label = "Resolution", value = "${media.width} x ${media.height}")
    }
}
@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
fun formatSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return if (mb > 1) String.format(Locale.US,"%.2f MB", mb) else String.format(Locale.US,"%.2f KB", kb)
}
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun findActivity(context: android.content.Context): Activity? {
    var ctx = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}