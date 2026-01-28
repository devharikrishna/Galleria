package com.irah.galleria.ui.navigation
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
sealed class Screen(val route: String) {
    object Gallery : Screen("gallery_screen")
    object Album : Screen("album_screen")
    object Settings : Screen("settings_screen")
    object RecycleBin : Screen("recycle_bin")
    object AlbumDetail : Screen("album_detail_screen") {
        const val ALBUM_ID_ARG = "albumId"
        const val ALBUM_NAME_ARG = "albumName"
        val routeWithArgs = "$route/{$ALBUM_ID_ARG}/{$ALBUM_NAME_ARG}"
        val arguments: List<NamedNavArgument> = listOf(
            navArgument(ALBUM_ID_ARG) { type = NavType.LongType },
            navArgument(ALBUM_NAME_ARG) { type = NavType.StringType }
        )
    }
    object MediaViewer : Screen("media_viewer_screen") {
        const val MEDIA_ID_ARG = "mediaId"
        const val ALBUM_ID_ARG = "albumId"  
        val routeWithArgs = "$route/{$MEDIA_ID_ARG}?albumId={$ALBUM_ID_ARG}"
        val arguments: List<NamedNavArgument> = listOf(
            navArgument(MEDIA_ID_ARG) { type = NavType.LongType },
            navArgument(ALBUM_ID_ARG) { 
                type = NavType.LongType
                defaultValue = -1L 
            }
        )
    }
    object Editor : Screen("editor_screen") {
        const val MEDIA_ID_ARG = "mediaId"
        val routeWithArgs = "$route/{$MEDIA_ID_ARG}"
        val arguments: List<NamedNavArgument> = listOf(
            navArgument(MEDIA_ID_ARG) { type = NavType.LongType }
        )
    }
}