package com.irah.galleria.ui.settings
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.ColorLens
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
import androidx.compose.material.icons.outlined.SwipeVertical
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
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
import com.irah.galleria.domain.model.BackgroundAnimationType
import com.irah.galleria.domain.model.GalleryViewType
import com.irah.galleria.domain.model.ThemeMode
import com.irah.galleria.domain.model.UiMode
import com.irah.galleria.ui.LocalBottomBarVisibility
import com.irah.galleria.ui.navigation.Screen
import com.irah.galleria.ui.theme.GlassScaffold
import com.irah.galleria.ui.theme.GlassSurface
import java.util.Locale.getDefault
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt

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
                if (available.y < -15) {
                    if (bottomBarVisibility.value) bottomBarVisibility.value = false
                } else if (available.y > 15) {
                    if (!bottomBarVisibility.value) bottomBarVisibility.value = true
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
                navigationIcon = {
                    Icon(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).padding(start = 12.dp),
                        tint = Color.Unspecified
                    )
                },
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
            item {
                SettingsGroup(title = "Developed By", icon = Icons.Outlined.CodeOff) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    SettingsItemSurface(position = SettingsItemPosition.SINGLE) {
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
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SocialIcon(
                                icon = ImageVector.vectorResource(id = R.drawable.ic_linkedin),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, getString(context, R.string.linkedin_url).toUri())
                                    context.startActivity(intent)
                                }
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            SocialIcon(
                                icon = ImageVector.vectorResource(id = R.drawable.ic_github),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, getString(context, R.string.github_url).toUri())
                                    context.startActivity(intent)
                                }
                            )
                            Spacer(modifier = Modifier.width(24.dp))
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
            }
            item {
                SettingsGroup(title = "Appearance", icon = Icons.Outlined.Palette) {
                    SettingsItemSurface(position = SettingsItemPosition.FIRST) {
                        Column {
                            Text(
                                "Theme",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 8.dp),
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
                        }
                    }
                    val currentUiMode = settings.uiMode
                    SettingsItemSurface(position = SettingsItemPosition.MIDDLE) {
                        Column {
                            Text(
                                "UI Mode",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeChip(
                                    selected = currentUiMode == UiMode.LIQUID_GLASS,
                                    label = "Glassy",
                                    icon = Icons.Outlined.AutoAwesome,
                                    onClick = { viewModel.setUiMode(UiMode.LIQUID_GLASS) }
                                )
                                ThemeChip(
                                    selected = currentUiMode == UiMode.MATERIAL,
                                    label = "Material",
                                    icon = Icons.Outlined.Android,
                                    onClick = { viewModel.setUiMode(UiMode.MATERIAL) }
                                )
                            }
                        }
                    }

                    if (currentUiMode == UiMode.LIQUID_GLASS) {
                        SettingsItemSurface(position = SettingsItemPosition.MIDDLE) {
                            Column {
                                Text(
                                    "Background Style",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp)
                                )
                                val animations = BackgroundAnimationType.entries.toTypedArray()
                                androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            items(animations.size) { index ->
                                val type = animations[index]
                                val isSelected = settings.blobAnimation == type
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { viewModel.setBlobAnimation(type) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp, 120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha=0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        val isDark = com.irah.galleria.ui.theme.LocalIsDarkTheme.current
                                        GlassSurface(
                                            modifier = Modifier.fillMaxSize(),
                                            border = false,
                                            color = Color.Transparent
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))) {
                                                when(type) {
                                                    BackgroundAnimationType.BLOB -> com.irah.galleria.ui.theme.AnimatedBlob(isDark)
                                                    BackgroundAnimationType.WAVE -> com.irah.galleria.ui.theme.AnimatedWave(isDark)
                                                    BackgroundAnimationType.GRADIENT -> com.irah.galleria.ui.theme.AnimatedGradient(isDark)
                                                    BackgroundAnimationType.PARTICLES -> com.irah.galleria.ui.theme.AnimatedParticles(isDark)
                                                    BackgroundAnimationType.MESH -> com.irah.galleria.ui.theme.AnimatedMesh(isDark)
                                                    BackgroundAnimationType.AURORA -> com.irah.galleria.ui.theme.AnimatedAurora(isDark)
                                                    BackgroundAnimationType.SPEED -> com.irah.galleria.ui.theme.AnimatedSpeed(isDark)
                                                    BackgroundAnimationType.CONSTELLATION -> com.irah.galleria.ui.theme.AnimatedConstellation(isDark)
                                                }
                                            }
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(20.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = type.name.lowercase().replaceFirstChar {
                                            if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                            }
                        }
                    }
                    SettingsSwitch(
                        title = "Dynamic Color",
                        subtitle = "Match system colors (Android 12+)",
                        icon = Icons.Outlined.ColorLens,
                        checked = settings.useDynamicColor,
                        position = if (settings.useDynamicColor) SettingsItemPosition.LAST else SettingsItemPosition.MIDDLE,
                        onCheckedChange = { viewModel.setUseDynamicColor(it) }
                    )
                    if (!settings.useDynamicColor) {
                        SettingsItemSurface(position = SettingsItemPosition.LAST) {
                            SettingsColorPicker(
                                selectedColor = settings.accentColor,
                                onColorSelected = { viewModel.setAccentColor(it) }
                            )
                        }
                    }
                }
            }
            item {
                SettingsGroup(title = "Gallery View", icon = Icons.Outlined.GridView) {
                    SettingsSwitch(
                        title = "Staggered Layout",
                        subtitle = "Use masonry style grid",
                        icon = Icons.Outlined.DashboardCustomize,  
                        checked = settings.galleryViewType == GalleryViewType.STAGGERED,
                        position = SettingsItemPosition.FIRST,
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
                        position = SettingsItemPosition.MIDDLE,
                        onValueChange = { viewModel.setGalleryGridCount(it.roundToInt()) }
                    )

                    SettingsSlider(
                        title = "Corner Radius",
                        value = settings.galleryCornerRadius.toFloat(),
                        range = 0f..32f,
                        steps = 0,  
                        icon = Icons.Outlined.RoundedCorner,
                        position = SettingsItemPosition.LAST,
                        onValueChange = { viewModel.setGalleryCornerRadius(it.roundToInt()) }
                    )
                }
            }
            item {
                SettingsGroup(title = "Albums List", icon = Icons.Outlined.PhotoAlbum) {
                    SettingsSlider(
                        title = "Items per Row",
                        value = settings.albumGridCount.toFloat(),
                        range = 2f..4f,
                        steps = 1,
                        icon = Icons.Outlined.GridOn,
                        position = SettingsItemPosition.FIRST,
                        onValueChange = { viewModel.setAlbumGridCount(it.roundToInt()) }
                    )
                    SettingsSlider(
                        title = "Corner Radius",
                        value = settings.albumCornerRadius.toFloat(),
                        range = 0f..32f,
                        steps = 0,
                        icon = Icons.Outlined.RoundedCorner,
                        position = SettingsItemPosition.MIDDLE,
                        onValueChange = { viewModel.setAlbumCornerRadius(it.roundToInt()) }
                    )
                    SettingsSwitch(
                        title = "Show Media Count",
                        subtitle = "Display count on album cards",
                        icon = Icons.Outlined.Numbers,
                        checked = settings.showMediaCount,
                        position = SettingsItemPosition.LAST,
                        onCheckedChange = { viewModel.setShowMediaCount(it) }
                    )
                }
            }
            item {
                 SettingsGroup(title = "Album Content", icon = Icons.Outlined.DashboardCustomize) {
                    SettingsSwitch(
                        title = "Staggered Layout",
                        subtitle = "Use masonry style grid inside albums",
                        icon = Icons.Outlined.DashboardCustomize,
                        checked = settings.albumDetailViewType == GalleryViewType.STAGGERED,
                        position = SettingsItemPosition.FIRST,
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
                        position = SettingsItemPosition.MIDDLE,
                        onValueChange = { viewModel.setAlbumDetailGridCount(it.roundToInt()) }
                    )
                     SettingsSlider(
                        title = "Corner Radius",
                        value = settings.albumDetailCornerRadius.toFloat(),
                        range = 0f..32f,
                        steps = 0,
                        icon = Icons.Outlined.RoundedCorner,
                        position = SettingsItemPosition.LAST,
                        onValueChange = { viewModel.setAlbumDetailCornerRadius(it.roundToInt()) }
                    )
                 }
            }
            item {
                SettingsGroup(title = "Playback & Media", icon = Icons.Outlined.PlayCircle) {
                    SettingsSwitch(
                        title = "Max Brightness",
                        subtitle = "Force max brightness in viewer",
                        icon = Icons.Outlined.BrightnessHigh,
                        checked = settings.maxBrightness,
                        position = SettingsItemPosition.FIRST,
                        onCheckedChange = { viewModel.setMaxBrightness(it) }
                    )
                    SettingsSwitch(
                        title = "Vertical Swipe",
                        subtitle = "Swipe up/down instead of left/right for Media files",
                        icon = Icons.Outlined.SwipeVertical,
                        checked = settings.verticalSwipe,
                        position = SettingsItemPosition.LAST,
                        onCheckedChange = { viewModel.setVerticalSwipe(it) }
                    )
                }
            }
            item {
                SettingsGroup(title = "Data Management", icon = Icons.Outlined.Storage) {
                    SettingsSwitch(
                        title = "Enable Recycle Bin",
                        subtitle = "Move items to bin before permanent deletion",
                        icon = Icons.Default.Restore,  
                        checked = settings.trashEnabled,
                        position = SettingsItemPosition.FIRST,
                        onCheckedChange = { viewModel.setTrashEnabled(it) }
                    )
                    SettingsItem(
                        title = "Open Recycle Bin",
                        subtitle = "Manage deleted items",
                        icon = Icons.Outlined.Delete,
                        position = SettingsItemPosition.LAST,
                        onClick = { navController.navigate(Screen.RecycleBin.route) }
                    )
                }
            }
        }
    }
}
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            content()
        }
    }
}
@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    position: SettingsItemPosition = SettingsItemPosition.SINGLE,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItemSurface(position = position) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
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
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    icon: ImageVector,
    position: SettingsItemPosition = SettingsItemPosition.SINGLE,
    onValueChange: (Float) -> Unit
) {
    SettingsItemSurface(position = position) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
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
}
@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    position: SettingsItemPosition = SettingsItemPosition.SINGLE,
    onClick: () -> Unit
) {
    SettingsItemSurface(position = position) {
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
}

@Composable
fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onValueChange: (Float, Float) -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures  { change, _ ->
                    val x = (change.position.x / size.width).coerceIn(0f, 1f)
                    val y = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange(x, y)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = (offset.x / size.width).coerceIn(0f, 1f)
                    val y = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onValueChange(x, y)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Draw Hue background
            drawRect(color = Color.hsv(hue, 1f, 1f))

            // 2. Clear horizontal white-to-transparent gradient (Saturation)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(w, 0f)
                )
            )

            // 3. Draw vertical transparent-to-black gradient (Value)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    start = Offset(0f, 0f),
                    end = Offset(0f, h)
                )
            )

            // 4. Draw selector
            val selectorX = saturation * w
            val selectorY = (1f - value) * h
            
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = 9.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun HueBar(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val h = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                    onHueChange(h)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val h = (offset.x / size.width).coerceIn(0f, 1f) * 360f
                    onHueChange(h)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            val hueColors = (0..360 step 60).map { Color.hsv(it.toFloat(), 1f, 1f) }
            
            drawRect(
                brush = Brush.linearGradient(
                    colors = hueColors,
                    start = Offset(0f, 0f),
                    end = Offset(w, 0f)
                )
            )
            
            val selectorX = (hue / 360f) * w
            drawCircle(
                color = Color.White,
                radius = h / 2f,
                center = Offset(selectorX, h / 2f),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColor: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val hsv = remember { 
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toInt(), hsv)
        mutableStateOf(Triple(hsv[0], hsv[1], hsv[2]))
    }
    
    val (h, s, v) = hsv.value
    val currentColor = remember(h, s, v) { Color.hsv(h, s, v) }
    
    // Hex text field state
    var hexText by remember(currentColor) { 
        mutableStateOf(String.format("%06X", (currentColor.toArgb() and 0xFFFFFF)))
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Custom Color", style = MaterialTheme.typography.headlineSmall)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SaturationValuePanel(
                    hue = h,
                    saturation = s,
                    value = v,
                    onValueChange = { newS, newV -> hsv.value = Triple(h, newS, newV) }
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hue", style = MaterialTheme.typography.labelMedium)
                    HueBar(
                        hue = h,
                        onHueChange = { newH -> hsv.value = Triple(newH, s, v) }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = hexText,
                        onValueChange = { newText ->
                            if (newText.length <= 6) {
                                hexText = newText.uppercase()
                                try {
                                    if (newText.length == 6) {
                                        val colorInt = "#$newText".toColorInt()
                                        val newHsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(colorInt, newHsv)
                                        hsv.value = Triple(newHsv[0], newHsv[1], newHsv[2])
                                    }
                                } catch (e: Exception) {
                                    // Invalid hex
                                }
                            }
                        },
                        label = { Text("Hex") },
                        prefix = { Text("#") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "H: ${h.toInt()}°",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "S: ${(s * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "V: ${(v * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    onColorSelected(currentColor.toArgb().toLong() and 0xFFFFFFFFL)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Select Color")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    var showCustomPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Accent Color", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val colors = listOf(
                0xFF4338CAL, // Deep Indigo
                0xFFE11D48L, // Rose Gold
                0xFF059669L, // Emerald Green
                0xFF475569L, // Slate Blue
                0xFFD97706L  // Amber
            )
            colors.forEach { colorVal ->
                val isSelected = selectedColor == colorVal
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Custom Color Picker Option
            val isCustomSelected = !colors.contains(selectedColor)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isCustomSelected) Color(selectedColor.toInt()) else Color.Transparent)
                    .border(
                        width = if (isCustomSelected) 2.dp else 1.dp,
                        color = if (isCustomSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha=0.5f),
                        shape = CircleShape
                    )
                    .clickable { showCustomPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = "Custom Color",
                    tint = if (isCustomSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColor = selectedColor,
            onColorSelected = {
                onColorSelected(it)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false }
        )
    }
}

enum class SettingsItemPosition {
    FIRST, MIDDLE, LAST, SINGLE
}

@Composable
fun getSettingsShape(position: SettingsItemPosition): androidx.compose.ui.graphics.Shape {
    val large = 28.dp
    val small = 4.dp
    return when (position) {
        SettingsItemPosition.FIRST -> RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
        SettingsItemPosition.MIDDLE -> RoundedCornerShape(small)
        SettingsItemPosition.LAST -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
        SettingsItemPosition.SINGLE -> RoundedCornerShape(large)
    }
}

@Composable
fun SettingsItemSurface(
    position: SettingsItemPosition,
    content: @Composable () -> Unit
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = getSettingsShape(position),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = false
    ) {
        content()
    }
}