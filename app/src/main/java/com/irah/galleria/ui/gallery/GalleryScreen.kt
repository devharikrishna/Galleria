package com.irah.galleria.ui.gallery
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.imageLoader
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.usecase.FilterType
import com.irah.galleria.domain.util.MediaOrder
import com.irah.galleria.domain.util.OrderType
import com.irah.galleria.ui.gallery.components.MediaGridItem
import com.irah.galleria.ui.navigation.Screen
import com.irah.galleria.ui.gallery.GalleryUiEvent
import com.irah.galleria.domain.model.MediaOperationState
import com.irah.galleria.ui.common.OperationProgressCard
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    viewModel: GalleryViewModel = hiltViewModel(),
    settingsViewModel: com.irah.galleria.ui.settings.SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val operationState by viewModel.operationState.collectAsState(initial = MediaOperationState.Idle)
    val settings by settingsViewModel.settings.collectAsState(initial = com.irah.galleria.domain.model.AppSettings())
    androidx.activity.compose.BackHandler(enabled = state.isSelectionMode) {
        viewModel.onEvent(GalleryEvent.ClearSelection)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is GalleryUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showAlbumSelectionSheet by remember { mutableStateOf(false) }
    var isCopyOperation by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
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
                 viewModel.onEvent(GalleryEvent.ClearSelection)
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
    val bottomBarVisibility = com.irah.galleria.ui.LocalBottomBarVisibility.current
    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            var accumulatedScroll = 0f

            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                
                
                if ((delta > 0 && accumulatedScroll < 0) || (delta < 0 && accumulatedScroll > 0)) {
                    accumulatedScroll = 0f
                }
                
                accumulatedScroll += delta

                if (accumulatedScroll < -150) {
                    if (bottomBarVisibility.value) bottomBarVisibility.value = false
                    accumulatedScroll = 0f
                } else if (accumulatedScroll > 150) {
                    if (!bottomBarVisibility.value) bottomBarVisibility.value = true
                    accumulatedScroll = 0f
                }
                
                return super.onPreScroll(available, source)
            }
        }
    }
    val context = LocalContext.current
    val uiMode = com.irah.galleria.ui.theme.LocalUiMode.current
    com.irah.galleria.ui.theme.GlassScaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val topBarColors = if (uiMode == com.irah.galleria.domain.model.UiMode.LIQUID_GLASS) {
                TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            }
            val normalTopBarColors = if (uiMode == com.irah.galleria.domain.model.UiMode.LIQUID_GLASS) {
                TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
            }
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedMediaIds.items.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(GalleryEvent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    colors = topBarColors,
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(GalleryEvent.SelectAll) }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Select All")
                        }
                    }
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.irah.galleria.R.mipmap.ic_launcher),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).padding(start = 12.dp),
                            tint = androidx.compose.ui.graphics.Color.Unspecified
                        )
                    },
                    title = { Text("Gallery") },
                    colors = normalTopBarColors,
                    actions = {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("All") }, onClick = { viewModel.onEvent(GalleryEvent.FilterChange(FilterType.All)); showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Images Only") }, onClick = { viewModel.onEvent(GalleryEvent.FilterChange(FilterType.Images)); showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Videos Only") }, onClick = { viewModel.onEvent(GalleryEvent.FilterChange(FilterType.Videos)); showFilterMenu = false })
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Date (Newest)") }, onClick = { viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Date(OrderType.Descending))); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Date (Oldest)") }, onClick = { viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Date(OrderType.Ascending))); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = { viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Name(OrderType.Ascending))); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Name (Z-A)") }, onClick = { viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Name(OrderType.Descending))); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Size (Largest)") }, onClick = { viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Size(OrderType.Descending))); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Size (Smallest)") }, onClick = { viewModel.onEvent(GalleryEvent.OrderChange(MediaOrder.Size(OrderType.Ascending))); showSortMenu = false })
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                com.irah.galleria.ui.gallery.components.AlbumSelectionSheet(
                    albums = state.albums.items,
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
                LinearProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.media.isEmpty()) {
                Text(
                    "No media found",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val contentPadding = PaddingValues(
                    top = 4.dp,  
                    bottom = padding.calculateBottomPadding() + 80.dp + 4.dp,  
                    start = 4.dp,
                    end = 4.dp
                )
                remember(state.media) { state.media.items.map { it.id } }
                val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                val staggeredGridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()
                val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                val density = androidx.compose.ui.platform.LocalDensity.current.density
                val itemSizePx = remember(screenWidth, settings.galleryGridCount) {
                     val spacing = 2 * density 
                     ((screenWidth * density - (settings.galleryGridCount + 1) * spacing) / settings.galleryGridCount).toInt()
                }

                val onMediaClick = remember(state.isSelectionMode, state.mediaOrder, state.filterType, navController) {
                    { media: Media ->
                        if (state.isSelectionMode) {
                            viewModel.onEvent(GalleryEvent.ToggleSelection(media.id))
                        } else {
                            val sortType = when(state.mediaOrder) {
                                is MediaOrder.Date -> "Date"
                                is MediaOrder.Name -> "Name"
                                is MediaOrder.Size -> "Size"
                            }
                            val orderDesc = state.mediaOrder.orderType is OrderType.Descending
                            val filterType = when(state.filterType) {
                                FilterType.Images -> "Images"
                                FilterType.Videos -> "Videos"
                                else -> "All"
                            }
                            navController.navigate(
                                Screen.MediaViewer.route + "/${media.id}" +
                                "?${Screen.MediaViewer.ALBUM_ID_ARG}=-1" +
                                "&${Screen.MediaViewer.SORT_TYPE_ARG}=$sortType" +
                                "&${Screen.MediaViewer.ORDER_DESC_ARG}=$orderDesc" +
                                "&${Screen.MediaViewer.FILTER_TYPE_ARG}=$filterType"
                            )
                        }
                    }
                }
                
                val onSelectionChange = remember(viewModel) {
                    { ids: Set<Long> ->
                        viewModel.onEvent(GalleryEvent.UpdateSelection(ids))
                    }
                }

                val onToggleSelection = remember(viewModel) {
                    { id: Long ->
                         viewModel.onEvent(GalleryEvent.ToggleSelection(id))
                    }
                }

                com.irah.galleria.ui.gallery.components.GalleryGridContent(
                    media = state.media,
                    selectedIds = state.selectedMediaIds,
                    isSelectionMode = state.isSelectionMode,
                    settings = settings,
                    contentPadding = contentPadding,
                    gridState = gridState,
                    staggeredGridState = staggeredGridState,
                    onMediaClick = onMediaClick,
                    onSelectionChange = onSelectionChange,
                    onToggleSelection = onToggleSelection,
                    imageLoader = context.imageLoader,
                    itemSizePx = itemSizePx
                )
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                com.irah.galleria.ui.common.SelectionActionMenu(
                    visible = state.isSelectionMode && operationState !is com.irah.galleria.domain.model.MediaOperationState.Running,
                    modifier = Modifier
                        .padding(bottom = if (operationState is com.irah.galleria.domain.model.MediaOperationState.Running) 80.dp else 16.dp),
                    onShare = { viewModel.shareSelectedMedia(context) },
                    onCopy = {
                        isCopyOperation = true
                        showAlbumSelectionSheet = true
                    },
                    onMove = {
                        isCopyOperation = false
                        showAlbumSelectionSheet = true
                    },
                    onDelete = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            performDelete()
                        } else {
                            showDeleteDialog = true
                        }
                    },
                    onFavorite = { viewModel.favoriteSelectedMedia() }
                )
            }
            
            androidx.compose.animation.AnimatedVisibility(
                visible = operationState is MediaOperationState.Running,
                enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp) 
                    .zIndex(15f) 
            ) {
                if (operationState is MediaOperationState.Running) {
                     OperationProgressCard(
                        state = operationState as MediaOperationState.Running
                    )
                }
            }
        }
    }
}