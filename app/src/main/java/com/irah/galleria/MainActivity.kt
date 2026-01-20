package com.irah.galleria
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.irah.galleria.ui.MainScreen
import com.irah.galleria.ui.theme.GalleriaTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.irah.galleria.domain.model.AppSettings
import com.irah.galleria.ui.settings.SettingsViewModel
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
                accentColor = settings.accentColor
            ) {
                com.irah.galleria.ui.common.PermissionWrapper {
                    MainScreen()
                }
            }
        }
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GalleriaTheme {
        Greeting("Android")
    }
}