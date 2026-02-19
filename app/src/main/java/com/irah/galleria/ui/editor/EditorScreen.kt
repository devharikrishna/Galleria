package com.irah.galleria.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun EditorScreen(
    navController: NavController,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var isComparing by remember { mutableStateOf(false) }
    
    // Hoisted state for smooth 60fps Straighten preview
    var tempStraightenDegrees by remember(state.adjustments.straightenDegrees) { mutableFloatStateOf(state.adjustments.straightenDegrees) }
    var showExitDialog by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler {
        showExitDialog = true
    }

    
    if (showExitDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Discard Edits?") },
            text = { Text("Are you sure you want to exit? You will lose any unsaved changes.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        navController.popBackStack()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    Scaffold(
        topBar = {
            EditorTopBar(
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                onCancel = { showExitDialog = true },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onSave = {
                    viewModel.saveImage { newId ->
                         if (newId != null) {
                             navController.navigate("media_viewer_screen/$newId") {
                                 popUpTo("editor_screen/{mediaId}") { inclusive = true }
                             }
                         } else {
                             navController.popBackStack()
                         }
                    }
                }
            )
        },
        bottomBar = {
            EditorBottomBarNested(
                state = state,
                onToolSelect = { viewModel.setTool(it) },
                onAutoVariantSelect = { viewModel.applyAutoVariant(it) },
                onAdjustmentChange = { viewModel.onAdjustmentChange(it) },
                onAdjustmentCommit = { viewModel.commitAdjustment() },
                onSetAspectRatio = { viewModel.setAspectRatio(it) },
                tempStraighten = tempStraightenDegrees,
                onTempStraightenChange = { tempStraightenDegrees = it },
                onToggleBackgroundRemove = { viewModel.toggleBackgroundRemove() },
                onSetBackgroundBlur = { viewModel.setBackgroundBlur(it) },
                onCommitBackgroundBlur = { viewModel.commitBackgroundBlur() },
                onCurveChannelChange = { viewModel.setActiveCurveChannel(it) },
                onCurvePointsChange = { viewModel.setCurvePoints(it) }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } 
            
            if (!state.isLoading) {
                val bitmapToShow = if (isComparing) state.originalBitmap else state.previewBitmap
                
                if (bitmapToShow != null) {
                    androidx.compose.foundation.layout.BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        
                        val imageWidth = bitmapToShow.width.toFloat()
                        val imageHeight = bitmapToShow.height.toFloat()
                        
                        val viewWidth = constraints.maxWidth.toFloat()
                        val viewHeight = constraints.maxHeight.toFloat()
                        
                        val scaleFactor = kotlin.math.min(viewWidth / imageWidth, viewHeight / imageHeight)
                        
                        val displayWidth = imageWidth * scaleFactor
                        val displayHeight = imageHeight * scaleFactor
                        
                        val offsetX = (viewWidth - displayWidth) / 2f
                        val offsetY = (viewHeight - displayHeight) / 2f
                        
                        val imageBounds = androidx.compose.ui.geometry.Rect(
                            offsetX, offsetY, offsetX + displayWidth, offsetY + displayHeight
                        )

                        var scale by remember { mutableFloatStateOf(1f) }
                        var offset by remember { mutableStateOf(Offset.Zero) }
                        
                        val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                             if (viewModel.state.value.activeTool != EditorTool.CROP) {
                                 scale = (scale * zoomChange).coerceIn(1f, 5f)
                                 offset += offsetChange * scale
                             }
                        }
                        
                         Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isComparing = true
                                            tryAwaitRelease()
                                            isComparing = false
                                        }
                                    )
                                }
                                .then(
                                    if (state.activeTool != EditorTool.CROP) {
                                         Modifier.transformable(state = transformState)
                                    } else Modifier
                                )
                                .graphicsLayer {
                                    val zoomNum = BitmapUtils.calculateAutoZoomScale(bitmapToShow.width, bitmapToShow.height, tempStraightenDegrees)
                                    val zoomDenominator = BitmapUtils.calculateAutoZoomScale(bitmapToShow.width, bitmapToShow.height, state.adjustments.straightenDegrees)
                                    val safeDenominator = if (zoomDenominator == 0f) 1f else zoomDenominator
                                    val autoZoom = zoomNum / safeDenominator
                                    
                                    rotationZ = (tempStraightenDegrees - state.adjustments.straightenDegrees)
                                    scaleX = scale * autoZoom
                                    scaleY = scale * autoZoom
                                    
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        ) {
                            Image(
                                bitmap = bitmapToShow.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        if (state.activeTool == EditorTool.CROP) {
                             LaunchedEffect(Unit) {
                                 if (scale != 1f) {
                                     scale = 1f
                                     offset = Offset.Zero
                                 }
                             }
                             
                             CropOverlay(
                                 modifier = Modifier.fillMaxSize(),
                                 imageBounds = imageBounds,
                                 adjustments = state.adjustments,
                                 onCropChange = { rect ->
                                     viewModel.onAdjustmentChange(state.adjustments.copy(cropRect = rect))
                                 },
                                 onCommit = { viewModel.commitAdjustment() },
                                 fixedAspectRatio = state.aspectRatio
                             )
                        }

                        if (isComparing) {
                             Text(
                                "Original", 
                                color = Color.White, 
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .background(Color.Black.copy(alpha=0.6f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            
            if (state.isSaving) {
                val progress by viewModel.saveProgress.collectAsState()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        Text("Saving...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.width(200.dp),
                            color = ProgressIndicatorDefaults.linearColor,
                            trackColor = ProgressIndicatorDefaults.linearTrackColor,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${(progress * 100).toInt()}%", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onCancel: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
        }
        Row {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if(canUndo) Color.White else Color.Gray)
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = if(canRedo) Color.White else Color.Gray)
            }
            TextButton(onClick = onSave) {
                Text("Save", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun EditorBottomBarNested(
    state: EditorState,
    onToolSelect: (EditorTool) -> Unit,
    onAutoVariantSelect: (AutoEnhanceVariant) -> Unit,
    onAdjustmentChange: (BitmapUtils.Adjustments) -> Unit,
    onAdjustmentCommit: () -> Unit,
    onSetAspectRatio: (Float?) -> Unit,
    tempStraighten: Float,
    onTempStraightenChange: (Float) -> Unit,
    onToggleBackgroundRemove: () -> Unit,
    onSetBackgroundBlur: (Float) -> Unit,
    onCommitBackgroundBlur: () -> Unit,
    onCurveChannelChange: (CurveChannel) -> Unit,
    onCurvePointsChange: (List<Pair<Float, Float>>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .navigationBarsPadding()
    ) {
        when (state.activeTool) {
            EditorTool.LIGHT -> {
                AdjustmentSlidersGroup(
                    tool = EditorTool.LIGHT,
                    adjustments = state.adjustments,
                    onAdjustmentChange = onAdjustmentChange,
                    onCommit = onAdjustmentCommit
                )
                HorizontalDivider(color = Color.DarkGray)
            }
            EditorTool.COLOR -> {
                AdjustmentSlidersGroup(
                    tool = EditorTool.COLOR,
                    adjustments = state.adjustments,
                    onAdjustmentChange = onAdjustmentChange,
                    onCommit = onAdjustmentCommit
                )
                HorizontalDivider(color = Color.DarkGray)
            }
            EditorTool.DETAIL -> {
                 AdjustmentSlidersGroup(
                    tool = EditorTool.DETAIL,
                    adjustments = state.adjustments,
                    onAdjustmentChange = onAdjustmentChange,
                    onCommit = onAdjustmentCommit
                )
                HorizontalDivider(color = Color.DarkGray)
            }
            EditorTool.HSL -> {
                 AdjustmentSlidersGroup(
                    tool = EditorTool.HSL,
                    adjustments = state.adjustments,
                    onAdjustmentChange = onAdjustmentChange,
                    onCommit = onAdjustmentCommit
                )
                HorizontalDivider(color = Color.DarkGray)
            }
            EditorTool.CROP -> {
                 Row(
                     modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                     horizontalArrangement = Arrangement.SpaceEvenly,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         IconButton(onClick = {
                             val current = state.adjustments.rotationDegrees
                             onAdjustmentChange(state.adjustments.copy(rotationDegrees = (current + 90f) % 360f))
                             onAdjustmentCommit()
                         }) {
                             Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", tint = Color.White)
                         }
                         Text("Rotate", style = MaterialTheme.typography.labelSmall, color = Color.White)
                     }
                     
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         IconButton(onClick = {
                             val current = state.adjustments.flipHorizontal
                             onAdjustmentChange(state.adjustments.copy(flipHorizontal = !current))
                             onAdjustmentCommit()
                         }) {
                             Icon(Icons.Default.SwapHoriz, contentDescription = "Flip H", tint = Color.White)
                         }
                         Text("Flip H", style = MaterialTheme.typography.labelSmall, color = Color.White)
                     }

                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         IconButton(onClick = {
                             val current = state.adjustments.flipVertical
                             onAdjustmentChange(state.adjustments.copy(flipVertical = !current))
                             onAdjustmentCommit()
                         }) {
                             Icon(Icons.Default.SwapVert, contentDescription = "Flip V", tint = Color.White)
                         }
                         Text("Flip V", style = MaterialTheme.typography.labelSmall, color = Color.White)
                     }
                 }
                 
                 Column(
                     modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                 ) {
                     Row(
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.SpaceBetween,
                         modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                     ) {
                         Text("Straighten", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(70.dp))
                         
                         EditorSlider(
                             value = tempStraighten,
                             onValueChange = { 
                                 onTempStraightenChange(it)
                             },
                             valueRange = -45f..45f,
                             onValueChangeFinished = { 
                                 onAdjustmentChange(state.adjustments.copy(straightenDegrees = tempStraighten))
                                 onAdjustmentCommit() 
                             },
                             modifier = Modifier.weight(1f)
                         )
                         
                         Text("${tempStraighten.toInt()}°", color = Color.LightGray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                     }
                 }
                 HorizontalDivider(color = Color.DarkGray)
                 
                 LazyRow(
                     modifier = Modifier.fillMaxWidth().padding(16.dp),
                     horizontalArrangement = Arrangement.spacedBy(12.dp),
                     contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                 ) {
                     val setRatio = { ratio: Float? -> onSetAspectRatio(ratio) }
                     
                     item { CropRatioButton("Free") { setRatio(null) } }
                     item { 
                         CropRatioButton("Original") { 
                             val original = state.originalBitmap
                             if (original != null) {
                                 setRatio(original.width.toFloat() / original.height.toFloat())
                             }
                         } 
                     }
                     item { CropRatioButton("Square") { setRatio(1f) } }
                     item { CropRatioButton("3:4") { setRatio(3f/4f) } }
                     item { CropRatioButton("4:3") { setRatio(4f/3f) } }
                     item { CropRatioButton("16:9") { setRatio(16f/9f) } }
                 }
                 HorizontalDivider(color = Color.DarkGray)
            }
            EditorTool.FILTER -> {
                 Column {
                     // Strength Slider (Only if filter is selected)
                     if (state.adjustments.filter != FilterType.NONE) {
                         Row(
                             modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text("Strength", color = Color.LightGray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                             EditorSlider(
                                 value = state.adjustments.filterStrength,
                                 onValueChange = { onAdjustmentChange(state.adjustments.copy(filterStrength = it)) },
                                 valueRange = 0f..1f,
                                 onValueChangeFinished = onAdjustmentCommit,
                                 modifier = Modifier.weight(1f)
                             )
                             Text(
                                 "${(state.adjustments.filterStrength * 100).toInt()}", 
                                 color = Color.White, 
                                 style = MaterialTheme.typography.bodySmall, 
                                 modifier = Modifier.width(30.dp)
                             )
                         }
                     }
                 
                     LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                    ) {
                        items(FilterType.entries.toTypedArray()) { filter ->
                            FilterButton(
                                filter = filter, 
                                isSelected = state.adjustments.filter == filter,
                                previewBitmap = state.originalBitmap, 
                                onClick = {
                                    onAdjustmentChange(state.adjustments.copy(filter = filter))
                                    if (state.adjustments.filter == FilterType.NONE && filter != FilterType.NONE) {
                                         onAdjustmentChange(state.adjustments.copy(filter = filter, filterStrength = 1f))
                                    } else {
                                         onAdjustmentChange(state.adjustments.copy(filter = filter))
                                    }
                                    onAdjustmentCommit()
                                }
                            )
                        }
                    }
                 }
                 HorizontalDivider(color = Color.DarkGray)
            }
            EditorTool.AUTO_ENHANCE -> {
                 LazyRow(
                     modifier = Modifier.fillMaxWidth().padding(16.dp),
                     horizontalArrangement = Arrangement.SpaceEvenly,
                     contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                 ) {
                     items(AutoEnhanceVariant.entries.toTypedArray()) { variant ->
                         val isSelected = state.activeAutoVariant == variant
                         val icon = when (variant) {
                             AutoEnhanceVariant.NONE -> Icons.Default.Block
                             AutoEnhanceVariant.BALANCED -> Icons.Default.AutoFixNormal
                             AutoEnhanceVariant.WARM -> Icons.Default.WbSunny
                             AutoEnhanceVariant.COOL -> Icons.Default.AcUnit
                             AutoEnhanceVariant.VIVID -> Icons.Default.Palette
                         }
                         
                         val label = variant.name.lowercase()
                             .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                         ToolButton(
                             icon = icon, 
                             label = label, 
                             isSelected = isSelected, 
                             onClick = { onAutoVariantSelect(variant) }
                         )
                     }
                 }
                 HorizontalDivider(color = Color.DarkGray)
            }
            EditorTool.BACKGROUND -> {
                 Column(
                     modifier = Modifier.fillMaxWidth().padding(16.dp),
                     horizontalAlignment = Alignment.CenterHorizontally,
                     verticalArrangement = Arrangement.spacedBy(16.dp)
                 ) {
                     val isBlurActive = state.adjustments.backgroundMode == BitmapUtils.BackgroundMode.BLUR
                     val currentBlur = if (isBlurActive) state.adjustments.backgroundBlurRadius else 0f
                     
                     Text("Blur Background", style = MaterialTheme.typography.bodyMedium, color = if(isBlurActive) MaterialTheme.colorScheme.primary else Color.LightGray)
                     
                     Row(
                         verticalAlignment = Alignment.CenterVertically,
                         modifier = Modifier.fillMaxWidth()
                     ) {
                         Text("Strength", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.width(60.dp))
                         EditorSlider(
                             value = currentBlur,
                             onValueChange = { onSetBackgroundBlur(it) },
                             onValueChangeFinished = { onCommitBackgroundBlur() },
                             valueRange = 0f..0.05f,
                             modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                         )
                         Text("${(currentBlur*200).toInt()}", style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.width(40.dp))
                     }
                     
                     if (state.isLoading) {
                         Text("Processing Segmentation...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                     }
                 }
                 HorizontalDivider(color = Color.DarkGray)
            }
            EditorTool.CURVES -> {
                 CurvesTool(
                     activeChannel = state.activeCurveChannel,
                     points = when(state.activeCurveChannel) {
                         CurveChannel.RGB -> state.adjustments.curveRGB
                         CurveChannel.RED -> state.adjustments.curveRed
                         CurveChannel.GREEN -> state.adjustments.curveGreen
                         CurveChannel.BLUE -> state.adjustments.curveBlue
                         CurveChannel.LUMINANCE -> state.adjustments.curveLuminance
                     },
                     onChannelChange = onCurveChannelChange,
                     onPointsChange = onCurvePointsChange,
                     onCommit = onAdjustmentCommit
                 )
                 HorizontalDivider(color = Color.DarkGray)
            }

            else -> {}
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
        ) {
            item { ToolButton(icon = Icons.Default.AutoAwesome, label = "Auto", isSelected = false, onClick = { onToolSelect(EditorTool.AUTO_ENHANCE) }) }
            item { ToolButton(Icons.Default.Brightness6, "Light", state.activeTool == EditorTool.LIGHT) { onToolSelect(EditorTool.LIGHT) } }
            item { ToolButton(Icons.Default.ColorLens, "Color", state.activeTool == EditorTool.COLOR) { onToolSelect(EditorTool.COLOR) } }
            item { ToolButton(Icons.Default.Texture, "Detail", state.activeTool == EditorTool.DETAIL) { onToolSelect(EditorTool.DETAIL) } }
            item { ToolButton(Icons.Default.Gradient, "HSL", state.activeTool == EditorTool.HSL) { onToolSelect(EditorTool.HSL) } }
            item { ToolButton(Icons.Default.Crop, "Crop", state.activeTool == EditorTool.CROP) { onToolSelect(EditorTool.CROP) } }
            item { ToolButton(Icons.Default.AutoFixHigh, "Filters", state.activeTool == EditorTool.FILTER) { onToolSelect(EditorTool.FILTER) } }
            item { ToolButton(Icons.Filled.Texture, "Background", state.activeTool == EditorTool.BACKGROUND) { onToolSelect(EditorTool.BACKGROUND) } }
            item { ToolButton(Icons.Default.Gradient, "Curves", state.activeTool == EditorTool.CURVES) { onToolSelect(EditorTool.CURVES) } }
        }
    }
}



@Composable
fun CurvesTool(
    activeChannel: CurveChannel,
    points: List<Pair<Float, Float>>,
    onChannelChange: (CurveChannel) -> Unit,
    onPointsChange: (List<Pair<Float, Float>>) -> Unit,
    onCommit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Channel Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CurveChannel.entries.forEach { channel ->
                val isSelected = activeChannel == channel
                val color = when(channel) {
                    CurveChannel.RGB -> Color.White
                    CurveChannel.RED -> Color(0xFFFF6B6B)
                    CurveChannel.GREEN -> Color(0xFF51CF66)
                    CurveChannel.BLUE -> Color(0xFF339AF0)
                    CurveChannel.LUMINANCE -> Color.Gray
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color.DarkGray else Color.Transparent)
                        .clickable { onChannelChange(channel) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = channel.name,
                        color = if (isSelected) color else Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(250.dp)
                .background(Color(0xFF202020), RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .pointerInput(activeChannel, points) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val hitThreshold = 30f // dp to px ideally, but approx 30px is ok
                            
                            val closestIndex = points.withIndex().minByOrNull { 
                                val px = it.value.first * size.width
                                val py = (1f - it.value.second) * size.height
                                val dx = px - offset.x
                                val dy = py - offset.y
                                dx*dx + dy*dy 
                            }?.index ?: -1
                            
                            if (closestIndex != -1 && closestIndex != 0 && closestIndex != points.size - 1) {
                                val p = points[closestIndex]
                                val px = p.first * size.width
                                val py = (1f - p.second) * size.height
                                val dx = px - offset.x
                                val dy = py - offset.y
                                if (dx*dx + dy*dy < hitThreshold*hitThreshold) {
                                     val newPoints = points.toMutableList()
                                     newPoints.removeAt(closestIndex)
                                     onPointsChange(newPoints)
                                     onCommit()
                                }
                            }
                        },
                        onTap = { offset ->
                            val x = (offset.x / size.width).coerceIn(0f, 1f)
                            val y = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                            val newPoints = points.toMutableList()
                            newPoints.add(x to y)
                            newPoints.sortBy { it.first }
                            onPointsChange(newPoints)
                            onCommit()
                        }
                    )
                }
                .pointerInput(activeChannel, points) {
                    detectDragGestures(
                        onDragEnd = { onCommit() }
                    ) { change, _ ->
                        val pos = change.position
                        val x = (pos.x / size.width).coerceIn(0f, 1f)
                        val y = (1f - (pos.y / size.height)).coerceIn(0f, 1f)
                        
                        val closestIndex = points.withIndex().minByOrNull { 
                            val px = it.value.first * size.width
                            val py = (1f - it.value.second) * size.height
                            val dx = px - pos.x
                            val dy = py - pos.y
                            dx*dx + dy*dy 
                        }?.index ?: -1
                        
                        val hitThreshold = 100f // larger threshold for dragging
                        
                        if (closestIndex != -1) {
                            val p = points[closestIndex]
                            val px = p.first * size.width
                            val py = (1f - p.second) * size.height
                            val dx = px - pos.x
                            val dy = py - pos.y
                            
                            // Only drag if reasonably close
                            if (dx*dx + dy*dy < hitThreshold*hitThreshold) {
                                val newX = if (closestIndex == 0 || closestIndex == points.size-1) p.first else x
                                // Allow Y to go full range
                                val newY = y.coerceIn(0f, 1f)
                                
                                val newPoints = points.toMutableList()
                                if (closestIndex > 0) {
                                     val prev = newPoints[closestIndex-1]
                                     if (newX <= prev.first) return@detectDragGestures 
                                }
                                if (closestIndex < points.size - 1) {
                                     val next = newPoints[closestIndex+1]
                                     if (newX >= next.first) return@detectDragGestures
                                }

                                newPoints[closestIndex] = newX to newY
                                // Simplify by resorting every frame? Might be jumpy but safe
                                newPoints.sortBy { it.first }
                                onPointsChange(newPoints)
                            }
                        }
                    }
                }
        ) {
            val lineColor = when(activeChannel) {
                CurveChannel.RGB -> Color.White
                CurveChannel.RED -> Color.Red
                CurveChannel.GREEN -> Color.Green
                CurveChannel.BLUE -> Color.Blue
                CurveChannel.LUMINANCE -> Color.LightGray
            }
            
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val step = w / 4
                
                // Grid Lines
                for (i in 1..3) {
                    drawLine(Color.Gray.copy(alpha=0.3f), Offset(step * i, 0f), Offset(step * i, h), strokeWidth=1f)
                    drawLine(Color.Gray.copy(alpha=0.3f), Offset(0f, step * i), Offset(w, step * i), strokeWidth=1f)
                }
                
                // Reference Diagonal (0,0) to (1,1) -> Visual (0, h) to (w, 0)
                drawLine(
                    color = Color.Gray.copy(alpha=0.5f),
                    start = Offset(0f, h),
                    end = Offset(w, 0f),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                val sorted = points.sortedBy { it.first }
                if (sorted.isNotEmpty()) {
                    // Calculate Spline Path for UI
                    val path = androidx.compose.ui.graphics.Path()
                    
                    // Same Monotone Cubic Spline logic as BitmapUtils for UI consistency
                    val n = sorted.size
                    if (n > 1) {
                         val xs = FloatArray(n) { sorted[it].first * w }
                         val ys = FloatArray(n) { (1f - sorted[it].second) * h }
                         val delta = FloatArray(n - 1)
                         for (i in 0 until n - 1) {
                             val dx = xs[i+1] - xs[i]
                             val dy = ys[i+1] - ys[i]
                             delta[i] = if (dx == 0f) 0f else dy / dx
                         }
                         val m = FloatArray(n)
                         m[0] = delta[0]
                         m[n-1] = delta[n-2]
                         for (i in 1 until n - 1) m[i] = (delta[i-1] + delta[i]) / 2f
                         
                         if (n > 2) {
                            for (i in 0 until n - 1) {
                                if (delta[i] == 0f) {
                                    m[i] = 0f
                                    m[i+1] = 0f
                                } else {
                                    val alpha = m[i] / delta[i]
                                    val beta = m[i+1] / delta[i]
                                    val dist = alpha * alpha + beta * beta
                                    if (dist > 9f) {
                                        val tau = 3f / kotlin.math.sqrt(dist)
                                        m[i] = tau * alpha * delta[i]
                                        m[i+1] = tau * beta * delta[i]
                                    }
                                }
                            }
                         }
                         
                         path.moveTo(xs[0], ys[0])
                         for (i in 0 until n - 1) {
                             // Draw segments
                             val segments = 20 // Segments per interval
                             val dx = xs[i+1] - xs[i]
                             if (dx > 0) {
                                 for (j in 1..segments) {
                                     val t = j.toFloat() / segments
                                     val t2 = t * t
                                     val t3 = t2 * t
                                     val h00 = 2 * t3 - 3 * t2 + 1
                                     val h10 = t3 - 2 * t2 + t
                                     val h01 = -2 * t3 + 3 * t2
                                     val h11 = t3 - t2
                                     val px = xs[i] + t * dx 
                                     val py = h00 * ys[i] + h10 * dx * m[i] + h01 * ys[i+1] + h11 * dx * m[i+1]
                                     path.lineTo(px, py)
                                 }
                             } else {
                                 path.lineTo(xs[i+1], ys[i+1])
                             }
                         }
                    } else {
                        // fallback single point
                         path.moveTo(sorted[0].first * w, (1f - sorted[0].second) * h)
                         path.lineTo(sorted[0].first * w, (1f - sorted[0].second) * h)
                    }

                    drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                    
                    for (p in sorted) {
                        drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(p.first * w, (1f - p.second) * h))
                        drawCircle(Color.Black, radius = 2.dp.toPx(), center = Offset(p.first * w, (1f - p.second) * h))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Presets
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
        ) {
            item {
                IconButton(onClick = { 
                     onPointsChange(listOf(0f to 0f, 1f to 1f))
                     onCommit()
                 }) {
                     Icon(Icons.Default.Refresh, "Reset", tint = Color.White)
                 }
            }
            item {
                 TextButton(onClick = {
                     onPointsChange(listOf(0f to 0f, 0.25f to 0.20f, 0.75f to 0.80f, 1f to 1f))
                     onCommit()
                 }) { Text("Soft Contrast") }
            }
            item {
                 TextButton(onClick = {
                     onPointsChange(listOf(0f to 0f, 0.25f to 0.15f, 0.75f to 0.85f, 1f to 1f))
                     onCommit()
                 }) { Text("Hard Contrast") }
            }
             item {
                 TextButton(onClick = {
                     onPointsChange(listOf(0f to 0.1f, 0.25f to 0.25f, 0.75f to 0.75f, 1f to 0.9f)) // Matte
                     onCommit()
                 }) { Text("Matte") }
            }
        }
    }
}







@Composable
fun AdjustmentSlidersGroup(
    tool: EditorTool,
    adjustments: BitmapUtils.Adjustments,
    onAdjustmentChange: (BitmapUtils.Adjustments) -> Unit,
    onCommit: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(10.dp)
            .heightIn(max = 180.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            when (tool) {
                EditorTool.LIGHT -> {
                    SliderParam("Exposure", adjustments.exposure, -2f..2f, { onAdjustmentChange(adjustments.copy(exposure = it)) }, onCommit)
                    SliderParam("Brightness", adjustments.brightness, -1f..1f, { onAdjustmentChange(adjustments.copy(brightness = it)) }, onCommit)
                    SliderParam("Contrast", adjustments.contrast, 0.5f..1.5f, { onAdjustmentChange(adjustments.copy(contrast = it)) }, onCommit, defaultValue = 1f)
                    SliderParam("Highlights", adjustments.highlights, -1f..1f, { onAdjustmentChange(adjustments.copy(highlights = it)) }, onCommit)
                    SliderParam("Shadows", adjustments.shadows, -1f..1f, { onAdjustmentChange(adjustments.copy(shadows = it)) }, onCommit)
                    SliderParam("Whites", adjustments.whites, -1f..1f, { onAdjustmentChange(adjustments.copy(whites = it)) }, onCommit)
                    SliderParam("Blacks", adjustments.blacks, -1f..1f, { onAdjustmentChange(adjustments.copy(blacks = it)) }, onCommit)
                }
                EditorTool.COLOR -> {
                    SliderParam("Saturation", adjustments.saturation, 0f..2f, { onAdjustmentChange(adjustments.copy(saturation = it)) }, onCommit, defaultValue = 1f)
                    SliderParam("Vibrance", adjustments.vibrance, -1f..1f, { onAdjustmentChange(adjustments.copy(vibrance = it)) }, onCommit)
                    SliderParam("Skin Tone", adjustments.skinTone, -1f..1f, { onAdjustmentChange(adjustments.copy(skinTone = it)) }, onCommit)
                    SliderParam("Skin Color", adjustments.skinColor, -1f..1f, { onAdjustmentChange(adjustments.copy(skinColor = it)) }, onCommit)
                    SliderParam("Temp", adjustments.temperature, -1f..1f, { onAdjustmentChange(adjustments.copy(temperature = it)) }, onCommit)
                    SliderParam("Tint", adjustments.tint, -1f..1f, { onAdjustmentChange(adjustments.copy(tint = it)) }, onCommit)
                }
                EditorTool.DETAIL -> {
                     SliderParam("Sharpen", adjustments.sharpen, 0f..1f, { onAdjustmentChange(adjustments.copy(sharpen = it)) }, onCommit)
                     SliderParam("Structure", adjustments.clarity, 0f..1f, { onAdjustmentChange(adjustments.copy(clarity = it)) }, onCommit)
                     SliderParam("Vignette", adjustments.vignette, 0f..1f, { onAdjustmentChange(adjustments.copy(vignette = it)) }, onCommit)
                     SliderParam("Denoise", adjustments.denoise, 0f..1f, { onAdjustmentChange(adjustments.copy(denoise = it)) }, onCommit)
                     SliderParam("Blur", adjustments.blur, 0f..1f, { onAdjustmentChange(adjustments.copy(blur = it)) }, onCommit)
                     SliderParam("Dehaze", adjustments.dehaze, 0f..1f, { onAdjustmentChange(adjustments.copy(dehaze = it)) }, onCommit)
                }
                EditorTool.HSL -> {
                    var selectedChannel by remember { mutableStateOf(BitmapUtils.HslChannel.RED) }
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        items(BitmapUtils.HslChannel.entries.toTypedArray()) { channel ->
                             val color = when(channel) {
                                 BitmapUtils.HslChannel.RED -> Color.Red
                                 BitmapUtils.HslChannel.ORANGE -> Color(0xFFFF8000)
                                 BitmapUtils.HslChannel.YELLOW -> Color.Yellow
                                 BitmapUtils.HslChannel.GREEN -> Color.Green
                                 BitmapUtils.HslChannel.AQUA -> Color.Cyan
                                 BitmapUtils.HslChannel.BLUE -> Color.Blue
                                 BitmapUtils.HslChannel.PURPLE -> Color(0xFF8000FF)
                                 BitmapUtils.HslChannel.MAGENTA -> Color.Magenta
                             }
                             
                             Box(
                                 modifier = Modifier
                                     .size(18.dp)
                                     .clip(CircleShape)
                                     .background(color)
                                     .border(
                                         width = if (selectedChannel == channel) 3.dp else 0.dp,
                                         color = if (selectedChannel == channel) Color.White else Color.Transparent,
                                         shape = CircleShape
                                     )
                                     .clickable { selectedChannel = channel }
                             )
                        }
                    }
                    
                    val currentShift = adjustments.hsl[selectedChannel] ?: BitmapUtils.HslShift()
                    
                    SliderParam("Hue", currentShift.hue, -180f..180f, { 
                        val newShift = currentShift.copy(hue = it)
                        val newMap = adjustments.hsl.toMutableMap()
                        newMap[selectedChannel] = newShift
                        onAdjustmentChange(adjustments.copy(hsl = newMap))
                    }, onCommit)
                    
                    SliderParam("Saturation", currentShift.saturation, -1f..1f, {
                        val newShift = currentShift.copy(saturation = it)
                        val newMap = adjustments.hsl.toMutableMap()
                        newMap[selectedChannel] = newShift
                        onAdjustmentChange(adjustments.copy(hsl = newMap))
                    }, onCommit)
                    
                    SliderParam("Luminance", currentShift.luminance, -1f..1f, {
                        val newShift = currentShift.copy(luminance = it)
                        val newMap = adjustments.hsl.toMutableMap()
                        newMap[selectedChannel] = newShift
                        onAdjustmentChange(adjustments.copy(hsl = newMap))
                    }, onCommit)
                }



                else -> {}
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SliderParam(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
    defaultValue: Float = 0f
) {
    val displayValue = if (range.endInclusive - range.start > 2) {
         value.toInt()
    } else {
         (value * 100).toInt()
    }


    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, 
            color = Color.White, 
            modifier = Modifier.width(70.dp), 
            style = MaterialTheme.typography.bodySmall
        )
        
        EditorSlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onCommit,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            "$displayValue",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .width(40.dp)
                .padding(start = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        
        val isChanged = kotlin.math.abs(value - defaultValue) > 0.01f
        IconButton(
            onClick = { 
                onValueChange(defaultValue)
                onCommit() 
            },
            enabled = isChanged,
            modifier = Modifier.size(24.dp)
        ) {
            if (isChanged) {
                Icon(
                    imageVector = Icons.Default.Refresh, 
                    contentDescription = "Reset",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        colors = androidx.compose.material3.SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color.DarkGray,
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.Black.copy(alpha=0.2f), CircleShape)
            )
        },
        track = { sliderState ->
            androidx.compose.material3.SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(2.dp),
                colors = androidx.compose.material3.SliderDefaults.colors(
                     activeTrackColor = MaterialTheme.colorScheme.primary,
                     inactiveTrackColor = Color.DarkGray
                )
            )
        }
    )
}

@Composable
fun ToolButton(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Icon(icon, label, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
    }
}



@Composable
fun FilterButton(
    filter: FilterType, 
    isSelected: Boolean, 
    previewBitmap: android.graphics.Bitmap?,
    onClick: () -> Unit
) {
    var thumbnail by remember(filter, previewBitmap) { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(filter, previewBitmap) {
        if (previewBitmap != null) {
            withContext(Dispatchers.Default) {
                val thumb = previewBitmap.scale(100, 100)
                val paint = android.graphics.Paint()
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(FilterUtils.createFilterMatrix(filter))
                android.graphics.Canvas(thumb).drawBitmap(thumb, 0f, 0f, paint)
                thumbnail = thumb
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray)
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = filter.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(filter.label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
    }
}


