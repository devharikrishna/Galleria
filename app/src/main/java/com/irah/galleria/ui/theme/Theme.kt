package com.irah.galleria.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun GalleriaTheme(
    themeMode: com.irah.galleria.domain.model.ThemeMode = com.irah.galleria.domain.model.ThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    accentColor: Long = 0xFF6650a4,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        com.irah.galleria.domain.model.ThemeMode.LIGHT -> false
        com.irah.galleria.domain.model.ThemeMode.DARK -> true
        com.irah.galleria.domain.model.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (e: Exception) {
                // Fallback if dynamic color fails
                val primary = androidx.compose.ui.graphics.Color(accentColor.toInt())
                val secondary = primary.copy(alpha = 0.8f) 
                val tertiary = androidx.compose.ui.graphics.Color.Gray 
                if (darkTheme) darkColorScheme(primary = primary, secondary = secondary, tertiary = tertiary)
                else lightColorScheme(primary = primary, secondary = secondary, tertiary = tertiary)
            }
        }
        else -> {
            // Safe color construction
            val primary = try {
                androidx.compose.ui.graphics.Color(accentColor.toInt())
            } catch (e: Exception) {
                androidx.compose.ui.graphics.Color(0xFF6650a4) // Default Purple
            }
            val secondary = primary.copy(alpha = 0.8f) 
            val tertiary = androidx.compose.ui.graphics.Color.Gray 

            if (darkTheme) {
                darkColorScheme(primary = primary, secondary = secondary, tertiary = tertiary)
            } else {
                lightColorScheme(primary = primary, secondary = secondary, tertiary = tertiary)
            }
        }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}