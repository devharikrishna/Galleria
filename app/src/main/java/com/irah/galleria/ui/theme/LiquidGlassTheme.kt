package com.irah.galleria.ui.theme

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
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

@Composable
fun AnimatedLiquidBackground(isDark: Boolean) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "liquid_bg")
    
    // Independent animations for chaotic/organic movement
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(13000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(17000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "t2"
    )
    val t3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(23000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "t3"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Deep Liquid Base
        val baseColors = if (isDark) {
            listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B)) // Deepest Slate/Navy
        } else {
            listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFFBAE6FD))
        }
        drawRect(brush = Brush.verticalGradient(baseColors))
        
        // Vibrant Aurora Blobs - Increased Alpha and Size
        val blobColors = if (isDark) {
            listOf(
                Color(0xFF4F46E5).copy(alpha = 0.5f), // Indigo
                Color(0xFFEC4899).copy(alpha = 0.4f), // Pink
                Color(0xFF06B6D4).copy(alpha = 0.4f), // Cyan
                Color(0xFF8B5CF6).copy(alpha = 0.5f), // Violet
                Color(0xFF10B981).copy(alpha = 0.3f)  // Emerald
            )
        } else {
             listOf(
                Color(0xFFA5F3FC).copy(alpha = 0.7f), // Cyan
                Color(0xFFFBCFE8).copy(alpha = 0.6f), // Pink
                Color(0xFFDDD6FE).copy(alpha = 0.7f), // Violet
                Color(0xFFBBF7D0).copy(alpha = 0.6f), // Green
                Color(0xFFBAE6FD).copy(alpha = 0.7f)  // Blue
            )
        }
        
        // Blob 1: Top-Left to Bottom-Right (Full Diagonal Sweep)
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(blobColors[0], Color.Transparent)),
            radius = w * 0.9f, // Massive radius
            center = androidx.compose.ui.geometry.Offset(
                x = w * -0.3f + (w * 1.6f * t1), // -30% to 130%
                y = h * -0.2f + (h * 1.4f * t2)  // -20% to 120%
            )
        )
        
        // Blob 2: Bottom-Right to Top-Left
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(blobColors[1], Color.Transparent)),
            radius = w * 0.85f,
            center = androidx.compose.ui.geometry.Offset(
                x = w * 1.3f - (w * 1.6f * t2), 
                y = h * 1.2f - (h * 1.4f * t3)
            )
        )

        // Blob 3: Bottom-Left to Top-Right
         drawCircle(
            brush = Brush.radialGradient(colors = listOf(blobColors[2], Color.Transparent)),
            radius = w * 0.8f,
            center = androidx.compose.ui.geometry.Offset(
                x = w * -0.2f + (w * 1.5f * t3), 
                y = h * 1.2f - (h * 1.4f * t1)
            )
        )
        
        // Blob 4: Top-Right to Bottom-Left
         drawCircle(
            brush = Brush.radialGradient(colors = listOf(blobColors[3], Color.Transparent)),
            radius = w * 0.85f,
            center = androidx.compose.ui.geometry.Offset(
                x = w * 1.2f - (w * 1.5f * t1), 
                y = h * -0.2f + (h * 1.5f * t3)
            )
        )
        
        // Blob 5: Wandering Center but wide coverage
         drawCircle(
            brush = Brush.radialGradient(colors = listOf(blobColors[4], Color.Transparent)),
            radius = w * 1.0f,
            center = androidx.compose.ui.geometry.Offset(
                x = w * 0.5f + (w * 0.5f * (t2 - 0.5f)), 
                y = h * 0.5f + (h * 0.5f * (t1 - 0.5f))
            )
        )
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
        // Translucent glass color
        val glassColor = if (isDark) {
            Color(0xFF1E293B).copy(alpha = 0.5f) // Dark Slate translucent
        } else {
            Color.White.copy(alpha = 0.4f) // Lighter/Cleaner white glass for Light Mode
        }
        
        val borderColor = if (isDark) {
            Color.White.copy(alpha = 0.1f) // More subtle, premium border
        } else {
            Color.White.copy(alpha = 0.7f)
        }
        
        val actualContentColor = if (isDark) Color.White else Color(0xFF0F172A)

        // Enhancing glass with a linear gradient sheen
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
            color = Color.Transparent, // Let the Box handle the visuals
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
            Color(0xFF0F172A).copy(alpha = 0.85f)
        } else {
            Color(0xFFF1F5F9).copy(alpha = 0.85f)
        }
        
        val borderColor = if (isDark) {
            Color.White.copy(alpha = 0.1f)
        } else {
            Color(0xFFE2E8F0) // Subtle Slate grid for light mode nav
        }
        
        Surface(
             color = glassColor,
             contentColor = contentColor,
             tonalElevation = 0.dp,
             modifier = modifier.border(
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
