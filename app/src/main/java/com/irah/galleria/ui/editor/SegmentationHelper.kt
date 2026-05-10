package com.irah.galleria.ui.editor

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter.ImageSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

class SegmentationHelper(private val context: Context) {

    private var imageSegmenter: ImageSegmenter? = null

    init { setupSegmenter() }

    private fun setupSegmenter() {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun segmentImage(bitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        if (imageSegmenter == null) {
            setupSegmenter()
            if (imageSegmenter == null) return@withContext null
        }
        try {
            // 1. Downscale to 1280px max for ML inference
            val maxDim = 1280
            val scale = if (bitmap.width > maxDim || bitmap.height > maxDim)
                maxDim.toFloat() / max(bitmap.width, bitmap.height) else 1f
            val procBmp = if (scale < 1f)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(), true)
            else bitmap

            val mpImage = BitmapImageBuilder(procBmp).build()
            val result  = imageSegmenter?.segment(mpImage)
            val maskImg = result?.categoryMask()?.orElse(null)

            if (maskImg != null) {
                val buf       = ByteBufferExtractor.extract(maskImg)
                val maskW     = maskImg.width
                val maskH     = maskImg.height
                val capacity  = buf.capacity()

                // 2. Binary mask: 0 → background, 1 → foreground
                buf.rewind()
                val binary = ByteArray(capacity) { if (buf.get().toInt() == 0) 0 else 1 }

                // 3. 1-pixel dilation (recovers thin hair/finger edges)
                val dilated = binary.copyOf()
                for (y in 0 until maskH) for (x in 0 until maskW) {
                    if (binary[y * maskW + x] == 0.toByte()) continue
                    for (ky in -1..1) { val ny = y + ky; if (ny < 0 || ny >= maskH) continue
                        for (kx in -1..1) { val nx = x + kx; if (nx < 0 || nx >= maskW) continue
                            dilated[ny * maskW + nx] = 1 } }
                }

                // 4. Guided filter — snaps the mask boundary to actual image edges
                //    using the processing bitmap as a guide at 1280px resolution.
                //    This is the key step that fixes inaccurate edges.
                val guidePx = IntArray(procBmp.width * procBmp.height)
                procBmp.getPixels(guidePx, 0, procBmp.width, 0, 0, procBmp.width, procBmp.height)

                // Scale guide to match mask dimensions if needed
                val guideW: Int; val guideH: Int; val guide: IntArray
                if (procBmp.width == maskW && procBmp.height == maskH) {
                    guideW = maskW; guideH = maskH; guide = guidePx
                } else {
                    val scaledG = Bitmap.createScaledBitmap(procBmp, maskW, maskH, true)
                    guideW = maskW; guideH = maskH
                    guide  = IntArray(maskW * maskH)
                    scaledG.getPixels(guide, 0, maskW, 0, 0, maskW, maskH)
                    scaledG.recycle()
                }

                val floatMask = FloatArray(guideW * guideH) { dilated[it].toFloat() }
                val refined   = guidedFilter(floatMask, guide, guideW, guideH, r = 10, eps = 0.04f)

                // 5. Write refined (still float) back as binary ALPHA_8
                val outBuf = java.nio.ByteBuffer.allocateDirect(capacity)
                for (v in refined) outBuf.put(if (v >= 0.5f) 255.toByte() else 0.toByte())
                outBuf.rewind()

                val maskBitmap = Bitmap.createBitmap(maskW, maskH, Bitmap.Config.ALPHA_8)
                maskBitmap.copyPixelsFromBuffer(outBuf)

                if (procBmp != bitmap) procBmp.recycle()
                return@withContext maskBitmap
            } else {
                if (procBmp != bitmap) procBmp.recycle()
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() { imageSegmenter?.close() }
}

// ─────────────────────────────────────────────────────────────────────────────
// Guided filter (He et al. 2013) — edge-preserving mask refinement
//
// Given a binary float mask p and an RGB guide image I (at the same resolution),
// this produces a smooth output q that snaps sharp transitions in p to strong
// edges in I.  Unlike a plain blur it PRESERVES edges from the guide image.
//
// Implementation uses two-pass (H+V) box filters for O(n) complexity.
// ─────────────────────────────────────────────────────────────────────────────
private fun guidedFilter(
    p: FloatArray, guide: IntArray, w: Int, h: Int,
    r: Int = 10, eps: Float = 0.04f
): FloatArray {
    val n = w * h

    // Grayscale guide [0,1]
    val I = FloatArray(n) { i ->
        val px = guide[i]
        (0.299f * ((px shr 16) and 0xFF) +
         0.587f * ((px shr 8)  and 0xFF) +
         0.114f * ( px         and 0xFF)) / 255f
    }

    // Two-pass box filter (horizontal then vertical)
    fun boxH(src: FloatArray): FloatArray {
        val dst  = FloatArray(n)
        val span = (2 * r + 1).toFloat()
        for (y in 0 until h) {
            val base = y * w
            var sum  = 0f
            for (kx in -r..r) sum += src[base + kx.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                dst[base + x] = sum / span
                sum -= src[base + (x - r).coerceIn(0, w - 1)]
                sum += src[base + (x + r + 1).coerceIn(0, w - 1)]
            }
        }
        return dst
    }
    fun boxV(src: FloatArray): FloatArray {
        val dst  = FloatArray(n)
        val span = (2 * r + 1).toFloat()
        for (x in 0 until w) {
            var sum = 0f
            for (ky in -r..r) sum += src[ky.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                dst[y * w + x] = sum / span
                sum -= src[(y - r).coerceIn(0, h - 1) * w + x]
                sum += src[(y + r + 1).coerceIn(0, h - 1) * w + x]
            }
        }
        return dst
    }
    fun box(src: FloatArray) = boxV(boxH(src))

    val II   = FloatArray(n) { I[it] * I[it] }
    val Ip   = FloatArray(n) { I[it] * p[it] }

    val mI   = box(I)
    val mP   = box(p)
    val mII  = box(II)
    val mIp  = box(Ip)

    val a = FloatArray(n) {
        val varI  = mII[it] - mI[it] * mI[it]
        val covIp = mIp[it] - mI[it] * mP[it]
        covIp / (varI + eps)
    }
    val b    = FloatArray(n) { mP[it] - a[it] * mI[it] }
    val mA   = box(a)
    val mB   = box(b)

    return FloatArray(n) { (mA[it] * I[it] + mB[it]).coerceIn(0f, 1f) }
}
