package com.irah.galleria.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun ForceSystemNavigationColor(
    navigationBarColor: Color,
    lightNavigationBars: Boolean = false
) {
    val context = LocalContext.current
    val view = LocalView.current

    if (!view.isInEditMode) {
        val window = findActivity(context)?.window

        // Re-apply on every composition, but use post() to ensure it runs AFTER other layout/theme passes
        androidx.compose.runtime.SideEffect {
            view.post {
                val insetsController = if (window != null) WindowCompat.getInsetsController(window, view) else null
                
                if (window != null && insetsController != null) {
                    // Clear translucent navigation if set, and ensure we draw backgrounds
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    
                    window.navigationBarColor = navigationBarColor.toArgb()
                    insetsController.isAppearanceLightNavigationBars = lightNavigationBars
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                }
            }
        }
    }
}

fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}