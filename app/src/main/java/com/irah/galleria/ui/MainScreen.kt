package com.irah.galleria.ui
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.irah.galleria.ui.album.AlbumDetailScreen
import com.irah.galleria.ui.album.AlbumScreen
import com.irah.galleria.ui.editor.EditorScreen
import com.irah.galleria.ui.gallery.GalleryScreen
import com.irah.galleria.ui.mediaviewer.MediaViewerScreen
import com.irah.galleria.ui.navigation.Screen
import com.irah.galleria.ui.recyclebin.RecycleBinScreen
import com.irah.galleria.ui.settings.SettingsScreen
import com.irah.galleria.ui.theme.GlassScaffold

val LocalBottomBarVisibility = compositionLocalOf { mutableStateOf(true) }
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Gallery : BottomNavItem(
        route = Screen.Gallery.route,
        title = "Gallery",
        selectedIcon = Icons.Filled.Collections,
        unselectedIcon = Icons.Outlined.Collections
    )
    object Album : BottomNavItem(
        route = Screen.Album.route,
        title = "Albums",
        selectedIcon = Icons.Filled.PhotoAlbum,
        unselectedIcon = Icons.Outlined.PhotoAlbum
    )
    object Settings : BottomNavItem(
        route = Screen.Settings.route,
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val bottomBarVisibility = remember { mutableStateOf(true) }
    val items = listOf(
        BottomNavItem.Gallery,
        BottomNavItem.Album,
        BottomNavItem.Settings
    )
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    CompositionLocalProvider(LocalBottomBarVisibility provides bottomBarVisibility) {
        GlassScaffold(
            contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val isMainScreen = items.any { it.route == currentDestination?.route }
                
                // Centralized System Navigation Bar Color Logic
                val currentRoute = currentDestination?.route
                val isBlackNavigationBar = currentRoute?.startsWith(Screen.Editor.route.substringBefore("/")) == true ||
                                         currentRoute?.startsWith(Screen.MediaViewer.route.substringBefore("/")) == true

                if (isBlackNavigationBar) {
                    com.irah.galleria.ui.util.ForceSystemNavigationColor(androidx.compose.ui.graphics.Color.Black)
                } else {
                     // Revert to transparent/default for other screens to allow Liquid Glass effect
                    com.irah.galleria.ui.util.ForceSystemNavigationColor(androidx.compose.ui.graphics.Color.Transparent)
                }

                AnimatedVisibility(
                    visible = isMainScreen && bottomBarVisibility.value,
                    enter = slideInVertically { it } + expandVertically(),
                    exit = slideOutVertically { it } + shrinkVertically()
                ) {
                    val uiMode = com.irah.galleria.ui.theme.LocalUiMode.current
                    if (uiMode == com.irah.galleria.domain.model.UiMode.LIQUID_GLASS) {
                        com.irah.galleria.ui.theme.GlassNavigationBar {
                             items.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = { Text(screen.title) },
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        NavigationBar {
                            items.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = { Text(screen.title) },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                }
            ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Gallery.route,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                composable(Screen.Gallery.route) {
                    GalleryScreen(navController = navController)
                }
                composable(Screen.Album.route) {
                    AlbumScreen(navController = navController)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(navController = navController)
                }
                composable(Screen.RecycleBin.route) {
                    RecycleBinScreen(navController = navController)
                }
                composable(
                    route = Screen.AlbumDetail.routeWithArgs,
                    arguments = Screen.AlbumDetail.arguments
                ) {
                    AlbumDetailScreen(navController = navController)
                }
                composable(
                    route = Screen.MediaViewer.routeWithArgs,
                    arguments = Screen.MediaViewer.arguments
                ) {
                    MediaViewerScreen(navController = navController)
                }
                composable(
                    route = Screen.Editor.routeWithArgs,
                    arguments = Screen.Editor.arguments
                ) {
                    EditorScreen(navController = navController)
                }
            }
        }
    }
}