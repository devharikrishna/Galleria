package com.irah.galleria.ui.editor

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irah.galleria.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class EditorState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val previewBitmap: Bitmap? = null,
    val originalBitmap: Bitmap? = null,
    
    // Detailed Adjustment Values
    val adjustments: BitmapUtils.Adjustments = BitmapUtils.Adjustments(),
    
    // UI State
    val activeTool: EditorTool = EditorTool.NONE,
    val aspectRatio: Float? = null,
    
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    
    // Cache for Auto Enhance Analysis (Exposure, Contrast)
    val autoEnhanceParams: Pair<Float, Float>? = null,
    val activeAutoVariant: AutoEnhanceVariant? = null
)

enum class EditorTool {
    NONE, LIGHT, COLOR, DETAIL, HSL, CROP, FILTER, MARKUP, AUTO_ENHANCE
}

enum class AutoEnhanceVariant {
    NONE, BALANCED, WARM, COOL, VIVID
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: MediaRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mediaId: Long = savedStateHandle.get<Long>("mediaId") 
        ?: savedStateHandle.get<String>("mediaId")?.toLongOrNull() 
        ?: -1L
    
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val history = ArrayDeque<BitmapUtils.Adjustments>()
    private val redoStack = ArrayDeque<BitmapUtils.Adjustments>()
    
    private var renderJob: Job? = null

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            val media = repository.getMediaById(mediaId)
            if (media != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val original = BitmapUtils.loadCorrectlyOrientedBitmap(context, Uri.parse(media.uri))
                        
                        if (original != null) {
                            val preview = BitmapUtils.applyAdjustments(original, _state.value.adjustments, 1080, 1920)
                            _state.update { 
                                it.copy(
                                    isLoading = false,
                                    originalBitmap = original,
                                    previewBitmap = preview
                                ) 
                            }
                            history.add(_state.value.adjustments)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun onAdjustmentChange(newAdjustments: BitmapUtils.Adjustments) {
        _state.update { it.copy(adjustments = newAdjustments) }
        triggerRender(newAdjustments)
    }
    
    private fun triggerRender(adjustments: BitmapUtils.Adjustments) {
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            val original = _state.value.originalBitmap ?: return@launch
            
            // Logic: If using CROP tool, we want to see the FULL image (uncropped) backing the overlay.
            // If checking Adjust/Filters, we want to see the CROPPED result.
            val effectiveAdjustments = if (_state.value.activeTool == EditorTool.CROP) {
                adjustments.copy(cropRect = null)
            } else {
                adjustments
            }

            // Reduce preview size for live updates to improve performance
            // 720p is sufficient for phone screens and much faster to process on CPU
            val newPreview = BitmapUtils.applyAdjustments(original, effectiveAdjustments, 720, 1280)
            _state.update { it.copy(previewBitmap = newPreview) }
        }
    }

    fun commitAdjustment() {
        val current = _state.value.adjustments
        if (history.isEmpty() || history.last() != current) {
            history.add(current)
            redoStack.clear()
            updateHistoryState()
        }
    }

    fun setTool(tool: EditorTool) {
        _state.update { 
            if (it.activeTool == tool) {
                it.copy(activeTool = EditorTool.NONE)
            } else {
                it.copy(activeTool = tool)
            }
        }
        // Re-render because switching to/from CROP changes how we display the preview
        triggerRender(_state.value.adjustments)
    }
    
    fun applyAutoVariant(variant: AutoEnhanceVariant) {
        val original = _state.value.originalBitmap ?: return
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, activeAutoVariant = variant) }
            
            // 1. Get or Calculate Base Params
            val baseParams = _state.value.autoEnhanceParams ?: run {
                val params = BitmapUtils.calculateAutoParameters(original)
                _state.update { it.copy(autoEnhanceParams = params) }
                params
            }
            
            val (baseExposure, baseContrast) = baseParams
            
            // 2. Apply Variant Specifics
            val current = _state.value.adjustments
            
            // Reset base values first (optional, but cleaner to start from 'current' minus previous auto? 
            // No, we just overwrite the relevant fields)
            
            var newAdjustments = current.copy(
                exposure = baseExposure,
                contrast = baseContrast
            )
            
