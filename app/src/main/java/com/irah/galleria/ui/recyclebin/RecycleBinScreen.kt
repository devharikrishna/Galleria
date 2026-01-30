package com.irah.galleria.ui.recyclebin
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.irah.galleria.ui.gallery.components.MediaGridItem
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    navController: NavController,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val actionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
             viewModel.onEvent(RecycleBinEvent.ClearSelection)
        }
    }
    val performRestore = {
        viewModel.restoreSelected { intentSender ->
            actionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }
    val performDeleteForever: () -> Unit = {
        viewModel.deleteForever { intentSender ->
            actionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }
    val performEmptyBin: () -> Unit = {
        viewModel.emptyBin { intentSender ->
            actionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }
    androidx.activity.compose.BackHandler(enabled = state.isSelectionMode) {
        viewModel.onEvent(RecycleBinEvent.ClearSelection)
    }
    var showEmptyBinDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showEmptyBinDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyBinDialog = false },
            title = { Text("Empty Bin") },
            text = { Text("Are you sure you want to permanently delete all items in the Recycle Bin? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyBinDialog = false
                    performEmptyBin()
                }) {
                    Text("Empty Bin", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyBinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Items") },
            text = { Text("Are you sure you want to restore selected item(s)?") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    performRestore()
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Permanently") },
            text = { Text("Are you sure you want to permanently delete selected item(s)? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    performDeleteForever()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    com.irah.galleria.ui.theme.GlassScaffold(
        topBar = {
            val uiMode = com.irah.galleria.ui.theme.LocalUiMode.current
            TopAppBar(
                title = { 
                    Text(if (state.isSelectionMode) "${state.selectedMediaIds.size} Selected" else "Recycle Bin") 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isSelectionMode) {
                        IconButton(onClick = { 
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                performRestore()
                            } else {
                                showRestoreDialog = true 
                            }
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore")
                        }
                        IconButton(onClick = { 
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                performDeleteForever()
                            } else {
                                showDeleteDialog = true 
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Forever")
                        }
                    } else if (state.media.isNotEmpty()) {
                         IconButton(onClick = { 
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                performEmptyBin()
                            } else {
                                showEmptyBinDialog = true 
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Empty Bin")
                        }
                    }
                },
                colors = if (uiMode == com.irah.galleria.domain.model.UiMode.LIQUID_GLASS) {
                    TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                } else {
                    TopAppBarDefaults.topAppBarColors()
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.media.isEmpty()) {
                Text("Recycle Bin is empty", modifier = Modifier.align(Alignment.Center))
            } else {
                val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                val density = androidx.compose.ui.platform.LocalDensity.current.density
                val itemSizePx = remember(screenWidth) {
                    ((screenWidth / 3) * density).toInt()
                }

                val mediaIds = remember(state.media) { state.media.map { it.id } }
                val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                com.irah.galleria.ui.gallery.components.DragSelectReceiver(
                    items = mediaIds,
                    selectedIds = state.selectedMediaIds,
                    onSelectionChange = { ids ->
                        viewModel.onEvent(RecycleBinEvent.UpdateSelection(ids))
                    },
                    getItemIndexAtPosition = { offset ->
                        gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                            val itemOffset = item.offset
                            offset.x >= itemOffset.x - 50 && offset.x <= itemOffset.x + item.size.width + 50 &&
                            offset.y >= itemOffset.y - 50 && offset.y <= itemOffset.y + item.size.height + 50
                        }?.index
                    },
                    scrollBy = { gridState.scrollBy(it) },
                    viewportHeight = { gridState.layoutInfo.viewportSize.height }
                ) { dragModifier ->
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize().then(dragModifier),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        items(
                        items = state.media,
                        key = { it.id },
                        contentType = { "media" }
                    ) { media ->
                            val isSelected = state.selectedMediaIds.contains(media.id)
                            MediaGridItem(
                                media = media,
                                isSelected = isSelected,
                                animationsEnabled = true,
                                itemSizePx = itemSizePx,
                                onClick = {
                                    if (state.isSelectionMode) {
                                         viewModel.onEvent(RecycleBinEvent.ToggleSelection(media.id))    
                                    } else {
                                        viewModel.onEvent(RecycleBinEvent.ToggleSelection(media.id))
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