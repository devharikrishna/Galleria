package com.irah.galleria.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.compose.ui.geometry.Rect
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.abs
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Deferred
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.core.graphics.scale

object BitmapUtils {

    enum class HslChannel {
        RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE, MAGENTA
    }

    data class HslShift(
        val hue: Float = 0f, 
        val saturation: Float = 0f, 
        val luminance: Float = 0f 
    )

    data class Adjustments(
        // LIGHT
        val exposure: Float = 0f,
        val brightness: Float = 0f,
        val contrast: Float = 1f,
        val highlights: Float = 0f,
        val shadows: Float = 0f,
        val whites: Float = 0f,
        val blacks: Float = 0f,

        // COLOR
        val saturation: Float = 1f,
        val vibrance: Float = 0f, 
        val temperature: Float = 0f,
        val tint: Float = 0f,
        val skinTone: Float = 0f, // -1.0 (Pale/Cool) to 1.0 (Warm/Saturated)
        val skinColor: Float = 0f, // -1.0 (Dark) to 1.0 (Light/Fair)

        // HSL
        val hsl: Map<HslChannel, HslShift> = emptyMap(),

        // DETAIL / EFFECTS
        val clarity: Float = 0f, // Mid-tone contrast (Structure)
        val sharpen: Float = 0f, // Convolution
        val vignette: Float = 0f,
        val denoise: Float = 0f,
        val blur: Float = 0f,
        val dehaze: Float = 0f,

        // GEOMETRY
        val rotationDegrees: Float = 0f, // 90, 180, 270...
        val straightenDegrees: Float = 0f, // Fine rotation
        val flipHorizontal: Boolean = false,
        val flipVertical: Boolean = false,
        val cropRect: Rect? = null,
        
        // FILTER
        val filter: FilterType = FilterType.NONE,
        val filterStrength: Float = 1f
    )

    suspend fun applyAdjustments(
        original: Bitmap,
        adjustments: Adjustments,
        previewWidth: Int = -1,
        previewHeight: Int = -1,
        onProgress: ((Float) -> Unit)? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        
        onProgress?.invoke(0.1f)

        // 1. Geometry & Scaling
        val baseBitmap = applyGeometryAndScale(original, adjustments, previewWidth, previewHeight)
        onProgress?.invoke(0.3f)
        
        // 2. Global ColorMatrix (Fast) - Exposure, Contrast, Basic Saturation, FILTER
        val cmBitmap = applyColorMatrixStart(baseBitmap, adjustments)
        onProgress?.invoke(0.5f)
        
        // 3. Pixel Processing (Slow/Detailed) - Tone Curves, HSL, Vignette
        // Optimization: Only run if needed
        var resultBitmap = cmBitmap
        if (needsPixelProcessing(adjustments)) {
             resultBitmap = applyPixelProcessing(cmBitmap, adjustments) { progress ->
                 // Pixel processing is step 3, mapped to range 0.5 -> 0.8
                 onProgress?.invoke(0.5f + (progress * 0.3f))
             }
        }
        onProgress?.invoke(0.8f)
        
        // 4. Convolution (Sharpen) & Structure
        if (adjustments.sharpen != 0f) {
            resultBitmap = applyConvolution(resultBitmap, adjustments)
        }
        
        if (adjustments.clarity != 0f) {
             // Structure/Clarity
             resultBitmap = applyStructure(resultBitmap, adjustments.clarity)
        }
        
        // 5. Blur (Downscale method)
        if (adjustments.blur > 0f) {
             resultBitmap = applyBlur(resultBitmap, adjustments.blur) 
        }

        // 6. Denoise (Box Blur Convolution)
        if (adjustments.denoise > 0f) {
             resultBitmap = applyDenoise(resultBitmap, adjustments.denoise)
        }

        onProgress?.invoke(1.0f)
        
        return@withContext resultBitmap
    }

    private fun needsPixelProcessing(adj: Adjustments): Boolean {
        return adj.highlights != 0f || adj.shadows != 0f || 
               adj.whites != 0f || adj.blacks != 0f ||
               adj.vibrance != 0f || adj.temperature != 0f || adj.tint != 0f ||
               adj.hsl.isNotEmpty() || adj.vignette != 0f || adj.skinTone != 0f ||
               adj.skinColor != 0f || adj.dehaze != 0f
    }

