package com.irah.galleria.domain.model
import androidx.compose.runtime.Immutable
@Immutable
data class Media(
    val id: Long,
    val uri: String,
    val path: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val timestamp: Long,
    val dateTaken: Long,
    val duration: Long? = null,  
    val width: Int = 0,
    val height: Int = 0,
    val bucketId: Long = 0,
    val bucketName: String = "",
    val relativePath: String? = null,
    val isFavorite: Boolean = false
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")
}