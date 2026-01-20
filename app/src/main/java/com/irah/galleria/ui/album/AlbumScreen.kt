package com.irah.galleria.ui.album
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
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
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < -5) {
                    bottomBarVisibility.value = false
                } else if (available.y > 5) {
                    bottomBarVisibility.value = true
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
                title = { Text("Albums") },
                colors = if (uiMode == com.irah.galleria.domain.model.UiMode.LIQUID_GLASS) {
                    androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                } else {
                    androidx.compose.material3.TopAppBarDefaults.topAppBarColors()
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.albums.isEmpty()) {
                Text(
                    "No albums found",
                    modifier = Modifier.align(Alignment.Center)
                )
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
    }
}