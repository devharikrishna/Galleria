package com.irah.galleria.ui.pdfexport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class PdfExportState(
    val mediaList: List<Media> = emptyList(),
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val exportedUri: Uri? = null,
    val error: String? = null
)

@HiltViewModel
class PdfExportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PdfExportState())
    val state: StateFlow<PdfExportState> = _state.asStateFlow()

    init {
        val mediaIdsString: String = savedStateHandle["mediaIds"] ?: ""
        val mediaIds = mediaIdsString.split(",").mapNotNull { it.trim().toLongOrNull() }
        loadMedia(mediaIds)
    }

    private fun loadMedia(ids: List<Long>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val mediaItems = ids.mapNotNull { mediaRepository.getMediaById(it) }
            _state.value = _state.value.copy(mediaList = mediaItems, isLoading = false)
        }
    }

    fun swapItems(fromIndex: Int, toIndex: Int) {
        val currentList = _state.value.mediaList.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _state.value = _state.value.copy(mediaList = currentList)
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true, error = null)
            try {
                val uri = withContext(Dispatchers.IO) {
                    generatePdf(context, _state.value.mediaList)
                }
                _state.value = _state.value.copy(isExporting = false, exportedUri = uri)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isExporting = false, error = e.message ?: "Failed to export PDF")
            }
        }
    }

    private fun generatePdf(context: Context, mediaList: List<Media>): Uri {
        val document = PdfDocument()

        val a4Short = 595
        val a4Long = 842
        val margin = 40

        for ((index, media) in mediaList.withIndex()) {
            val rawBitmap = loadBitmap(context, media.uri.toUri(), a4Long - margin * 2)
                ?: continue
            val bitmap = applyExifRotation(context, media.uri.toUri(), rawBitmap)

            val isLandscape = bitmap.width > bitmap.height
            val pageWidth = if (isLandscape) a4Long else a4Short
            val pageHeight = if (isLandscape) a4Short else a4Long

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val availableWidth = pageWidth - margin * 2
            val availableHeight = pageHeight - margin * 2

            val scale = minOf(
                availableWidth.toFloat() / bitmap.width,
                availableHeight.toFloat() / bitmap.height
            )
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()

            val left = margin + (availableWidth - scaledWidth) / 2f
            val top = margin + (availableHeight - scaledHeight) / 2f

            val destRect = android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight)
            canvas.drawBitmap(bitmap, null, destRect, null)
            bitmap.recycle()

            document.finishPage(page)
        }

        val cacheDir = File(context.cacheDir, "pdf_exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, "Galleria_Export_${System.currentTimeMillis()}.pdf")
        file.outputStream().use { outputStream ->
            document.writeTo(outputStream)
        }
        document.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = androidx.exifinterface.media.ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            val matrix = android.graphics.Matrix()
            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.preScale(-1f, 1f)
                }
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun loadBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                val sampleSize = calculateInSampleSize(options, maxDimension, maxDimension)

                context.contentResolver.openInputStream(uri)?.use { stream2 ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }
                    BitmapFactory.decodeStream(stream2, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

