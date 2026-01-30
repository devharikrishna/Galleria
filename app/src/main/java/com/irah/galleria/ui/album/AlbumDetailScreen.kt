package com.irah.galleria.ui.album
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.imageLoader
import com.irah.galleria.ui.gallery.components.AlbumSelectionSheet
import com.irah.galleria.ui.navigation.Screen
import com.irah.galleria.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    navController: NavController,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settings by settingsViewModel.settings.collectAsState(initial = com.irah.galleria.domain.model.AppSettings())
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
             val action = pendingAction
             pendingAction = null
             if (action != null) {
                 action.invoke()
             } else {
                 viewModel.onEvent(AlbumDetailEvent.ClearSelection)
             }
        } else {
            pendingAction = null
        }
    }
    val performDelete = {
        viewModel.deleteSelectedMedia { intentSender ->
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                 pendingAction = { 
                    viewModel.deleteSelectedMedia { sender ->
                        permissionLauncher.launch(
                            IntentSenderRequest.Builder(sender).build()
                        )
                    }
                 }
            }
            permissionLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
        }
    }
    var showAlbumSelectionSheet by remember { mutableStateOf(false) }
    var isCopyOperation by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    val albums by viewModel.albums.collectAsState(initial = emptyList())
    androidx.activity.compose.BackHandler(enabled = state.isSelectionMode) {
        viewModel.onEvent(AlbumDetailEvent.ClearSelection)
    }
    val uiMode = com.irah.galleria.ui.theme.LocalUiMode.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AlbumDetailUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    com.irah.galleria.ui.theme.GlassScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val topBarColors = if (uiMode == com.irah.galleria.domain.model.UiMode.LIQUID_GLASS) {
                androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            } else {
                 androidx.compose.material3.TopAppBarDefaults.topAppBarColors()
            }
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedMediaIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(AlbumDetailEvent.ClearSelection) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear Selection")
                        }
                    },
                    colors = topBarColors,
                    actions = {
                        if (state.isTrash) {
                             IconButton(onClick = { 
                                 viewModel.restoreSelectedMedia { intentSender ->
                                     permissionLauncher.launch(
                                         IntentSenderRequest.Builder(intentSender).build()
                                     )
                                 }
                             }) {
                                 Icon(Icons.Filled.Refresh, contentDescription = "Restore")
                             }
                        } else {
                            IconButton(onClick = { viewModel.favoriteSelectedMedia() }) {
                                Icon(Icons.Filled.FavoriteBorder, contentDescription = "Favorite")
                            }
                        }

                        IconButton(onClick = { 
                            isCopyOperation = false
                            showAlbumSelectionSheet = true 
                        }) {
                            Icon(Icons.Filled.Folder, contentDescription = "Move to Album")
                        }
                        IconButton(onClick = { 
                            isCopyOperation = true
                            showAlbumSelectionSheet = true 
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy to Album")
                        }
                        IconButton(onClick = { viewModel.shareSelectedMedia(navController.context) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { 
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                performDelete()
                            } else {
                                showDeleteDialog = true 
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(state.albumName) },
                    colors = topBarColors,
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(if (settings.trashEnabled) "Move to Recycle Bin" else "Delete Permanently") },
                    text = { 
                        Text(if (settings.trashEnabled) 
                            "Are you sure you want to move file(s) to recycler bin?" 
                            else "Are you sure you want to permanently delete selected item(s)") 
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            performDelete()
                        }) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            if (showAlbumSelectionSheet) {
                AlbumSelectionSheet(
                    albums = albums,
                    onAlbumSelected = { album ->
                        showAlbumSelectionSheet = false
                        val target = album.relativePath ?: "Pictures/${album.name}"
                        if (isCopyOperation) {
                            viewModel.copySelectedMedia(target)
                        } else {
                            fun performMove() {
                                viewModel.moveSelectedMedia(target) { intentSender ->
                                    pendingAction = { performMove() }
                                    permissionLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                            }
                            performMove()
                        }
                    },
                    onCreateNewAlbum = {
                        showAlbumSelectionSheet = false
                        showCreateAlbumDialog = true
                    },
                    onDismissRequest = { showAlbumSelectionSheet = false },
                    isCopyOperation = isCopyOperation
                )
            }
            if (showCreateAlbumDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateAlbumDialog = false },
                    title = { Text("New Album") },
                    text = { 
                        OutlinedTextField(
                            value = newAlbumName,
                            onValueChange = { newAlbumName = it },
                            label = { Text("Album Name") }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newAlbumName.isNotBlank()) {
                                showCreateAlbumDialog = false
                                val target = "Pictures/$newAlbumName"
                                if (isCopyOperation) {
                                     viewModel.copySelectedMedia(target)
                                } else {
                                    fun performMove() {
                                        viewModel.moveSelectedMedia(target) { intentSender ->
                                             pendingAction = { performMove() }
                                             permissionLauncher.launch(
                                                IntentSenderRequest.Builder(intentSender).build()
                                             )
                                        }
                                    }
                                    performMove()
                                }
                            }
                        }) {
                            Text(if (isCopyOperation) "Create & Copy" else "Create & Move")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateAlbumDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            if (state.isLoading) {
                androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
            } else if (state.media.isEmpty()) {
                val emptyMessage = when (state.albumId) {
                    -2L -> "No favorites added"
                    -3L -> "No screenshots found"
                    else -> "Album is empty"
                }
                Text(
                    text = emptyMessage,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                )
            } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            val staggeredGridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()
            val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            val itemSizePx = remember(screenWidth, settings.albumDetailGridCount) {
                ((screenWidth / settings.albumDetailGridCount) * density).toInt()
            }

            com.irah.galleria.ui.album.components.AlbumDetailGridContent(
                media = state.media,
                selectedIds = state.selectedMediaIds,
                isSelectionMode = state.isSelectionMode,
                settings = settings,
                gridState = gridState,
                staggeredGridState = staggeredGridState,
                onMediaClick = { media ->
                     navController.navigate(
                         Screen.MediaViewer.route + "/${media.id}?albumId=${state.albumId}"
                     )
                },
                onSelectionChange = { ids ->
                    viewModel.onEvent(AlbumDetailEvent.UpdateSelection(ids))
                },
                onToggleSelection = { id ->
                    viewModel.onEvent(AlbumDetailEvent.ToggleSelection(id))
                },
                imageLoader = context.imageLoader,
                itemSizePx = itemSizePx
            )
        }
    }
}
}

