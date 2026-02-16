package com.irah.galleria.ui.album
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.irah.galleria.ui.album.components.AlbumGridItem
import com.irah.galleria.ui.navigation.Screen
import com.irah.galleria.ui.theme.GlassScaffold
import com.irah.galleria.ui.theme.LocalUiMode
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    viewModel: AlbumViewModel = hiltViewModel(),
    settingsViewModel: com.irah.galleria.ui.settings.SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settings by settingsViewModel.settings.collectAsState(initial = com.irah.galleria.domain.model.AppSettings())
    val bottomBarVisibility = com.irah.galleria.ui.LocalBottomBarVisibility.current
    val nestedScrollConnection = androidx.compose.runtime.remember {
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
    val uiMode = LocalUiMode.current
    GlassScaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.irah.galleria.R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).padding(start = 12.dp),
                        tint = androidx.compose.ui.graphics.Color.Unspecified
                    )
                },
                title = { Text("Albums") },
                colors = if (uiMode == com.irah.galleria.domain.model.UiMode.LIQUID_GLASS) {
                    TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                } else {
                    TopAppBarDefaults.topAppBarColors()
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(settings.albumGridCount),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        start = 8.dp,
                        end = 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 80.dp  
                    ),
                ) {
                    if (state.smartAlbums.isNotEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(settings.albumGridCount) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                state.smartAlbums.forEach { album ->
                                    SmartAlbumItem(
                                        album = album,
                                        onClick = {
                                            when(album.id) {
                                                -4L -> navController.navigate(Screen.RecycleBin.route)
                                                else -> navController.navigate(Screen.AlbumDetail.route + "/${album.id}/${album.name}")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (state.albums.isEmpty() && state.smartAlbums.isEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(settings.albumGridCount) }) {
                            Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp)) {
                                Text(
                                    "No albums found",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    } else {
                        items(state.albums) { album ->
                            AlbumGridItem(
                                album = album,
                                showMediaCount = settings.showMediaCount,
                                cornerRadius = settings.albumCornerRadius,
                                onClick = {
                                    navController.navigate(
                                        Screen.AlbumDetail.route + "/${album.id}/${album.name}"
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            val operationState by viewModel.operationState.collectAsState(initial = com.irah.galleria.domain.model.MediaOperationState.Idle)
            
            androidx.compose.animation.AnimatedVisibility(
                visible = operationState is com.irah.galleria.domain.model.MediaOperationState.Running,
                enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp) 
                    .zIndex(15f)
            ) {
                if (operationState is com.irah.galleria.domain.model.MediaOperationState.Running) {
                     com.irah.galleria.ui.common.OperationProgressCard(
                        state = operationState as com.irah.galleria.domain.model.MediaOperationState.Running
                    )
                }
            }
        }
    }
}

@Composable
fun SmartAlbumItem(
    album: com.irah.galleria.domain.model.Album,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        val icon = when(album.id) {
            -2L -> Icons.Outlined.FavoriteBorder
            -3L -> Icons.Outlined.Smartphone
            -4L -> Icons.Outlined.AutoDelete
            else -> Icons.Outlined.Folder
        }
        val tint = MaterialTheme.colorScheme.primary
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                imageVector = if (album.id == -4L) Icons.Outlined.Delete else icon,
                contentDescription = album.name,
                tint = tint,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
        Text(
            text = "${album.count}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}