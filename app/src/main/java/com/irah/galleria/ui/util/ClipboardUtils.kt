package com.irah.galleria.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ClipboardUtils {

    fun copyImageToClipboard(context: Context, bitmap: Bitmap) {
        try {
            // 1. Save Bitmap to a temporary file in cache
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val file = File(cachePath, "sticker_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            // 2. Get content URI using FileProvider
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // 3. Create ClipData and set to Clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newUri(context.contentResolver, "Sticker", contentUri)
            
            // Note: For Android 12+ it's recommended to add the clip to the system clipboard
            // with a description of the content type.
            clipboard.setPrimaryClip(clip)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
