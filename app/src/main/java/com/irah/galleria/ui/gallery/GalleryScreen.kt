package com.irah.galleria.ui.gallery

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.irah.galleria.domain.usecase.FilterType
import com.irah.galleria.domain.util.MediaOrder
import com.irah.galleria.domain.util.OrderType
import com.irah.galleria.ui.gallery.components.MediaGridItem
import com.irah.galleria.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    viewModel: GalleryViewModel = hiltViewModel(),
    settingsViewModel: com.irah.galleria.ui.settings.SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settings by settingsViewModel.settings.collectAsState(initial = com.irah.galleria.domain.model.AppSettings())

    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    var showMoveSheet by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
             pendingAction?.invoke()
             pendingAction = null
             viewModel.onEvent(GalleryEvent.ClearSelection)
        } else {
            pendingAction = null
        }
    }

    val bottomBarVisibility = com.irah.galleria.ui.LocalBottomBarVisibility.current
    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < -5) { // Scheduling hide
                    bottomBarVisibility.value = false
                } else if (available.y > 5) { // Scheduling show
                    bottomBarVisibility.value = true
                }
                return super.onPreScroll(available, source)
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedMediaIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(GalleryEvent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    actions = {
                        IconButton(onClick = { showMoveSheet = true }) {
                            Icon(Icons.Default.Folder, contentDescription = "Move to Album")
                        }
                        IconButton(onClick = { viewModel.shareSelectedMedia(context) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = {
                            val action = {
                                viewModel.deleteSelectedMedia { intentSender ->
                                    pendingAction = { viewModel.deleteSelectedMedia { } } // Retry action (simplified, ideally re-trigger logic)
                                    // Actually, we need to capture the retry properly.
                                    // A simple way is to define the function locally.
                                }
                            }
                            // Let's use a robust recursive approach
                            fun performDelete() {
                                viewModel.deleteSelectedMedia { intentSender ->
                                    pendingAction = { performDelete() }
                                    permissionLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                            }
                            performDelete()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            } else {
                TopAppBar( // ... existing top bar code ...
                    title = { Text("Gallery") },
                    actions = {
                        // Filter Action
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All") },
                                onClick = {
                                    viewModel.onEvent(GalleryEvent.FilterChange(FilterType.All))
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Images Only") },
                                onClick = {
                                    viewModel.onEvent(GalleryEvent.FilterChange(FilterType.Images))
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Videos Only") },
                                onClick = {
                                    viewModel.onEvent(GalleryEvent.FilterChange(FilterType.Videos))
                                    showFilterMenu = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Recycle Bin") },
                                onClick = {
                                    showFilterMenu = false
                                    navController.navigate(Screen.RecycleBin.route)
                                }
                            )
                        }

                        // Sort Action
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Date (Newest)") },
                                onClick = {
                                    viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Date(OrderType.Descending)))
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Oldest)") },
                                onClick = {
                                    viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Date(OrderType.Ascending)))
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = {
                                    viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Name(OrderType.Ascending)))
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)") },
                                onClick = {
                                    viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Name(OrderType.Descending)))
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size (Largest)") },
                                onClick = {
                                    viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Size(OrderType.Descending)))
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size (Smallest)") },
                                onClick = {
                                    viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Size(OrderType.Ascending)))
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showMoveSheet) {
                com.irah.galleria.ui.gallery.components.AlbumSelectionSheet(
                    albums = state.albums,
                    onAlbumSelected = { album ->
                        showMoveSheet = false
                        val target = album.relativePath ?: "Pictures/${album.name}"
                        
                        fun performMove() {
                            viewModel.moveSelectedMedia(target) { intentSender ->
                                pendingAction = { performMove() }
                                permissionLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                        }
                        performMove()
                    },
                    onCreateNewAlbum = {
                        showMoveSheet = false
                        showCreateAlbumDialog = true
                    },
                    onDismissRequest = { showMoveSheet = false }
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
                                val target = "Pictures/$newAlbumName"
                                fun performMove() {
                                    viewModel.moveSelectedMedia(target) { intentSender ->
                                         pendingAction = { performMove() }
                                         permissionLauncher.launch(
                                            IntentSenderRequest.Builder(intentSender).build()
                                         )
                                    }
                                }
                                performMove()
                                showCreateAlbumDialog = false
                            }
                        }) {
                            Text("Create & Move")
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.media.isEmpty()) {
                Text(
                    "No media found",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val contentPadding = PaddingValues(
                    top = 4.dp, // Removed padding.calculateTopPadding() to fix double gap
                    bottom = padding.calculateBottomPadding() + 80.dp + 4.dp, // Add BottomBar padding + extra
                    start = 4.dp,
                    end = 4.dp
                )

                if (settings.galleryViewType == com.irah.galleria.domain.model.GalleryViewType.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings.galleryGridCount),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(settings.gridSpacing.dp),
                        verticalArrangement = Arrangement.spacedBy(settings.gridSpacing.dp)
                    ) {
                        items(state.media) { media ->
                            val isSelected = state.selectedMediaIds.contains(media.id)
                            MediaGridItem(
                                media = media,
                                isStaggered = false,
                                cornerRadius = settings.galleryCornerRadius,
                                animationsEnabled = settings.animationsEnabled,
                                isSelected = isSelected,
                                onClick = {
                                    if (state.isSelectionMode) {
                                        viewModel.onEvent(GalleryEvent.ToggleSelection(media.id))
                                    } else {
                                        navController.navigate(
                                            Screen.MediaViewer.route + "/${media.id}"
                                        )
                                    }
                                },
                                onLongClick = {
                                    viewModel.onEvent(GalleryEvent.ToggleSelection(media.id))
                                }
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid(
                        columns = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Fixed(settings.galleryGridCount),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(settings.gridSpacing.dp),
                        verticalItemSpacing = settings.gridSpacing.dp
                    ) {
                        items(state.media) { media ->
                            val isSelected = state.selectedMediaIds.contains(media.id)
                            MediaGridItem(
                                media = media,
                                modifier = Modifier.fillMaxWidth(),
                                isStaggered = true,
                                cornerRadius = settings.galleryCornerRadius,
                                animationsEnabled = settings.animationsEnabled,
                                isSelected = isSelected,
                                onClick = {
                                    if (state.isSelectionMode) {
                                        viewModel.onEvent(GalleryEvent.ToggleSelection(media.id))
                                    } else {
                                        navController.navigate(
                                            Screen.MediaViewer.route + "/${media.id}"
                                        )
                                    }
                                },
                                onLongClick = {
                                    viewModel.onEvent(GalleryEvent.ToggleSelection(media.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