            when (variant) {
                AutoEnhanceVariant.NONE -> {
                    // Reset to neutral
                    newAdjustments = newAdjustments.copy(
                        exposure = 0f,
                        contrast = 1f,
                        temperature = 0f,
                        tint = 0f,
                        saturation = 1f,
                        vibrance = 0f
                    )
                }
                AutoEnhanceVariant.BALANCED -> {
                    // Just base
                }
                AutoEnhanceVariant.WARM -> {
                    newAdjustments = newAdjustments.copy(
                        temperature = 0.2f, // Warm up
                        tint = 0.05f
                    )
                }
                AutoEnhanceVariant.COOL -> {
                    newAdjustments = newAdjustments.copy(
                        temperature = -0.2f, // Cool down
                        tint = -0.05f
                    )
                }
                AutoEnhanceVariant.VIVID -> {
                    newAdjustments = newAdjustments.copy(
                        saturation = 1.2f,
                        vibrance = 0.3f,
                        contrast = (baseContrast * 1.1f).coerceIn(0.5f, 1.5f) // Extra pop
                    )
                }
            }
            
            onAdjustmentChange(newAdjustments)
            commitAdjustment()
            
            _state.update { it.copy(isLoading = false) }
        }
    }
    
    fun setAspectRatio(ratio: Float?) {
        _state.update { it.copy(aspectRatio = ratio) }
        
        // If ratio is set, we force update the crop rect immediately
        if (ratio != null) {
             val original = _state.value.originalBitmap
             if (original != null) {
                 val imageRatio = original.width.toFloat() / original.height.toFloat()
                 val newRect = BitmapUtils.calculateMaxRect(imageRatio, ratio)
                 onAdjustmentChange(_state.value.adjustments.copy(cropRect = newRect))
             }
        } else {
             // Free crop - just keep current or reset? 
             // Usually keeping current is fine, but if we want to confirm "Free", let's leave it.
             // Or maybe reset to full? Let's leave crop as is but unlock ratio.
        }
    }

    fun undo() {
        if (history.size > 1) {
            val current = history.removeLast()
            redoStack.add(current)
            val previous = history.last()
            
            _state.update { it.copy(adjustments = previous) }
            triggerRender(previous)
            updateHistoryState()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            history.add(next)
            
            _state.update { it.copy(adjustments = next) }
            triggerRender(next)
            updateHistoryState()
        }
    }
    
    private fun updateHistoryState() {
        _state.update { 
            it.copy(
                canUndo = history.size > 1,
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    private val _saveProgress = MutableStateFlow(0f)
    val saveProgress: StateFlow<Float> = _saveProgress.asStateFlow()

    fun saveImage(onComplete: (Long?) -> Unit) {
        val original = _state.value.originalBitmap ?: return
        val adjustments = _state.value.adjustments
        
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            _saveProgress.value = 0f
            var savedId: Long? = null
            
            // Get original media info for folder path
            val originalMedia = repository.getMediaById(mediaId)
            
            withContext(Dispatchers.IO) {
                try {
                    // 1. Process Bitmap (0% -> 80%)
                    val finalBitmap = BitmapUtils.applyAdjustments(
                        original = original, 
                        adjustments = adjustments,
                        onProgress = { p -> 
                            // Scale process progress to 80% of total bar
                            _saveProgress.value = p * 0.8f 
                        }
                    )
                    
                    // 2. Save File (80% -> 100%)
                    _saveProgress.value = 0.85f
                    
                    // Name: IMG_2024..._EDIT.jpg or Original_EDIT.jpg
                    val originalName = originalMedia?.name?.substringBeforeLast('.') ?: "IMG_${System.currentTimeMillis()}"
                    val filename = "${originalName}_EDIT.jpg"
                    
                    // Path: Use original relative path or fallback
                    val relativePath = originalMedia?.relativePath ?: (Environment.DIRECTORY_PICTURES + "/Galleria")
                    
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                        put(android.provider.MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                    }
                    
                    val resolver = context.contentResolver
                    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            // Compress takes time
                             finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                        
                        _saveProgress.value = 0.95f
                        
                        contentValues.clear()
                        contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                        
                        try {
                            savedId = ContentUris.parseId(uri)
                        } catch (e: Exception) {
                            // Fallback if parsable
                            savedId = uri.lastPathSegment?.toLongOrNull()
                        }
                        
                         _saveProgress.value = 1.0f
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _state.update { it.copy(isSaving = false) }
            withContext(Dispatchers.Main) { onComplete(savedId) }
        }
    }
}
