package com.irah.galleria
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.irah.galleria.domain.model.AppSettings
import com.irah.galleria.ui.MainScreen
import com.irah.galleria.ui.common.PermissionWrapper
import com.irah.galleria.ui.settings.SettingsViewModel
import com.irah.galleria.ui.theme.GalleriaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SettingsViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsState(initial = AppSettings())
            GalleriaTheme(
                themeMode = settings.themeMode,
                uiMode = settings.uiMode,
                useDynamicColor = settings.useDynamicColor,
                accentColor = settings.accentColor,
                blobAnimation = settings.blobAnimation
            ) {
                PermissionWrapper {
                    MainScreen()
                }
            }
        }
    }
}