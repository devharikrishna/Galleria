package com.irah.galleria.ui.recyclebin

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
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

    Scaffold(
        topBar = {
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
                            viewModel.restoreSelected { intentSender ->
                                actionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore")
                        }
                        IconButton(onClick = {
                             viewModel.deleteForever { intentSender ->
                                actionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Forever")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.media.isEmpty()) {
                Text("Recycle Bin is empty", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(state.media) { media ->
                        val isSelected = state.selectedMediaIds.contains(media.id)
                        MediaGridItem(
                            media = media,
                            isSelected = isSelected,
                            animationsEnabled = false,
                            onClick = {
                                viewModel.onEvent(RecycleBinEvent.ToggleSelection(media.id))    
                            },
                            onLongClick = {
                                viewModel.onEvent(RecycleBinEvent.ToggleSelection(media.id))
                            }
                        )
                    }
                }
            }
        }
    }
}
