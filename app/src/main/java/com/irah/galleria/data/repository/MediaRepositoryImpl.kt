package com.irah.galleria.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.irah.galleria.domain.model.Album
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    override fun getMedia(): Flow<List<Media>> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryMedia())
            }
        }
        contentResolver.registerContentObserver(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            },
            true,
            observer
        )
        contentResolver.registerContentObserver(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            },
            true,
            observer
        )

        trySend(queryMedia())

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    override fun getAlbums(): Flow<List<Album>> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryAlbums())
            }
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        
        contentResolver.registerContentObserver(uri, true, observer)
        trySend(queryAlbums())
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }.flowOn(Dispatchers.IO)

    override fun getMediaByAlbumId(albumId: Long): Flow<List<Media>> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryMedia(albumId))
            }
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        contentResolver.registerContentObserver(uri, true, observer)
        trySend(queryMedia(albumId))
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }.flowOn(Dispatchers.IO)

    override suspend fun deleteMedia(mediaList: List<Media>): android.content.IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = mediaList.map { Uri.parse(it.uri) }
            val pi = MediaStore.createTrashRequest(contentResolver, uris, true) // Trash it
            return pi.intentSender
        } else {
            try {
                for (media in mediaList) {
                    contentResolver.delete(Uri.parse(media.uri), null, null)
                }
            } catch (e: android.app.RecoverableSecurityException) {
                return e.userAction.actionIntent.intentSender
            }
            return null
        }
    }

    override suspend fun moveMedia(mediaList: List<Media>, targetPath: String): android.content.IntentSender? {
        // Fallback strategy: Copy to new location -> Delete original
        // This is necessary because updating RELATIVE_PATH is often restricted or flaky across different Android versions/vendors.
        
        val failedMoves = mutableListOf<Media>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (media in mediaList) {
                try {
                    // Method 1: Try direct RELATIVE_PATH update (Fastest)
                    // If the target path is the same, skip
                    if (media.relativePath == targetPath || media.relativePath == "$targetPath/") continue

                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, targetPath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val rows = contentResolver.update(Uri.parse(media.uri), values, null, null)
                    
                    if (rows > 0) {
                        val finalValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                         contentResolver.update(Uri.parse(media.uri), finalValues, null, null)
                    } else {
                        throw SecurityException("Update failed for ${media.uri}")
                    }
                } catch (e: Exception) {
                    // Method 2: Copy + Delete (Robust)
                    // If direct update fails (even with RecSecEx), fallback to Copy+Delete immediately.
                    // This allows us to process the whole batch and request Delete permission for all originals at once (Android 11+).
                    
                    // Attempt Copy
                    try {
                        // CHECK DUPLICATE BEFORE COPYING to prevent duplication loop on retry
                        if (doesSearchResultExist(media.name, targetPath)) {
                             // Already copied (likely from previous attempt), just needs deletion
                             failedMoves.add(media)
                        } else {
                            val newUri = copyMedia(media, targetPath)
                            if (newUri != null) {
                                // Copy successful, track original for deletion
                                failedMoves.add(media)
                            }
                        }
                    } catch (copyEx: Exception) {
                        copyEx.printStackTrace()
                    }
                }
            }
            
            if (failedMoves.isNotEmpty()) {
                // We have successfully copied "failedMoves", now delete the originals
                // Android R+ needs createTrashRequest/createDeleteRequest logic which deleteMedia already has
                return deleteMedia(failedMoves)
            }
        }
        return null
    }

    private fun doesSearchResultExist(name: String, targetPath: String): Boolean {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        // RELATIVE_PATH in DB usually ends with /
        val pathArg = if (targetPath.endsWith("/")) targetPath else "$targetPath/"
        val selectionArgs = arrayOf(name, pathArg)
        val queryUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        
        try {
            contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
                return cursor.count > 0
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    private fun copyMedia(media: Media, targetPath: String): Uri? {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, media.name)
                put(MediaStore.MediaColumns.MIME_TYPE, media.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, targetPath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 1) // Pending
                }
            }

            val collection = if (media.mimeType.startsWith("video")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                 else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val newUri = contentResolver.insert(collection, contentValues)

            newUri?.let { destUri ->
                contentResolver.openInputStream(Uri.parse(media.uri))?.use { input ->
                    contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val finalValues = ContentValues().apply {
                         put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    contentResolver.update(destUri, finalValues, null, null)
                }
                return destUri
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun getTrashedMedia(): Flow<List<Media>> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryTrashedMedia())
            }
        }
        // Register observer on external content (same uri)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        contentResolver.registerContentObserver(uri, true, observer)
        trySend(queryTrashedMedia())
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }.flowOn(Dispatchers.IO)

    override suspend fun restoreMedia(mediaList: List<Media>): android.content.IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             val uris = mediaList.map { Uri.parse(it.uri) }
             val pi = MediaStore.createTrashRequest(contentResolver, uris, false) // Untrash
             return pi.intentSender
        }
        return null
    }

    override suspend fun deleteForever(mediaList: List<Media>): android.content.IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = mediaList.map { Uri.parse(it.uri) }
            val pi = MediaStore.createDeleteRequest(contentResolver, uris) // Permanent Delete
            return pi.intentSender
        } else {
            // Pre-Android 11, try to delete directly (same as deleteMedia behavior on older APIs)
            try {
                for (media in mediaList) {
                    contentResolver.delete(Uri.parse(media.uri), null, null)
                }
            } catch (e: android.app.RecoverableSecurityException) {
                return e.userAction.actionIntent.intentSender
            }
            return null
        }
    }

    private fun queryMedia(albumId: Long? = null): List<Media> {
        val mediaList = mutableListOf<Media>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
        )

        val selection = if (albumId != null) {
            "${MediaStore.MediaColumns.BUCKET_ID} = ?"
        } else {
            null
        }
        
        val selectionArgs = if (albumId != null) {
            arrayOf(albumId.toString())
        } else {
            null
        }

        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

        val queryUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        
        // We only want images and videos
        val selectionMimeType = if (selection == null) {
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
        } else {
            "$selection AND (${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
        }
        
        val selectionArgsMimeType = if (selectionArgs == null) {
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
        } else {
            selectionArgs + arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
        }

        try {
            contentResolver.query(
                queryUri,
                projection,
                selectionMimeType,
                selectionArgsMimeType,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val durationColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val size = cursor.getLong(sizeColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateTaken = cursor.getLong(dateTakenColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val bucketId = cursor.getLong(bucketIdColumn)
                    val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"
                    val width = if (widthColumn != -1) cursor.getInt(widthColumn) else 0
                    val height = if (heightColumn != -1) cursor.getInt(heightColumn) else 0
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else null

                    val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                           cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))
                        } catch(e: Exception) { null }
                    } else null

                    val contentUri = ContentUris.withAppendedId(
                        if (mimeType.startsWith("video")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI 
                        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    mediaList.add(
                        Media(
                            id = id,
                            uri = contentUri.toString(),
                            path = path,
                            name = name,
                            size = size,
                            mimeType = mimeType,
                            timestamp = dateAdded,
                            dateTaken = dateTaken,
                            duration = duration,
                            width = width,
                            height = height,
                            bucketId = bucketId,
                            bucketName = bucketName,
                            relativePath = relativePath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mediaList
    }

    private fun queryAlbums(): List<Album> {
        val albums = mutableMapOf<Long, Album>()
        
        // Re-use queryMedia to get all media and group by bucketId
        // Ideally we should do a projection query for just buckets, but MediaStore is tricky with 'GROUP BY' via ContentResolver
        // So fetching all media and grouping is a safe (though less efficient) default for MVP.
        // For production, we can optimize by querying distinct bucket_ids if possible, or maintaining a separate cache.
        // However, standard approach often involves iterating.
        
        // Let's do a lighter query just for buckets if possible, but distinct is hard.
        // Let's just iterate the media query results since we likely cache it in UI layer or ViewModel.
        
        val mediaList = queryMedia() 
        
        mediaList.forEach { media ->
            if (!albums.containsKey(media.bucketId)) {
                albums[media.bucketId] = Album(
                    id = media.bucketId,
                    name = media.bucketName,
                    relativePath = media.relativePath, 
                    uri = media.uri, // Use first image as cover
                    count = 1,
                    timestamp = media.dateTaken
                )
            } else {
                val currentAlbum = albums[media.bucketId]!!
                albums[media.bucketId] = currentAlbum.copy(
                    count = currentAlbum.count + 1,
                    timestamp = maxOf(currentAlbum.timestamp, media.dateTaken) // Keep latest
                )
            }
        }
        
        return albums.values.sortedByDescending { it.timestamp }
    }


    private fun queryTrashedMedia(): List<Media> {
        val mediaList = mutableListOf<Media>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return mediaList

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION
        )

        val bundle = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putString("android:query-arg-sql-sort-order", "${MediaStore.MediaColumns.DATE_TAKEN} DESC")
        }

        val queryUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

        try {
            contentResolver.query(
                queryUri,
                projection,
                bundle,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val durationColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val size = cursor.getLong(sizeColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateTaken = cursor.getLong(dateTakenColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val bucketId = cursor.getLong(bucketIdColumn)
                    val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"
                    val width = if (widthColumn != -1) cursor.getInt(widthColumn) else 0
                    val height = if (heightColumn != -1) cursor.getInt(heightColumn) else 0
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else null

                    val contentUri = ContentUris.withAppendedId(
                        if (mimeType.startsWith("video")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI 
                        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    mediaList.add(
                        Media(
                            id = id,
                            uri = contentUri.toString(),
                            path = path,
                            name = name,
                            size = size,
                            mimeType = mimeType,
                            timestamp = dateAdded,
                            dateTaken = dateTaken,
                            duration = duration,
                            width = width,
                            height = height,
                            bucketId = bucketId,
                            bucketName = bucketName,
                            relativePath = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mediaList
    }
}