    private fun applyGeometryAndScale(
        source: Bitmap, 
        adjustments: Adjustments,
        reqW: Int,
        reqH: Int
    ): Bitmap {
        val matrix = Matrix()
        
        // 1. Rotate & Straighten (Fine Rotation)
        val totalRotation = adjustments.rotationDegrees + adjustments.straightenDegrees
        if (totalRotation != 0f) {
            matrix.postRotate(totalRotation)
        }
        
        if (adjustments.flipHorizontal) matrix.postScale(-1f, 1f)
        if (adjustments.flipVertical) matrix.postScale(1f, -1f)
        
        var transformedSource = source
        if (totalRotation != 0f || adjustments.flipHorizontal || adjustments.flipVertical) {
             if (adjustments.straightenDegrees != 0f) {
                 val zoom = calculateAutoZoomScale(source.width, source.height, adjustments.straightenDegrees)
                 matrix.postScale(zoom, zoom)
             }
            
             transformedSource = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
             matrix.reset()
             if (adjustments.straightenDegrees != 0f) {
                 val newW = transformedSource.width
                 val newH = transformedSource.height
                 val targetW = source.width
                 val targetH = source.height
                 
                 // Center Crop
                 val cropX = ((newW - targetW) / 2).coerceAtLeast(0)
                 val cropY = ((newH - targetH) / 2).coerceAtLeast(0)
                 val cropW = targetW.coerceAtMost(newW - cropX)
                 val cropH = targetH.coerceAtMost(newH - cropY)
                 
                 val cropped = Bitmap.createBitmap(transformedSource, cropX, cropY, cropW, cropH)
                 if (cropped != transformedSource) {
                     transformedSource = cropped
                 }
             }
        }

        var srcX = 0
        var srcY = 0
        var srcW = transformedSource.width
        var srcH = transformedSource.height

        if (adjustments.cropRect != null) {
            srcX = (adjustments.cropRect.left * transformedSource.width).toInt().coerceAtLeast(0)
            srcY = (adjustments.cropRect.top * transformedSource.height).toInt().coerceAtLeast(0)
            srcW = (adjustments.cropRect.width * transformedSource.width).toInt().coerceAtMost(transformedSource.width - srcX)
            srcH = (adjustments.cropRect.height * transformedSource.height).toInt().coerceAtMost(transformedSource.height - srcY)
        }
        
        // Safety check
        if (srcW <= 0) srcW = 1
        if (srcH <= 0) srcH = 1
        
        // Scale Logic
        var scale: Float
        if (reqW > 0 && reqH > 0) {
            val targetSize = max(reqW, reqH)
            val sourceSize = max(srcW, srcH)
            if (sourceSize > targetSize) {
                scale = targetSize.toFloat() / sourceSize.toFloat()
                matrix.postScale(scale, scale)
            }
        }

        return Bitmap.createBitmap(transformedSource, srcX, srcY, srcW, srcH, matrix, true)
    }

