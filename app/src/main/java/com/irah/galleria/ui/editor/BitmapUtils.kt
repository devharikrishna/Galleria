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

    enum class BackgroundMode {
        NONE, REMOVE, BLUR
    }

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

        // CURVES
        val curveRGB: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f),
        val curveRed: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f),
        val curveGreen: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f),
        val curveBlue: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f),
        val curveLuminance: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f),

        // DETAIL / EFFECTS
        val clarity: Float = 0f, // Mid-tone contrast (Structure)
        val sharpen: Float = 0f, // Convolution
        val vignette: Float = 0f,
        val denoise: Float = 0f,
        val blur: Float = 0f,
        val dehaze: Float = 0f,

        // BACKGROUND
        val backgroundMode: BackgroundMode = BackgroundMode.NONE,
        val backgroundBlurRadius: Float = 0f,

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
        segmentationMask: Bitmap? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        
        onProgress?.invoke(0.1f)

        // 1. Initial Source Configuration
        var sourceForGeometry = original
        if (adjustments.backgroundMode != BackgroundMode.NONE && segmentationMask != null) {
             sourceForGeometry = applyBackgroundEffect(original, segmentationMask, adjustments)
        }
        
        // 2. Geometry & Scaling
        val geometricBitmap = applyGeometryAndScale(sourceForGeometry, adjustments, previewWidth, previewHeight)
        
        onProgress?.invoke(0.4f)
        
        // 3. Global ColorMatrix
        val cmBitmap = applyColorMatrixStart(geometricBitmap, adjustments)
        onProgress?.invoke(0.5f)
        
        // 4. Pixel Processing (Curves, HSL, Highlights/Shadows)
        var resultBitmap = cmBitmap
        
        // Calculate curve LUTs
        val defaultCurve = listOf(0f to 0f, 1f to 1f)
        val lutRGB = if (adjustments.curveRGB != defaultCurve) calculateCurveLut(adjustments.curveRGB) else null
        val lutRed = if (adjustments.curveRed != defaultCurve) calculateCurveLut(adjustments.curveRed) else null
        val lutGreen = if (adjustments.curveGreen != defaultCurve) calculateCurveLut(adjustments.curveGreen) else null
        val lutBlue = if (adjustments.curveBlue != defaultCurve) calculateCurveLut(adjustments.curveBlue) else null
        val lutLum = if (adjustments.curveLuminance != defaultCurve) calculateCurveLut(adjustments.curveLuminance) else null
        
        val hasCurves = lutRGB != null || lutRed != null || lutGreen != null || lutBlue != null || lutLum != null
        
        if (needsPixelProcessing(adjustments) || hasCurves) {
             resultBitmap = applyPixelProcessing(cmBitmap, adjustments, lutRGB, lutRed, lutGreen, lutBlue, lutLum) { progress ->
                 onProgress?.invoke(0.5f + (progress * 0.3f))
             }
        }
        onProgress?.invoke(0.8f)
        
        // 5. Convolution (Sharpen) & Structure
        if (adjustments.sharpen != 0f) {
            resultBitmap = applyConvolution(resultBitmap, adjustments)
        }
        
        if (adjustments.clarity != 0f) {
             resultBitmap = applyStructure(resultBitmap, adjustments.clarity)
        }
        
        // 6. Blur (Global)
        if (adjustments.blur > 0f) {
             resultBitmap = blurBitmap(resultBitmap, adjustments.blur) ?: resultBitmap
        }

        // 7. Denoise
        if (adjustments.denoise > 0f) {
             resultBitmap = applyDenoise(resultBitmap, adjustments.denoise)
        }
        
        // Cleanup intermediate
        if (sourceForGeometry != original && sourceForGeometry != geometricBitmap) {
            sourceForGeometry.recycle()
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
        
        // 1. Rotate & Straighten
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
        
        if (srcW <= 0) srcW = 1
        if (srcH <= 0) srcH = 1
        
        // Scale Logic
        if (reqW > 0 && reqH > 0) {
            val targetSize = max(reqW, reqH)
            val sourceSize = max(srcW, srcH)
            if (sourceSize > targetSize) {
                val scale = targetSize.toFloat() / sourceSize.toFloat()
                matrix.postScale(scale, scale)
            }
        }

        return Bitmap.createBitmap(transformedSource, srcX, srcY, srcW, srcH, matrix, true)
    }

    private fun applyColorMatrixStart(source: Bitmap, adj: Adjustments): Bitmap {
        val cm = ColorMatrix()
        
        // Brightness, Contrast, Saturation (Global)
        if (adj.contrast != 1f) {
            val scale = adj.contrast
            val translate = (-.5f * scale + .5f) * 255f
            cm.postConcat(ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        
        if (adj.saturation != 1f) {
             cm.postConcat(ColorMatrix().apply { setSaturation(adj.saturation) })
        }
        
        if (adj.exposure != 0f) {
             val scale = 2.0.pow(adj.exposure.toDouble()).toFloat()
             val scaleMat = ColorMatrix().apply { setScale(scale, scale, scale, 1f) }
             cm.postConcat(scaleMat)
        }
        
        if (adj.brightness != 0f) {
             val t = adj.brightness * 255f
             val mat = floatArrayOf(
                 1f, 0f, 0f, 0f, t,
                 0f, 1f, 0f, 0f, t,
                 0f, 0f, 1f, 0f, t,
                 0f, 0f, 0f, 1f, 0f
             )
             cm.postConcat(ColorMatrix(mat))
        }

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        
        val out = createBitmap(source.width, source.height)
        val canvas = Canvas(out)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }
    
    private fun calculateCurveLut(points: List<Pair<Float, Float>>): IntArray {
        val size = 256
        val lut = IntArray(size)

        if (points.isEmpty()) {
            for (i in 0 until size) lut[i] = i
            return lut
        }

        // 1. Prepare arrays
        // Ensure sorted by X
        val sortedPoints = points.sortedBy { it.first }
        val n = sortedPoints.size
        val x = FloatArray(n)
        val y = FloatArray(n)

        for (i in 0 until n) {
            x[i] = sortedPoints[i].first * 255f
            y[i] = sortedPoints[i].second * 255f
        }

        // 2. Compute slopes (secants)
        // delta[i] = (y[i+1] - y[i]) / (x[i+1] - x[i])
        val delta = FloatArray(n - 1)
        for (i in 0 until n - 1) {
            val h = x[i+1] - x[i]
            if (h == 0f) {
                delta[i] = 0f 
            } else {
                delta[i] = (y[i+1] - y[i]) / h
            }
        }

        // 3. Compute tangents (m)
        val m = FloatArray(n)
        
        // Endpoints
        m[0] = delta[0]
        m[n-1] = delta[n-2]

        // Internal points: average of adjacent secants
        for (i in 1 until n - 1) {
            m[i] = (delta[i-1] + delta[i]) / 2f
        }

        // 4. Enforce Monotonicity (prevent overshoot)
        if (n > 2) {
            for (i in 0 until n - 1) {
                if (delta[i] == 0f) {
                    m[i] = 0f
                    m[i+1] = 0f
                } else {
                    val alpha = m[i] / delta[i]
                    val beta = m[i+1] / delta[i]
                    val distance = alpha * alpha + beta * beta
                    if (distance > 9f) {
                        val tau = 3f / sqrt(distance)
                        m[i] = tau * alpha * delta[i]
                        m[i+1] = tau * beta * delta[i]
                    }
                }
            }
        }

        // 5. Generate LUT
        for (i in 0 until size) {
            val tX = i.toFloat()
            
            // Handle out of bounds
            if (tX <= x[0]) {
                lut[i] = y[0].toInt().coerceIn(0, 255)
                continue
            }
            if (tX >= x[n-1]) {
                lut[i] = y[n-1].toInt().coerceIn(0, 255)
                continue
            }

            // Find segment
            var k = 0
            while (k < n - 2 && tX > x[k+1]) {
                k++
            }

            val h = x[k+1] - x[k]
            if (h == 0f) {
                lut[i] = y[k].toInt().coerceIn(0, 255)
            } else {
                val t = (tX - x[k]) / h
                val t2 = t * t
                val t3 = t2 * t
                val h00 = 2 * t3 - 3 * t2 + 1
                val h10 = t3 - 2 * t2 + t
                val h01 = -2 * t3 + 3 * t2
                val h11 = t3 - t2
                
                val valY = h00 * y[k] + h10 * h * m[k] + h01 * y[k+1] + h11 * h * m[k+1]
                lut[i] = valY.toInt().coerceIn(0, 255)
            }
        }
        return lut
    }

    private suspend fun applyPixelProcessing(
        source: Bitmap, 
        adj: Adjustments,
        lutRGB: IntArray?,
        lutRed: IntArray?,
        lutGreen: IntArray?,
        lutBlue: IntArray?,
        lutLum: IntArray?,
        onProgress: ((Float) -> Unit)? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val workingBitmap = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        if (workingBitmap == null) return@withContext source
        
        val cores = Runtime.getRuntime().availableProcessors()
        val chunkHeight = max(height / cores, 10)
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
            if (adj.whites != 0f && x > 0.7f) {
                val t = (x - 0.7f) / 0.3f
                x += adj.whites * t * 0.2f
            }
            if (adj.blacks != 0f && x < 0.3f) {
                 val t = (0.3f - x) / 0.3f
                 x += adj.blacks * t * 0.2f
            }
            toneLut[i] = (x.coerceIn(0f, 1f) * 255).toInt()
        }

        val tempAdj = adj.temperature * 40 
        val tintAdj = adj.tint * 20        
        val skinToneAdj = adj.skinTone     

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

                        // Temperature/Tint
                        if (tempAdj != 0f || tintAdj != 0f) {
                            r = (r + tempAdj).toInt().coerceIn(0, 255)
                            b = (b - tempAdj).toInt().coerceIn(0, 255)
                            g = (g + tintAdj).toInt().coerceIn(0, 255)
                        }

                        // Skin Tone
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

                        // Vibrance
                        if (adj.vibrance != 0f) {
                            val max = max(r, max(g, b))
                            val min = min(r, min(g, b))
                            val sat = if (max == 0) 0f else (max - min).toFloat() / max
                            val vib = adj.vibrance * 0.5f * (1f - sat)
                            r = (r + r * vib).toInt().coerceIn(0, 255)
                            g = (g + g * vib).toInt().coerceIn(0, 255)
                            b = (b + b * vib).toInt().coerceIn(0, 255)
                        }

                        // Tone Mapping
                        r = toneLut[r]
                        g = toneLut[g]
                        b = toneLut[b]

                        // Vignette
                         if (adj.vignette > 0f) {
                            val dx = cx - width / 2f
                            val dist = sqrt(dx * dx + dySq) * invMaxRad
                            val edge0 = 0.3f
                            val edge1 = 1.0f
                            val t = ((dist - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
                            val smoothT = t * t * (3f - 2f * t)
                            val darkness = smoothT * adj.vignette
                            val f = 1f - darkness
                            r = (r * f).toInt()
                            g = (g * f).toInt()
                            b = (b * f).toInt()
                        }

                        // Apply Curves
                        // 1. Master RGB
                        if (lutRGB != null) {
                            r = lutRGB[r]
                            g = lutRGB[g]
                            b = lutRGB[b]
                        }
                        
                        // 2. Individual Channels
                        if (lutRed != null) r = lutRed[r]
                        if (lutGreen != null) g = lutGreen[g]
                        if (lutBlue != null) b = lutBlue[b]
                        
                        // 3. Luminance
                        if (lutLum != null) {
                            // Y = 0.299R + 0.587G + 0.114B
                            // Integer approximation: (R*77 + G*150 + B*29) >> 8
                            val y = (r * 77 + g * 150 + b * 29) shr 8
                            val newY = lutLum[y.coerceIn(0, 255)]
                            
                            if (y > 0) {
                                val scale = newY.toFloat() / y
                                r = (r * scale).toInt().coerceIn(0, 255)
                                g = (g * scale).toInt().coerceIn(0, 255)
                                b = (b * scale).toInt().coerceIn(0, 255)
                            } else {
                                if (newY > 0) {
                                    r = newY
                                    g = newY
                                    b = newY
                                }
                            }
                        }

                        // HSL (Simplistic)
                        if (adj.hsl.isNotEmpty()) {
                             val hsv = FloatArray(3)
                             Color.RGBToHSV(r, g, b, hsv)
                             
                             val hue = hsv[0]
                             var satScale = 1f
                             var lumScale = 1f
                             var hueShift = 0f

                             var c1: HslChannel = HslChannel.RED
                             var c2: HslChannel = HslChannel.RED
                             var t = 0f
                             
                             if (hue < 30) { c1 = HslChannel.RED; c2 = HslChannel.ORANGE; t = hue / 30f }
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
                
                workingBitmap.setPixels(chunkPixels, 0, width, 0, startY, width, currentChunkHeight)
                
                val current = progressCounter.incrementAndGet()
                onProgress?.invoke(current.toFloat() / totalChunks)
                Unit
            }
            jobs.add(job)
        }
        
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

    private suspend fun applyBackgroundEffect(
        original: Bitmap,
        mask: Bitmap,
        adj: Adjustments
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = original.width
        val height = original.height

        val maxMaskDim = 1024
        val shouldDownscaleMask = width > maxMaskDim || height > maxMaskDim
        
        val workingMaskWidth = if (shouldDownscaleMask) {
            val ratio = width.toFloat() / height
            if (width > height) maxMaskDim else (maxMaskDim * ratio).toInt()
        } else width
        
        val workingMaskHeight = if (shouldDownscaleMask) {
            val ratio = height.toFloat() / width
            if (height > width) maxMaskDim else (maxMaskDim * ratio).toInt()
        } else height
        
        val scaledMask = mask.scale(workingMaskWidth, workingMaskHeight)
        
        // 2. Feather Mask (Blur the alpha/content)
        // Ensure mutable ARGB for stackBlur
        val blurMask = if (scaledMask.config != Bitmap.Config.ARGB_8888 || !scaledMask.isMutable) {
            scaledMask.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            scaledMask
        }
        
        try {
            stackBlur(
                getPixelsOrFail(blurMask),
                blurMask.width,
                blurMask.height,
                3 // Small radius for edge smoothing (relative to 1024px)
            )
        } catch (e: OutOfMemoryError) {
             android.util.Log.e("BitmapUtils", "OOM during mask feathering", e)
             // Continue without feathering if OOM
        }
        val finalMask = if (blurMask.width != width || blurMask.height != height) {
            blurMask.scale(width, height)
        } else {
             blurMask
        }

        val output = createBitmap(width, height)

        val blurredBg = if (adj.backgroundMode == BackgroundMode.BLUR) {
             val radius = adj.backgroundBlurRadius
             blurBitmap(original, radius) // Returns full-size bitmap (using optimization internally)
        } else {
             null
        }

        // 3. Alpha Blend Row-by-Row to avoid huge IntArray allocations
        val rowPixels = IntArray(width)
        val rowMaskPixels = IntArray(width)
        val rowBgPixels = if (blurredBg != null) IntArray(width) else null
        
        for (y in 0 until height) {
            // Read rows
            original.getPixels(rowPixels, 0, width, 0, y, width, 1)
            finalMask.getPixels(rowMaskPixels, 0, width, 0, y, width, 1)
            if (blurredBg != null && rowBgPixels != null) {
                blurredBg.getPixels(rowBgPixels, 0, width, 0, y, width, 1)
            }
            
            for (x in 0 until width) {
                val maskVal = (rowMaskPixels[x] ushr 24) and 0xFF
                val subjectAlpha = maskVal / 255f
                
                if (subjectAlpha >= 1f) {
                    // Keep original (rowPixels[x] is already original)
                } else if (subjectAlpha <= 0f) {
                    if (rowBgPixels != null) {
                        rowPixels[x] = rowBgPixels[x]
                    } else {
                        rowPixels[x] = Color.TRANSPARENT
                    }
                } else {
                    val fg = rowPixels[x]
                    val bg = rowBgPixels?.get(x) ?: Color.TRANSPARENT
                    rowPixels[x] = blendColors(bg, fg, subjectAlpha)
                }
            }
            // Write row
            output.setPixels(rowPixels, 0, width, 0, y, width, 1)
        }
        
        // Recycle temps
        if (scaledMask != mask) scaledMask.recycle()
        if (blurMask != scaledMask) blurMask.recycle()
        if (finalMask != blurMask) finalMask.recycle()
        blurredBg?.recycle()
        
        return@withContext output
    }
    
    private fun blendColors(bg: Int, fg: Int, ratio: Float): Int {
        val inv = 1f - ratio
        
        val aBg = (bg shr 24) and 0xFF
        val rBg = (bg shr 16) and 0xFF
        val gBg = (bg shr 8) and 0xFF
        val bBg = bg and 0xFF
        
        val aFg = (fg shr 24) and 0xFF
        val rFg = (fg shr 16) and 0xFF
        val gFg = (fg shr 8) and 0xFF
        val bFg = fg and 0xFF
        
        val a = (aBg * inv + aFg * ratio).toInt()
        val r = (rBg * inv + rFg * ratio).toInt()
        val g = (gBg * inv + gFg * ratio).toInt()
        val b = (bBg * inv + bFg * ratio).toInt()
        
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    
    private fun getPixelsOrFail(bitmap: Bitmap): IntArray {
        val p = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(p, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return p
    }

    private suspend fun applyStructure(source: Bitmap, amount: Float): Bitmap = withContext(Dispatchers.Default) {
        if (amount <= 0f) return@withContext source
        
        val width = source.width
        val height = source.height
        
        val radius = 5.coerceAtLeast((width * 0.005f).toInt())
        
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

    fun blurBitmap(source: Bitmap, amount: Float): Bitmap? {
        if (amount <= 0f) return source
        // Optimization: Don't blur giant bitmaps. Downscale, blur, upscale.
        // Max dimension for blur calculation
        val maxDim = 1024
        val width = source.width
        val height = source.height
        
        val shouldDownscale = width > maxDim || height > maxDim
        
        val targetWidth = if (shouldDownscale) {
             val ratio = width.toFloat() / height
             if (width > height) maxDim else (maxDim * ratio).toInt()
        } else width
        
        val targetHeight = if (shouldDownscale) {
             val ratio = height.toFloat() / width
             if (height > width) maxDim else (maxDim * ratio).toInt()
        } else height
        
        // 1. Create scaled input if needed
        val input = if (shouldDownscale) {
            source.scale(targetWidth, targetHeight)
        } else {
             source.copy(Bitmap.Config.ARGB_8888, true)
        }
        
        val radius = (amount * 50).toInt().coerceAtLeast(1)
        val pixels = IntArray(input.width * input.height)
        input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
        
        try {
            stackBlur(pixels, input.width, input.height, radius)
            input.setPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
            
            // 2. Return result (Upscale if we downscaled)
            return if (shouldDownscale) {
                val upscaled = input.scale(width, height)
                input.recycle()
                upscaled
            } else {
                input
            }
            
        } catch (e: OutOfMemoryError) {
             android.util.Log.e("BitmapUtils", "OOM in blurBitmap", e)
             if (shouldDownscale) input.recycle()
             return source // Return unblurred on error
        }
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
