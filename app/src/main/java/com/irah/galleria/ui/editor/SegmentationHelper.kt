package com.irah.galleria.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter.ImageSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class SegmentationHelper(private val context: Context) {

    private var imageSegmenter: ImageSegmenter? = null

    init {
        setupSegmenter()
    }

    private fun setupSegmenter() {
        android.util.Log.d("SegmentationHelper", "Setting up segmenter...")
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("deeplab_v3.tflite")
            .build()

        val options = ImageSegmenterOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setOutputCategoryMask(true)
            .build()

        try {
            imageSegmenter = ImageSegmenter.createFromOptions(context, options)
            android.util.Log.d("SegmentationHelper", "Segmenter created successfully.")
        } catch (e: Exception) {
            android.util.Log.e("SegmentationHelper", "Error creating segmenter", e)
            e.printStackTrace()
        }
    }

    // Change return type to Bitmap (the mask)
    suspend fun segmentImage(bitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        android.util.Log.d("SegmentationHelper", "Starting segmentation. Original: ${bitmap.width}x${bitmap.height}")
        if (imageSegmenter == null) {
            setupSegmenter()
            if (imageSegmenter == null) return@withContext null
        }
        
        try {
            // 1. Downscale if too large to prevent OOM
            val maxDimension = 1024
            val scaleFactor = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                maxDimension.toFloat() / kotlin.math.max(bitmap.width, bitmap.height)
            } else {
                1.0f
            }
            
            val processingBitmap = if (scaleFactor < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scaleFactor).toInt(),
                    (bitmap.height * scaleFactor).toInt(),
                    true
                )
            } else {
                bitmap
            }
            
            android.util.Log.d("SegmentationHelper", "Processing bitmap size: ${processingBitmap.width}x${processingBitmap.height}")

            val mpImage = BitmapImageBuilder(processingBitmap).build()
            val result = imageSegmenter?.segment(mpImage)
            val maskImage = result?.categoryMask()?.orElse(null)

            if (maskImage != null) {
                 val buffer = ByteBufferExtractor.extract(maskImage)
                 val maskWidth = maskImage.width
                 val maskHeight = maskImage.height
                 
                 // Normalize Mask: 0 -> 0, Anything else -> 255
                 // This creates a binary mask where Subject is full white (255) and Background is black (0).
                 // This is essential for proper Alpha Blending and Feathering.
                 val capacity = buffer.capacity()
                 val normalizedBuffer = java.nio.ByteBuffer.allocateDirect(capacity)
                 buffer.rewind()
                 
                 for (i in 0 until capacity) {
                     val byteVal = buffer.get()
                     if (byteVal.toInt() == 0) {
                         normalizedBuffer.put(0.toByte())
                     } else {
                         normalizedBuffer.put(255.toByte())
                     }
                 }
                 normalizedBuffer.rewind()
                 
                 // Create Mask Bitmap (ALPHA_8)
                 val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
                 maskBitmap.copyPixelsFromBuffer(normalizedBuffer)
                 
                 // Clean up scaled bitmap if we created one
                 if (processingBitmap != bitmap) {
                     processingBitmap.recycle()
                 }
                 
                 return@withContext maskBitmap
            } else {
                 if (processingBitmap != bitmap) processingBitmap.recycle()
                 return@withContext null
            }
        } catch (e: Exception) {
            android.util.Log.e("SegmentationHelper", "Error during segmentation", e)
            e.printStackTrace()
            null
        }
    }
    
    // Obsolete methods removeBackground and blurBackground removed. 
    // Logic moved to BitmapUtils for non-destructive pipeline.

    fun close() {
        imageSegmenter?.close()
    }
}
