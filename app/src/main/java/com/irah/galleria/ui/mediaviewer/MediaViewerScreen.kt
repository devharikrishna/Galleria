package com.irah.galleria.ui.mediaviewer
import android.app.Activity
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.irah.galleria.domain.model.Media
import com.irah.galleria.ui.common.VideoPlayer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    navController: NavController,
    viewModel: MediaViewerViewModel = hiltViewModel(),
    settingsViewModel: com.irah.galleria.ui.settings.SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settings by settingsViewModel.settings.collectAsState(initial = com.irah.galleria.domain.model.AppSettings())
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    val insetsController = remember(window) { 
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }
    var showControls by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
        }
    }
    LaunchedEffect(settings.maxBrightness) {
        window?.let { win ->
            val params = win.attributes
            params.screenBrightness = if (settings.maxBrightness) 1.0f else android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            win.attributes = params
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            window?.let { win ->
                val params = win.attributes
                params.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = params
            }
        }
    }
    LaunchedEffect(showControls) {
        if (showControls) {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    LaunchedEffect(Unit) {
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
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)  
                        .systemBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            }
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)  
                        .systemBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { 
                        showInfoSheet = true 
                    }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
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
            CircularProgressIndicator()
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
    return if (mb > 1) String.format("%.2f MB", mb) else String.format("%.2f KB", kb)
}
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}