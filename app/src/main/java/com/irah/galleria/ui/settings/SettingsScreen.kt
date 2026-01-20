package com.irah.galleria.ui.settings

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.CodeOff
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.irah.galleria.R
import com.irah.galleria.domain.model.GalleryViewType
import com.irah.galleria.domain.model.ThemeMode
import com.irah.galleria.domain.model.UiMode
import com.irah.galleria.ui.LocalBottomBarVisibility
import com.irah.galleria.ui.navigation.Screen
import com.irah.galleria.ui.theme.GlassScaffold
import com.irah.galleria.ui.theme.GlassSurface
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    val bottomBarVisibility = LocalBottomBarVisibility.current
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -5) {
                    bottomBarVisibility.value = false
                } else if (available.y > 5) {
                    bottomBarVisibility.value = true
                }
                return super.onPreScroll(available, source)
            }
        }
    }


    GlassScaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        topBar = {
            val uiMode = com.irah.galleria.ui.theme.LocalUiMode.current
            TopAppBar(
                title = { Text("Settings") },
                colors = if (uiMode == UiMode.LIQUID_GLASS) {
                    androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                } else {
                    androidx.compose.material3.TopAppBarDefaults.topAppBarColors()
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {


            // --- Developed By ---
            item {
                SettingsGroup(title = "Developed By", icon = Icons.Outlined.CodeOff) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Profile Image
                            Image(
                                painter = painterResource(id = R.drawable.developer_image),
                                contentDescription = "Developer",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = getString(context, R.string.developer_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Senior AI and Android Developer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Social Icons Row
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            // LinkedIn
                            SocialIcon(
                                icon = ImageVector.vectorResource(id = R.drawable.ic_linkedin),
                                onClick = {
                                    // Assuming specific linkedIn URL or a search
                                    val intent = Intent(Intent.ACTION_VIEW, getString(context, R.string.linkedin_url).toUri())
                                    context.startActivity(intent)
                                }
                            )
                            Spacer(modifier = Modifier.width(24.dp))

                            // GitHub
                            SocialIcon(
                                icon = ImageVector.vectorResource(id = R.drawable.ic_github),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, getString(context, R.string.github_url).toUri())
                                    context.startActivity(intent)
                                }
                            )


                            Spacer(modifier = Modifier.width(24.dp))
                            // Email
                            SocialIcon(
                                icon = Icons.Outlined.Email,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = "mailto:${getString(context, R.string.developer_email)}".toUri()
                                    }
                                    try { context.startActivity(intent) } catch (e: Exception) {
                                        Log.e("SettingsScreen", "Error launching email intent", e)
                                    }
                                }
                            )


                        }
                    }
                }
            }

            // --- Appearance ---
            item {
                SettingsGroup(title = "Appearance", icon = Icons.Outlined.Palette) {
                    // Theme Mode
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeChip(
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            label = "System",
                            icon = Icons.Outlined.Android,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                        )
                        ThemeChip(
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            label = "Light",
                            icon = Icons.Outlined.LightMode,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                        )
                        ThemeChip(
                            selected = settings.themeMode == ThemeMode.DARK,
                            label = "Dark",
                            icon = Icons.Outlined.DarkMode,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                        )
                    }
                    // Theme Mode
                    Text(
                        "UI Mode",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    val currentUiMode = settings.uiMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeChip(
                            selected = currentUiMode == UiMode.MATERIAL,
                            label = "Material",
                            icon = Icons.Outlined.Android,
                            onClick = { viewModel.setUiMode(UiMode.MATERIAL) }
                        )
                        ThemeChip(
                            selected = currentUiMode == UiMode.LIQUID_GLASS,
                            label = "Glassy",
                            icon = Icons.Outlined.AutoAwesome,
                            onClick = { viewModel.setUiMode(UiMode.LIQUID_GLASS) }
                        )
                    }


                    // Dynamic Color
                    SettingsSwitch(
                        title = "Dynamic Color",
                        subtitle = "Match system colors (Android 12+)",
                        icon = Icons.Outlined.ColorLens,
                        checked = settings.useDynamicColor,
                        onCheckedChange = { viewModel.setUseDynamicColor(it) }
                    )

                    if (!settings.useDynamicColor) {
                        SettingsColorPicker(
                            selectedColor = settings.accentColor,
                            onColorSelected = { viewModel.setAccentColor(it) }
                        )
                    }
                }
            }

            // --- Gallery View ---
            item {
                SettingsGroup(title = "Gallery View", icon = Icons.Outlined.GridView) {
                    SettingsSwitch(
                        title = "Staggered Layout",
                        subtitle = "Use masonry style grid",
                        icon = Icons.Outlined.DashboardCustomize, // Or ViewQuilt
                        checked = settings.galleryViewType == GalleryViewType.STAGGERED,
                        onCheckedChange = {
                            viewModel.setGalleryViewType(if (it) GalleryViewType.STAGGERED else GalleryViewType.GRID)
                        }
                    )

                    SettingsSlider(
                        title = "Items per Row",
                        value = settings.galleryGridCount.toFloat(),
                        range = 2f..5f,
                        steps = 2,
                        icon = Icons.Outlined.GridOn,
                        onValueChange = { viewModel.setGalleryGridCount(it.roundToInt()) }
                    )

                    SettingsSlider(
                        title = "Corner Radius",
                        value = settings.galleryCornerRadius.toFloat(),
                        range = 0f..32f,
                        steps = 0, // Continuous
                        icon = Icons.Outlined.RoundedCorner,
                        onValueChange = { viewModel.setGalleryCornerRadius(it.roundToInt()) }
                    )
                }
            }

            // --- Albums List ---
            item {
                SettingsGroup(title = "Albums List", icon = Icons.Outlined.PhotoAlbum) {
                    SettingsSlider(
                        title = "Items per Row",
                        value = settings.albumGridCount.toFloat(),
                        range = 1f..4f,
                        steps = 2,
                        icon = Icons.Outlined.GridOn,
                        onValueChange = { viewModel.setAlbumGridCount(it.roundToInt()) }
                    )

                    SettingsSlider(
                        title = "Corner Radius",
                        value = settings.albumCornerRadius.toFloat(),
                        range = 0f..32f,
                        steps = 0,
                        icon = Icons.Outlined.RoundedCorner,
                        onValueChange = { viewModel.setAlbumCornerRadius(it.roundToInt()) }
                    )

                    SettingsSwitch(
                        title = "Show Media Count",
                        subtitle = "Display count on album cards",
                        icon = Icons.Outlined.Numbers,
                        checked = settings.showMediaCount,
                        onCheckedChange = { viewModel.setShowMediaCount(it) }
                    )
                }
            }

            // --- Album Content ---
            item {
                 SettingsGroup(title = "Album Content", icon = Icons.Outlined.DashboardCustomize) {
                    SettingsSwitch(
                        title = "Staggered Layout",
                        subtitle = "Use masonry style grid inside albums",
                        icon = Icons.Outlined.DashboardCustomize,
                        checked = settings.albumDetailViewType == GalleryViewType.STAGGERED,
                        onCheckedChange = {
                            viewModel.setAlbumDetailViewType(if (it) GalleryViewType.STAGGERED else GalleryViewType.GRID)
                        }
                    )

                    SettingsSlider(
                        title = "Items per Row",
                        value = settings.albumDetailGridCount.toFloat(),
                        range = 2f..5f,
                        steps = 2,
                        icon = Icons.Outlined.GridOn,
                        onValueChange = { viewModel.setAlbumDetailGridCount(it.roundToInt()) }
                    )

                    SettingsSlider(
                        title = "Corner Radius",
                        value = settings.albumDetailCornerRadius.toFloat(),
                        range = 0f..32f,
                        steps = 0,
                        icon = Icons.Outlined.RoundedCorner,
                        onValueChange = { viewModel.setAlbumDetailCornerRadius(it.roundToInt()) }
                    )
                 }
            }

            // --- Media & Playback ---
            item {
                SettingsGroup(title = "Playback & Media", icon = Icons.Outlined.PlayCircle) {

                    SettingsSwitch(
                        title = "Max Brightness",
                        subtitle = "Force max brightness in viewer",
                        icon = Icons.Outlined.BrightnessHigh,
                        checked = settings.maxBrightness,
                        onCheckedChange = { viewModel.setMaxBrightness(it) }
                    )
                }
            }

            // --- Data & About ---
            item {
                SettingsGroup(title = "Data Management", icon = Icons.Outlined.Storage) {
                    SettingsSwitch(
                        title = "Enable Recycle Bin",
                        subtitle = "Move items to bin before permanent deletion",
                        icon = Icons.Default.Restore, // Using Restore icon to signify safety
                        checked = settings.trashEnabled,
                        onCheckedChange = { viewModel.setTrashEnabled(it) }
                    )

                    SettingsItem(
                        title = "Open Recycle Bin",
                        subtitle = "Manage deleted items",
                        icon = Icons.Outlined.Delete,
                        onClick = { navController.navigate(Screen.RecycleBin.route) }
                    )
                }
            }

        }
    }
}


// --- Components ---

@Composable
fun SettingsGroup(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }



        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp, 
            border = false // Or true if desired
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                content()
            }
        }
    }
}


@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    icon: ImageVector,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = value.toInt().toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.padding(start = 40.dp)
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun ThemeChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.height(40.dp)
    )
}

@Composable
fun SettingsColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Accent Color", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val colors = listOf(
                0xFF304FFEL, // Royal Indigo
                0xFFC51162L, // Magenta Rose
                0xFF009688L, // Deep Teal
                0xFFFF6D00L, // Blaze Orange
                0xFF455A64L  // Midnight Slate
            )
            colors.forEach { colorVal ->
                val isSelected = selectedColor == colorVal
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(colorVal.toInt()))
                        .clickable { onColorSelected(colorVal) }
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
