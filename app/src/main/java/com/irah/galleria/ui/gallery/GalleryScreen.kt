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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.imageLoader
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
    androidx.activity.compose.BackHandler(enabled = state.isSelectionMode) {
        viewModel.onEvent(GalleryEvent.ClearSelection)
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
                
                // Reset accumulation if direction changes logic can be subtle, 
                // but usually just adding to accumulator works if we clamp or reset on toggle.
                // A simple approach:
                if ((delta > 0 && accumulatedScroll < 0) || (delta < 0 && accumulatedScroll > 0)) {
                    accumulatedScroll = 0f
                }
                
                accumulatedScroll += delta

                if (accumulatedScroll < -150) { // Scroll down (content moves up)
                    if (bottomBarVisibility.value) bottomBarVisibility.value = false
                    accumulatedScroll = 0f // Reset after triggering
                } else if (accumulatedScroll > 150) { // Scroll up (content moves down)
                    if (!bottomBarVisibility.value) bottomBarVisibility.value = true
                    accumulatedScroll = 0f // Reset after triggering
                }
                
                return super.onPreScroll(available, source)
            }
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiMode = com.irah.galleria.ui.theme.LocalUiMode.current
    com.irah.galleria.ui.theme.GlassScaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
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
                    title = { Text("${state.selectedMediaIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(GalleryEvent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    colors = topBarColors,
                    actions = {
                        IconButton(onClick = { 
                            isCopyOperation = false
                            showAlbumSelectionSheet = true 
                        }) {
                            Icon(Icons.Default.Folder, contentDescription = "Move to Album")
                        }
                        IconButton(onClick = { 
                            isCopyOperation = true
                            showAlbumSelectionSheet = true 
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy to Album")
                        }
                        IconButton(onClick = { viewModel.shareSelectedMedia(context) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { 
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                performDelete()
                            } else {
                                showDeleteDialog = true 
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            } else {
                TopAppBar(
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
                    albums = state.albums,
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
                val mediaIds = remember(state.media) { state.media.map { it.id } }
                if (settings.galleryViewType == com.irah.galleria.domain.model.GalleryViewType.GRID) {
                        val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                        
                        // Smart Pre-loading for Grid - use singleton ImageLoader
                        val imageLoader = context.imageLoader
                        val preloadedIds = remember { mutableSetOf<Long>() }
                        val lastVisibleIndex by remember {
                           androidx.compose.runtime.derivedStateOf {
                               gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                           }
                        }
                        
                        val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                        val density = androidx.compose.ui.platform.LocalDensity.current.density
                        val itemSizePx = remember(screenWidth, settings.galleryGridCount) {
                             ((screenWidth / settings.galleryGridCount) * density).toInt()
                        }
                        
                        androidx.compose.runtime.LaunchedEffect(lastVisibleIndex) {
                            val totalItems = state.media.size
                            val startIndex = lastVisibleIndex + 1
                            val endIndex = (startIndex + 20).coerceAtMost(totalItems)
                            
                            if (startIndex < endIndex) {
                                for (i in startIndex until endIndex) {
                                    val media = state.media[i]
                                    if (preloadedIds.add(media.id)) {
                                        val request = coil.request.ImageRequest.Builder(context)
                                            .data(media.uri)
                                            .size(itemSizePx)
                                            .memoryCacheKey("${media.id}_$itemSizePx")
                                            .diskCacheKey("${media.id}_$itemSizePx")
                                            .build()
                                        imageLoader.enqueue(request)
                                    }
                                }
                            }
                        }
                    com.irah.galleria.ui.gallery.components.DragSelectReceiver(
                        items = mediaIds,
                        selectedIds = state.selectedMediaIds,
                        onSelectionChange = { ids ->
                             viewModel.onEvent(GalleryEvent.UpdateSelection(ids))
                        },
                        getItemIndexAtPosition = { offset ->
                            gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                val itemOffset = item.offset
                                val itemSize = item.size
                                offset.x >= itemOffset.x - 50 && offset.x <= itemOffset.x + itemSize.width + 50 &&
                                offset.y >= itemOffset.y - 50 && offset.y <= itemOffset.y + itemSize.height + 50
                            }?.index
                        },
                        scrollBy = { gridState.scrollBy(it) },
                        viewportHeight = { gridState.layoutInfo.viewportSize.height }
                    ) { dragModifier ->
                        com.irah.galleria.ui.gallery.components.FastScroller(
                            gridState = gridState,
                            itemCount = state.media.size,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(settings.galleryGridCount),
                                modifier = Modifier.fillMaxSize().then(dragModifier),
                                contentPadding = contentPadding,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(
                                    items = state.media,
                                    key = { it.id },
                                    contentType = { "media" }
                                ) { media ->
                                    val isSelected = state.selectedMediaIds.contains(media.id)
                                    MediaGridItem(
                                        media = media,
                                        isStaggered = false,
                                        cornerRadius = settings.galleryCornerRadius,
                                        animationsEnabled = settings.animationsEnabled,
                                        isSelected = isSelected,
                                        gridColumnCount = settings.galleryGridCount,
                                        onClick = {
                                            if (state.isSelectionMode) {
                                                viewModel.onEvent(GalleryEvent.ToggleSelection(media.id))
                                            } else {
                                                navController.navigate(
                                                    Screen.MediaViewer.route + "/${media.id}"
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val staggeredGridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

                    // Smart Pre-loading for Staggered Grid - use singleton ImageLoader
                    val imageLoader = context.imageLoader
                    val preloadedIds = remember { mutableSetOf<Long>() }
                    val lastVisibleIndex by remember {
                        androidx.compose.runtime.derivedStateOf {
                            staggeredGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        }
                    }
                    
                    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                    val density = androidx.compose.ui.platform.LocalDensity.current.density
                    val itemSizePx = remember(screenWidth, settings.galleryGridCount) {
                         ((screenWidth / settings.galleryGridCount) * density).toInt()
                    }

                    androidx.compose.runtime.LaunchedEffect(lastVisibleIndex) {
                        val totalItems = state.media.size
                        val startIndex = lastVisibleIndex + 1
                        val endIndex = (startIndex + 20).coerceAtMost(totalItems)

                        if (startIndex < endIndex) {
                            for (i in startIndex until endIndex) {
                                val media = state.media[i]
                                if (preloadedIds.add(media.id)) {
                                    val request = coil.request.ImageRequest.Builder(context)
                                        .data(media.uri)
                                        .size(itemSizePx)
                                        .memoryCacheKey("${media.id}_$itemSizePx")
                                        .diskCacheKey("${media.id}_$itemSizePx")
                                        .build()
                                    imageLoader.enqueue(request)
                                }
                            }
                        }
                    }
                    com.irah.galleria.ui.gallery.components.DragSelectReceiver(
                        items = mediaIds,
                        selectedIds = state.selectedMediaIds,
                        onSelectionChange = { ids ->
                             viewModel.onEvent(GalleryEvent.UpdateSelection(ids))
                        },
                        getItemIndexAtPosition = { offset ->
                            staggeredGridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                val itemOffset = item.offset
                                val itemSize = item.size
                                offset.x >= itemOffset.x - 50 && offset.x <= itemOffset.x + itemSize.width + 50 &&
                                offset.y >= itemOffset.y - 50 && offset.y <= itemOffset.y + itemSize.height + 50
                            }?.index
                        },
                        scrollBy = { staggeredGridState.scrollBy(it) },
                        viewportHeight = { staggeredGridState.layoutInfo.viewportSize.height }
                    ) { dragModifier ->
                        com.irah.galleria.ui.gallery.components.FastScroller(
                            staggeredGridState = staggeredGridState,
                            itemCount = state.media.size,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid(
                                state = staggeredGridState,
                                columns = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Fixed(settings.galleryGridCount),
                                modifier = Modifier.fillMaxSize().then(dragModifier),
                                contentPadding = contentPadding,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalItemSpacing = 2.dp
                            ) {
                                items(
                                    items = state.media,
                                    key = { it.id },
                                    contentType = { "media" }
                                ) { media ->
                                    val isSelected = state.selectedMediaIds.contains(media.id)
                                    MediaGridItem(
                                        media = media,
                                        modifier = Modifier.fillMaxWidth(),
                                        isStaggered = true,
                                        cornerRadius = settings.galleryCornerRadius,
                                        animationsEnabled = settings.animationsEnabled,
                                        isSelected = isSelected,
                                        gridColumnCount = settings.galleryGridCount,
                                        onClick = {
                                            if (state.isSelectionMode) {
                                                viewModel.onEvent(GalleryEvent.ToggleSelection(media.id))
                                            } else {
                                                navController.navigate(
                                                    Screen.MediaViewer.route + "/${media.id}"
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}