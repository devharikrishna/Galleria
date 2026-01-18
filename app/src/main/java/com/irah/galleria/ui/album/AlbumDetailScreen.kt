package com.irah.galleria.ui.album

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.irah.galleria.ui.album.AlbumDetailEvent
import com.irah.galleria.ui.gallery.components.AlbumSelectionSheet
import com.irah.galleria.ui.gallery.components.MediaGridItem
import com.irah.galleria.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    navController: NavController,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
             viewModel.onEvent(AlbumDetailEvent.ClearSelection)
        }
    }
    
    var showMoveSheet by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    val albums by viewModel.albums.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedMediaIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(AlbumDetailEvent.ClearSelection) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear Selection")
                        }
                    },
                     actions = {
                        IconButton(onClick = { showMoveSheet = true }) {
                            Icon(Icons.Filled.Folder, contentDescription = "Move to Album")
                        }
                        IconButton(onClick = { viewModel.shareSelectedMedia(navController.context) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = {
                            viewModel.deleteSelectedMedia { intentSender ->
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(state.albumName) },
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
             if (showMoveSheet) {
                AlbumSelectionSheet(
                    albums = albums,
                    onAlbumSelected = { album ->
                        showMoveSheet = false
                        val target = album.relativePath ?: "Pictures/${album.name}"
                        viewModel.moveSelectedMedia(target) { intentSender ->
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(intentSender).build()
                            )
                        }
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
                                viewModel.moveSelectedMedia("Pictures/$newAlbumName") { intentSender ->
                                     deleteLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                     )
                                }
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
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.media) { media ->
                    val isSelected = state.selectedMediaIds.contains(media.id)
                    MediaGridItem(
                        media = media,
                        isSelected = isSelected,
                        onClick = {
                            if (state.isSelectionMode) {
                                viewModel.onEvent(AlbumDetailEvent.ToggleSelection(media.id))
                            } else {
                                navController.navigate(
                                    Screen.MediaViewer.route + "/${media.id}?albumId=${media.bucketId}"
                                )
                            }
                        },
                        onLongClick = {
                            viewModel.onEvent(AlbumDetailEvent.ToggleSelection(media.id))
                        }
                    )
                }
            }
        }
    }
}