    private fun applyColorMatrixStart(source: Bitmap, adjustments: Adjustments): Bitmap {
        val output = createBitmap(source.width, source.height)
        val canvas = Canvas(output)
        val paint = Paint()
        val cm = ColorMatrix()
        
        // 0. Apply Preset Filter first (base look)
        if (adjustments.filter != FilterType.NONE) {
            val base = FilterUtils.createFilterMatrix(adjustments.filter)
            val scaled = FilterUtils.scaleFilterMatrix(base, adjustments.filterStrength)
            cm.postConcat(scaled)
        }

        // Exposure (Gain)
        if (adjustments.exposure != 0f) {
            // 2^exposure (e.g. +1 -> 2x, -1 -> 0.5x)
            val gain = 2.0.pow(adjustments.exposure.toDouble()).toFloat()
            cm.setScale(gain, gain, gain, 1f)
        }

        // Brightness (Offset)
        if (adjustments.brightness != 0f) {
             val t = adjustments.brightness * 100f // Range approx -100 to 100
             cm.postConcat(ColorMatrix(floatArrayOf(
                 1f, 0f, 0f, 0f, t,
                 0f, 1f, 0f, 0f, t,
                 0f, 0f, 1f, 0f, t,
                 0f, 0f, 0f, 1f, 0f
             )))
        }

        // Contrast
        if (adjustments.contrast != 1f) {
            val scale = adjustments.contrast
            val translate = (-0.5f * (scale - 1)) * 255f
            cm.postConcat(ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )))
        }

        // Saturation
        if (adjustments.saturation != 1f) {
            val sat = ColorMatrix()
            sat.setSaturation(adjustments.saturation)
            cm.postConcat(sat)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    private suspend fun applyPixelProcessing(
        source: Bitmap, 
        adj: Adjustments,
        onProgress: ((Float) -> Unit)? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val workingBitmap = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        if (workingBitmap == null) return@withContext source // Fallback
        val cores = Runtime.getRuntime().availableProcessors()
        val chunkHeight = max(height / cores, 10) // Ensure at least 10 rows
        val toneLut = IntArray(256)
        for (i in 0..255) {
            var x = i / 255f
            
            if (adj.shadows != 0f) {
                val influence = (1f - x).pow(3) 
                x += adj.shadows * influence * 0.5f
            }

            if (adj.highlights != 0f) {
                val influence = x.pow(3)
                x += adj.highlights * influence * 0.5f 
            }
            
            if (adj.whites != 0f) {
                 if (x > 0.7f) {
                     val t = (x - 0.7f) / 0.3f
                     x += adj.whites * t * 0.2f
                 }
            }
             if (adj.blacks != 0f) {
                 if (x < 0.3f) {
                     val t = (0.3f - x) / 0.3f
                     x += adj.blacks * t * 0.2f
                 }
            }
            toneLut[i] = (x.coerceIn(0f, 1f) * 255).toInt()
        }

        val tempAdj = adj.temperature * 40 // Range +/- 40
        val tintAdj = adj.tint * 20        // Range +/- 20
        val skinToneAdj = adj.skinTone     // -1 to 1

        val totalChunks = (height + chunkHeight - 1) / chunkHeight
        val progressCounter = AtomicInteger(0)
        val jobs = mutableListOf<Deferred<Unit>>()
        
        val maxRadSq = (width * width / 4f) + (height * height / 4f)
        val invMaxRad = 1f / sqrt(maxRadSq)

        for (startY in 0 until height step chunkHeight) {
            val job = async {
                val currentChunkHeight = min(chunkHeight, height - startY)
                val pixelCount = width * currentChunkHeight
                val chunkPixels = IntArray(pixelCount)
                workingBitmap.getPixels(chunkPixels, 0, width, 0, startY, width, currentChunkHeight)
                
                for (cy in 0 until currentChunkHeight) {
                    val globalY = startY + cy
                    val dy = globalY - height / 2f
                    val dySq = dy * dy
                    val rowOffset = cy * width
                    
                    for (cx in 0 until width) {
                        val i = rowOffset + cx
                        val p = chunkPixels[i]
                        
                        var r = (p shr 16) and 0xFF
                        var g = (p shr 8) and 0xFF
                        var b = p and 0xFF
                        val a = (p shr 24) and 0xFF

                        if (tempAdj != 0f || tintAdj != 0f) {
                            r = (r + tempAdj).toInt().coerceIn(0, 255)
                            b = (b - tempAdj).toInt().coerceIn(0, 255)
                            g = (g + tintAdj).toInt().coerceIn(0, 255)
                        }

                        if (skinToneAdj != 0f) {
                             if (g in (b + 1)..<r) {
                                val gRatio = if (r > 0) g.toFloat()/r else 0f
                                if (gRatio > 0.4f && gRatio < 0.85f) {
                                    val factor = skinToneAdj * 0.3f
                                    val lum = (r+g+b)/3
                                    r = (r + (r-lum)*factor).toInt().coerceIn(0,255)
                                    g = (g + (g-lum)*factor).toInt().coerceIn(0,255)
                                    if (adj.skinColor != 0f) {
                                         val brightFactor = 1f + (adj.skinColor * 0.15f)
                                         r = (r * brightFactor).toInt().coerceIn(0, 255)
                                         g = (g * brightFactor).toInt().coerceIn(0, 255)
                                         b = (b * brightFactor).toInt().coerceIn(0, 255)
                                    }
                                }
                            }
                        }
                        
                        // Dehaze
                         if (adj.dehaze != 0f) {
                             val dehaze = adj.dehaze
                             r = (r - (dehaze * 10)).toInt().coerceIn(0, 255)
                             g = (g - (dehaze * 10)).toInt().coerceIn(0, 255)
                             b = (b - (dehaze * 10)).toInt().coerceIn(0, 255)
                        }

                        // C. Vibrance
                        if (adj.vibrance != 0f) {
                            val max = max(r, max(g, b))
                            val min = min(r, min(g, b))
                            val sat = if (max == 0) 0f else (max - min).toFloat() / max
                            val vib = adj.vibrance * 0.5f * (1f - sat)
                            r = (r + r * vib).toInt().coerceIn(0, 255)
                            g = (g + g * vib).toInt().coerceIn(0, 255)
                            b = (b + b * vib).toInt().coerceIn(0, 255)
                        }

                        // D. Tone Mapping
                        r = toneLut[r]
                        g = toneLut[g]
                        b = toneLut[b]

                        // E. Vignette (Optimized & Smooth)
                         if (adj.vignette > 0f) {
                            val dx = cx - width / 2f
                            val dist = sqrt(dx * dx + dySq) * invMaxRad
                            
                            // SmoothStep Falloff
                            // Start darkening at 0.3 (30% from center)
                            // Full effect at 1.0 (corners)
                            val edge0 = 0.3f
                            val edge1 = 1.0f
                            val t = ((dist - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
                            val smoothT = t * t * (3f - 2f * t)
                            
                            val darkness = smoothT * adj.vignette
                            // Apply darkness (multiply)
                            val f = 1f - darkness
                            
                            r = (r * f).toInt()
                            g = (g * f).toInt()
                            b = (b * f).toInt()
                        }

                        // F. HSL (Heavy - Smooth Interpolation)
                        // ... HSL Logic ...
                        if (adj.hsl.isNotEmpty()) {
                             val hsv = FloatArray(3)
                             Color.RGBToHSV(r, g, b, hsv)
                             // ... (Rest of HSL logic remains same, just ensure variable scope)
                             
                             val hue = hsv[0]
                             var satScale = 1f
                             var lumScale = 1f
                             var hueShift = 0f

                            var c1: HslChannel = HslChannel.RED
                             var c2: HslChannel = HslChannel.RED
                             var t = 0f
                             
                             if (hue < 30) { c1 = HslChannel.RED; c2 = HslChannel.ORANGE; t =
                                 hue / 30f }
                             else if (hue < 60) { c1 = HslChannel.ORANGE; c2 = HslChannel.YELLOW; t = (hue - 30) / 30f }
                             else if (hue < 120) { c1 = HslChannel.YELLOW; c2 = HslChannel.GREEN; t = (hue - 60) / 60f }
                             else if (hue < 180) { c1 = HslChannel.GREEN; c2 = HslChannel.AQUA; t = (hue - 120) / 60f }
                             else if (hue < 240) { c1 = HslChannel.AQUA; c2 = HslChannel.BLUE; t = (hue - 180) / 60f }
                             else if (hue < 280) { c1 = HslChannel.BLUE; c2 = HslChannel.PURPLE; t = (hue - 240) / 40f }
                             else if (hue < 315) { c1 = HslChannel.PURPLE; c2 = HslChannel.MAGENTA; t = (hue - 280) / 35f }
                             else { c1 = HslChannel.MAGENTA; c2 = HslChannel.RED; t = (hue - 315) / 45f }
                             
                             val s1 = adj.hsl[c1]
                             val s2 = adj.hsl[c2]
                             
                             if (s1 != null) {
                                 val w = 1f - t
                                 hueShift += s1.hue * w
                                 satScale += s1.saturation * w
                                 lumScale += s1.luminance * w
                             }
                             if (s2 != null) {
                                 val w = t
                                 hueShift += s2.hue * w
                                 satScale += s2.saturation * w
                                 lumScale += s2.luminance * w
                             }
                             
                             if (hueShift != 0f || satScale != 1f || lumScale != 1f) {
                                 hsv[0] = (hsv[0] + hueShift).mod(360f)
                                 hsv[1] = (hsv[1] * satScale).coerceIn(0f, 1f)
                                 hsv[2] = (hsv[2] * lumScale).coerceIn(0f, 1f)
                                 val newColor = Color.HSVToColor(hsv)
                                 r = Color.red(newColor)
                                 g = Color.green(newColor)
                                 b = Color.blue(newColor)
                             }
                        }

                        chunkPixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }
                
                // Write back
                workingBitmap.setPixels(chunkPixels, 0, width, 0, startY, width, currentChunkHeight)
                
                val current = progressCounter.incrementAndGet()
                onProgress?.invoke(current.toFloat() / totalChunks)
                Unit
            }
            jobs.add(job)
        }
        
        // Await all chunks
        jobs.forEach { it.await() }

        return@withContext workingBitmap
    }



    private suspend fun applyConvolution(source: Bitmap, adj: Adjustments): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val output = createBitmap(width, height)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val newPixels = IntArray(width * height)
        
        val amount = adj.sharpen
        // If amount is small, skip
        if (amount <= 0f) return@withContext source

        val kernelCenter = 4f * amount + 1f
        val kernelNeighbor = -amount

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val p = pixels[idx]
                val pTop = pixels[idx - width]
                val pBottom = pixels[idx + width]
                val pLeft = pixels[idx - 1]
                val pRight = pixels[idx + 1]

                val a = (p shr 24) and 0xFF
                
                var r = ((p shr 16) and 0xFF) * kernelCenter +
                        ((pTop shr 16) and 0xFF) * kernelNeighbor +
                        ((pBottom shr 16) and 0xFF) * kernelNeighbor +
                        ((pLeft shr 16) and 0xFF) * kernelNeighbor +
                        ((pRight shr 16) and 0xFF) * kernelNeighbor
                        
                var g = ((p shr 8) and 0xFF) * kernelCenter +
                        ((pTop shr 8) and 0xFF) * kernelNeighbor +
                        ((pBottom shr 8) and 0xFF) * kernelNeighbor +
                        ((pLeft shr 8) and 0xFF) * kernelNeighbor +
                        ((pRight shr 8) and 0xFF) * kernelNeighbor

                var b = (p and 0xFF) * kernelCenter +
                        (pTop and 0xFF) * kernelNeighbor +
                        (pBottom and 0xFF) * kernelNeighbor +
                        (pLeft and 0xFF) * kernelNeighbor +
                        (pRight and 0xFF) * kernelNeighbor

                r = r.coerceIn(0f, 255f)
                g = g.coerceIn(0f, 255f)
                b = b.coerceIn(0f, 255f)

                newPixels[idx] = (a shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
            }
        }
        
        output.setPixels(newPixels, 0, width, 0, 0, width, height)
        return@withContext output
    }

    private suspend fun applyDenoise(source: Bitmap, amount: Float): Bitmap = withContext(Dispatchers.Default) {
        if (amount <= 0f) return@withContext source
        
        val width = source.width
        val height = source.height
        val output = createBitmap(width, height)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val newPixels = IntArray(width * height)
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                
                // Sum 3x3
                var rSum = 0
                var gSum = 0
                var bSum = 0
                
                // Unroll loop for 3x3
                for (ky in -1..1) {
                    val kIdx = idx + (ky * width)
                    
                    // -1
                    var p = pixels[kIdx - 1]
                    rSum += (p shr 16) and 0xFF
                    gSum += (p shr 8) and 0xFF
                    bSum += p and 0xFF
                    
                    // 0
                    p = pixels[kIdx]
                    rSum += (p shr 16) and 0xFF
                    gSum += (p shr 8) and 0xFF
                    bSum += p and 0xFF
                    
                    // +1
                    p = pixels[kIdx + 1]
                    rSum += (p shr 16) and 0xFF
                    gSum += (p shr 8) and 0xFF
                    bSum += p and 0xFF
                }
                
                val blurredR = rSum / 9
                val blurredG = gSum / 9
                val blurredB = bSum / 9
                
                // Blend
                val pOriginal = pixels[idx]
                val origR = (pOriginal shr 16) and 0xFF
                val origG = (pOriginal shr 8) and 0xFF
                val origB = pOriginal and 0xFF
                val a = (pOriginal shr 24) and 0xFF
                
                val finalR = (origR + (blurredR - origR) * amount).toInt().coerceIn(0, 255)
                val finalG = (origG + (blurredG - origG) * amount).toInt().coerceIn(0, 255)
                val finalB = (origB + (blurredB - origB) * amount).toInt().coerceIn(0, 255)
                
                newPixels[idx] = (a shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }
        
        output.setPixels(newPixels, 0, width, 0, 0, width, height)
        return@withContext output
    }

    private suspend fun applyStructure(source: Bitmap, amount: Float): Bitmap = withContext(Dispatchers.Default) {
        if (amount <= 0f) return@withContext source
        
        val width = source.width
        val height = source.height
        
        val radius = 5.coerceAtLeast((width * 0.005f).toInt()) // Adaptive radius approx 0.5% of width
        
        val blurredIndices = IntArray(width * height)
        source.getPixels(blurredIndices, 0, width, 0, 0, width, height)
        stackBlur(blurredIndices, width, height, radius)
        
        val output = createBitmap(width, height)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pOrig = pixels[i]
            val pBlur = blurredIndices[i]
            
            val rO = (pOrig shr 16) and 0xFF
            val gO = (pOrig shr 8) and 0xFF
            val bO = pOrig and 0xFF
            val a = (pOrig shr 24) and 0xFF
            
            val rB = (pBlur shr 16) and 0xFF
            val gB = (pBlur shr 8) and 0xFF
            val bB = pBlur and 0xFF
            
            val diffR = rO - rB
            val diffG = gO - gB
            val diffB = bO - bB
            
            val r = (rO + diffR * amount).toInt().coerceIn(0, 255)
            val g = (gO + diffG * amount).toInt().coerceIn(0, 255)
            val b = (bO + diffB * amount).toInt().coerceIn(0, 255)
            
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return@withContext output
    }

    private fun applyBlur(source: Bitmap, amount: Float): Bitmap {
        if (amount <= 0f) return source
        val radius = (amount * 50).toInt().coerceAtLeast(1)
        
        val width = source.width
        val height = source.height
        val newBitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        newBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        stackBlur(pixels, width, height, radius)
        
        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return newBitmap
    }
    
    // Stack Blur Algorithm
    private fun stackBlur(pixels: IntArray, width: Int, height: Int, radius: Int) {
        if (radius < 1) return

        val wh = width * height
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var p: Int
        val dv = IntArray(256 * div)
        for (i in 0 until 256 * div) dv[i] = i / div
        
        var divSum = (div + 1) shr 1
        divSum *= divSum
        val dv256 = IntArray(256 * divSum)
        for (i in 0 until 256 * divSum) dv256[i] = i / divSum

        // Breakdown RGB
        for (i in 0 until wh) {
            p = pixels[i]
            r[i] = (p and 0xff0000) shr 16
            g[i] = (p and 0x00ff00) shr 8
            b[i] = p and 0x0000ff
        }
        
        val tempR = IntArray(wh)
        val tempG = IntArray(wh)
        val tempB = IntArray(wh)

        // 3 passes
        (0 until 3).forEach { _ ->
            boxBlurHorizontal(r, tempR, width, height, radius)
            boxBlurVertical(tempR, r, width, height, radius)

            boxBlurHorizontal(g, tempG, width, height, radius)
            boxBlurVertical(tempG, g, width, height, radius)

            boxBlurHorizontal(b, tempB, width, height, radius)
            boxBlurVertical(tempB, b, width, height, radius)
        }

        // Reassemble
        for (i in 0 until wh) {
             val alpha = (pixels[i] shr 24) and 0xff
             pixels[i] = (alpha shl 24) or (r[i] shl 16) or (g[i] shl 8) or b[i]
        }
    }

    private fun boxBlurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
         val div = 2 * radius + 1
         for (y in 0 until h) {
             val rowOffset = y * w

             var sum = 0
             for (i in -radius..radius) {
                 val px = i.coerceIn(0, w - 1)
                 sum += src[rowOffset + px]
             }

             for (x in 0 until w) {
                 dst[rowOffset + x] = sum / div

                 // Slide window
                 val removingPx = (x - radius).coerceIn(0, w - 1)
                 val addingPx = (x + radius + 1).coerceIn(0, w - 1)

                 sum -= src[rowOffset + removingPx]
                 sum += src[rowOffset + addingPx]
             }
         }
    }

    private fun boxBlurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
         val div = 2 * radius + 1
         for (x in 0 until w) {
             var sum = 0
             for (i in -radius..radius) {
                 val py = i.coerceIn(0, h - 1)
                 sum += src[py * w + x]
             }

             for (y in 0 until h) {
                 dst[y * w + x] = sum / div

                 // Slide window
                 val removingPy = (y - radius).coerceIn(0, h - 1)
                 val addingPy = (y + radius + 1).coerceIn(0, h - 1)

                 sum -= src[removingPy * w + x]
                 sum += src[addingPy * w + x]
             }
         }
    }

    fun calculateAutoZoomScale(width: Int, height: Int, degrees: Float): Float {
        val rad = Math.toRadians(abs(degrees).toDouble())
        val sin = sin(rad)
        val cos = cos(rad)
        
        val w = width.toDouble()
        val h = height.toDouble()
        if (w <= 0.0 || h <= 0.0) return 1f
        
        val aspectRatio = w / h
        return if (aspectRatio > 1) {
            (cos + sin * aspectRatio).toFloat()
        } else {
            (cos + sin / aspectRatio).toFloat()
        }
    }

    suspend fun loadCorrectlyOrientedBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inMutable = true
            
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: return@withContext null
            inputStream.close()
            inputStream = null

            // 3. Check Exif Orientation
            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            
            var rotation = 0f
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270f
            }
            
            if (rotation != 0f) {
                val matrix = Matrix()
                matrix.postRotate(rotation)
                return@withContext Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            
            return@withContext bitmap
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
        }
    }

    fun calculateMaxRect(imageRatio: Float, targetAspectRatio: Float): Rect {
        
        val rectRatio = targetAspectRatio / imageRatio
        
        var rw: Float
        var rh: Float

        if (rectRatio < 1f) {
            rw = rectRatio
            rh = 1f
        } else {
            rw = 1f
            rh = 1f / rectRatio
        }
        
        val l = (1f - rw) / 2f
        val t = (1f - rh) / 2f
        
        return Rect(l, t, l + rw, t + rh)
    }


    suspend fun calculateAutoParameters(bitmap: Bitmap): Pair<Float, Float> = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        
        // 1. Downscale for performance (max 256px dimension)
        val scale = min(1f, 256f / max(width, height))
        val smallW = (width * scale).toInt().coerceAtLeast(1)
        val smallH = (height * scale).toInt().coerceAtLeast(1)
        
        val smallBitmap = if (scale < 1f) {
             bitmap.scale(smallW, smallH)
        } else {
             if (bitmap.isMutable) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
        
        val pixels = IntArray(smallW * smallH)
        smallBitmap.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)
        if (smallBitmap != bitmap) {
            smallBitmap.recycle()
        }

        val histogram = IntArray(256)
        var totalPixels = 0
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // Rec. 709 luminance
            val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b).toInt().coerceIn(0, 255)
            histogram[lum]++
            totalPixels++
        }

        val threshold = (totalPixels * 0.005f).toInt()
        
        var minLum = 0
        var count = 0
        for (i in 0..255) {
            count += histogram[i]
            if (count > threshold) {
                minLum = i
                break
            }
        }
        
        var maxLum = 255
        count = 0
        for (i in 255 downTo 0) {
            count += histogram[i]
            if (count > threshold) {
                maxLum = i
                break
            }
        }
        var sumLum = 0L
        for (i in 0..255) {
            sumLum += i * histogram[i]
        }
        val meanLum = sumLum.toFloat() / totalPixels
        val currentRange = (maxLum - minLum).coerceAtLeast(1)
        val stretchFactor = 255f / currentRange

        val suggestedContrast = (1f + (stretchFactor - 1f) * 0.25f).coerceIn(0.8f, 1.4f)

        val normMean = meanLum / 255f
        val diff = 0.5f - normMean
        val suggestedExposure = (diff * 1.5f).coerceIn(-0.5f, 0.5f)

        return@withContext Pair(suggestedExposure, suggestedContrast)
    }
    fun ratioToRectRatio(aspectRatio: Float, imageRatio: Float): Float {
        return aspectRatio / imageRatio
    }
}
