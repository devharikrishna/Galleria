package com.irah.galleria.ui.theme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.irah.galleria.domain.model.UiMode

val LocalUiMode = staticCompositionLocalOf { UiMode.MATERIAL }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit
) {
    if (LocalUiMode.current == UiMode.LIQUID_GLASS) {
        val isDark = LocalIsDarkTheme.current
        Box(modifier = modifier.fillMaxSize()) {
            AnimatedLiquidBackground(isDark = isDark)
            Scaffold(
                modifier = Modifier,
                containerColor = Color.Transparent,
                contentColor = if (isDark) Color.White else Color(0xFF0F172A),
                topBar = topBar,
                bottomBar = bottomBar,
                snackbarHost = snackbarHost,
                floatingActionButton = floatingActionButton,
                floatingActionButtonPosition = floatingActionButtonPosition,
                contentWindowInsets = contentWindowInsets,
                content = content
            )
        }
    } else {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            contentWindowInsets = contentWindowInsets,
            content = content
        )
    }
}
val LocalBackgroundAnimation = staticCompositionLocalOf { com.irah.galleria.domain.model.BackgroundAnimationType.BLOB }

@Composable
fun AnimatedLiquidBackground(isDark: Boolean) {
    val animationType = LocalBackgroundAnimation.current
    when(animationType) {
        com.irah.galleria.domain.model.BackgroundAnimationType.BLOB -> AnimatedBlob(isDark)
        com.irah.galleria.domain.model.BackgroundAnimationType.WAVE -> AnimatedWave(isDark)
        com.irah.galleria.domain.model.BackgroundAnimationType.GRADIENT -> AnimatedGradient(isDark)
        com.irah.galleria.domain.model.BackgroundAnimationType.PARTICLES -> AnimatedParticles(isDark)
        com.irah.galleria.domain.model.BackgroundAnimationType.MESH -> AnimatedMesh(isDark)
    }
}
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: Boolean = true,
    content: @Composable () -> Unit
) {
    val isGlass = LocalUiMode.current == UiMode.LIQUID_GLASS
    if (isGlass) {
        val isDark = LocalIsDarkTheme.current
        val glassColor = if (isDark) {
            Color(0xFF1E293B).copy(alpha = 0.5f)  
        } else {
            Color.White.copy(alpha = 0.4f)  
        }
        val borderColor = if (isDark) {
            Color.White.copy(alpha = 0.1f)  
        } else {
            Color.White.copy(alpha = 0.7f)
        }
        val actualContentColor = if (isDark) Color.White else Color(0xFF0F172A)
        val sheenGradient = Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                )
            } else {
                 listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.White.copy(alpha = 0.1f)
                )
            }
        )
        Surface(
            modifier = modifier
                .then(if (border) Modifier.border(1.dp, borderColor, shape) else Modifier),
            shape = shape,
            color = Color.Transparent,  
            contentColor = actualContentColor,
            tonalElevation = 0.dp, 
            shadowElevation = 0.dp,
        ) {
            Box(
                 modifier = Modifier
                    .background(glassColor)
                    .background(sheenGradient)
            ) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            content = content
        )
    }
}
@Composable
fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit
) {
    if (LocalUiMode.current == UiMode.LIQUID_GLASS) {
        val isDark = LocalIsDarkTheme.current
        val glassColor = if (isDark) {
            Color(0xFF0F172A).copy(alpha = 0.70f)
        } else {
            Color(0xFFF1F5F9).copy(alpha = 0.70f)
        }
        val borderColor = if (isDark) {
            Color.White.copy(alpha = 0.1f)
        } else {
            Color(0xFFE2E8F0)  
        }
        Surface(
             color = glassColor,
             contentColor = contentColor,
             tonalElevation = 0.dp,
             modifier = modifier
                 .border(
                 width = 1.dp,
                 color = borderColor,
                 shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
             ),
             shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            NavigationBar(
                modifier = Modifier,
                containerColor = Color.Transparent,
                contentColor = contentColor,
                tonalElevation = 0.dp,
                windowInsets = windowInsets,
                content = content
            )
        }
    } else {
        NavigationBar(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            windowInsets = windowInsets,
            content = content
        )
    }
}