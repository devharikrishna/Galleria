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
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.net.toUri
import com.irah.galleria.domain.model.Album
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.model.MediaOperationState
import com.irah.galleria.domain.model.OperationType
import com.irah.galleria.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaRepository {
    private val contentResolver: ContentResolver = context.contentResolver
    private val prefs = context.getSharedPreferences("media_favorites", Context.MODE_PRIVATE)
    private val favoritesFlow = MutableStateFlow(loadFavorites())

    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
    }

    override fun getMedia(): Flow<List<Media>> {
        val mediaFlow = callbackFlow {
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

        return combine(mediaFlow, favoritesFlow) { mediaList, favs ->
             mediaList.map { it.copy(isFavorite = favs.contains(it.id.toString())) }
        }
    }
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
    override fun getMediaByAlbumId(albumId: Long): Flow<List<Media>> {
        val mediaFlow = callbackFlow {
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
        
        return combine(mediaFlow, favoritesFlow) { mediaList, favs ->
             mediaList.map { it.copy(isFavorite = favs.contains(it.id.toString())) }
        }
    }
    private val _operationState = MutableStateFlow<MediaOperationState>(MediaOperationState.Idle)
    override val operationState: StateFlow<MediaOperationState> = _operationState.asStateFlow()

    private var lastProgressUpdate = 0L
    private val PROGRESS_UPDATE_INTERVAL = 100L // Update UI at most every 100ms

    private fun updateOperationState(state: MediaOperationState) {
        if (state is MediaOperationState.Running) {
            val currentTime = System.currentTimeMillis()
            if (state.progress == 0 || state.progress == state.total || 
                currentTime - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL) {
                _operationState.value = state
                lastProgressUpdate = currentTime
            }
        } else {
            _operationState.value = state
            lastProgressUpdate = 0L
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun deleteMedia(mediaList: List<Media>): android.content.IntentSender? = withContext(Dispatchers.IO) {
        if (mediaList.isEmpty()) return@withContext null
        updateOperationState(MediaOperationState.Running(OperationType.DELETE, 0, mediaList.size, ""))
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = mediaList.map { Uri.parse(it.uri) }
            val pi = MediaStore.createTrashRequest(contentResolver, uris, true)
            // For IntentSender requests, we mark as completed 'preparation' immediately
            updateOperationState(MediaOperationState.Completed(OperationType.DELETE, mediaList.size))
            return@withContext pi.intentSender
        } else {
            try {
                var deletedCount = 0
                for ((index, media) in mediaList.withIndex()) {
                    updateOperationState(MediaOperationState.Running(OperationType.DELETE, index + 1, mediaList.size, media.name))
                    try {
                        contentResolver.delete(Uri.parse(media.uri), null, null)
                        deletedCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                updateOperationState(MediaOperationState.Completed(OperationType.DELETE, deletedCount))
            } catch (e: android.app.RecoverableSecurityException) {
                updateOperationState(MediaOperationState.Error(OperationType.DELETE, "Permission required"))
                return@withContext e.userAction.actionIntent.intentSender
            } catch (e: Exception) {
                updateOperationState(MediaOperationState.Error(OperationType.DELETE, e.message ?: "Unknown error"))
            }
            return@withContext null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun moveMedia(mediaList: List<Media>, targetPath: String): android.content.IntentSender? = withContext(Dispatchers.IO) {
        val failedMoves = mutableListOf<Media>()
        val neededPermissions = mutableListOf<Uri>()
        updateOperationState(MediaOperationState.Running(OperationType.MOVE, 0, mediaList.size, ""))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for ((index, media) in mediaList.withIndex()) {
                 updateOperationState(MediaOperationState.Running(OperationType.MOVE, index + 1, mediaList.size, media.name))
                try {
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
                        throw java.io.IOException("Update failed for ${media.uri}")
                    }
                } catch (e: Exception) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && e is SecurityException) {
                        neededPermissions.add(Uri.parse(media.uri))
                        continue
                    }
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && e is android.app.RecoverableSecurityException) {
                         updateOperationState(MediaOperationState.Error(OperationType.MOVE, "Permission required"))
                        return@withContext e.userAction.actionIntent.intentSender
                    }
                    try {
                        if (doesSearchResultExist(media.name, targetPath)) {
                             failedMoves.add(media)
                        } else {
                            val newUri = copyMediaSingle(media, targetPath)
                            if (newUri != null) {
                                failedMoves.add(media)
                            }
                        }
                    } catch (copyEx: Exception) {
                        copyEx.printStackTrace()
                    }
                }
            }
            if (neededPermissions.isNotEmpty()) {
                 updateOperationState(MediaOperationState.Error(OperationType.MOVE, "Permissions required for ${neededPermissions.size} files"))
                val pi = MediaStore.createWriteRequest(contentResolver, neededPermissions)
                return@withContext pi.intentSender
            }
            if (failedMoves.isNotEmpty()) {
                 // For failed moves that were copied, we need to delete originals.
                 // This effectively becomes a "Delete" operation for those specific files.
                 // If we recursively call deleteForever, it might overwrite our MOVE state? 
                 // Yes, but deleteForever uses its own state updates. 
                 // Ideally we should handle this without clobbering state, but for now it's acceptable.
                 deleteForever(failedMoves)
            }
        } else {
            // Legacy support
            for ((index, media) in mediaList.withIndex()) {
                updateOperationState(MediaOperationState.Running(OperationType.MOVE, index + 1, mediaList.size, media.name))
                try {
                    val newUri = copyMediaSingle(media, targetPath)
                    if (newUri != null) {
                        deleteForever(listOf(media))
                    } else {
                         failedMoves.add(media)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    failedMoves.add(media)
                }
            }
        }
        updateOperationState(MediaOperationState.Completed(OperationType.MOVE, mediaList.size - failedMoves.size))
        return@withContext null
    }

    override suspend fun copyMedia(mediaList: List<Media>, targetPath: String) = withContext(Dispatchers.IO) {
        updateOperationState(MediaOperationState.Running(OperationType.COPY, 0, mediaList.size, ""))
        var successCount = 0

        
        for ((index, media) in mediaList.withIndex()) {
            updateOperationState(MediaOperationState.Running(OperationType.COPY, index + 1, mediaList.size, media.name))
            
            // Try copy directly. If it fails due to constraint, so be it, or handle rename.
            // For now, simpler is faster.
            if (copyMediaSingle(media, targetPath) != null) {
                successCount++
            }
        }
        updateOperationState(MediaOperationState.Completed(OperationType.COPY, successCount))
    }

    private fun doesSearchResultExist(name: String, targetPath: String): Boolean {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
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

    private fun copyMediaSingle(media: Media, targetPath: String): Uri? {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, media.name)
                put(MediaStore.MediaColumns.MIME_TYPE, media.mimeType)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, targetPath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)  
                } else {
                     val externalDir = android.os.Environment.getExternalStorageDirectory().absolutePath
                     val finalPath = java.io.File(externalDir, "$targetPath/${media.name}")
                     finalPath.parentFile?.mkdirs()
                     put(MediaStore.MediaColumns.DATA, finalPath.absolutePath)
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
                contentResolver.openInputStream(media.uri.toUri())?.use { input ->
                    contentResolver.openOutputStream(destUri)?.use { output ->
                        // Increase buffer size to 64KB (default is usually 8KB) for faster IO
                        input.copyTo(output, bufferSize = 64 * 1024) 
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
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        contentResolver.registerContentObserver(uri, true, observer)
        trySend(queryTrashedMedia())
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }.flowOn(Dispatchers.IO)

    override suspend fun restoreMedia(mediaList: List<Media>): android.content.IntentSender? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             updateOperationState(MediaOperationState.Running(OperationType.RESTORE, 0, mediaList.size, ""))
             val uris = mediaList.map { it.uri.toUri() }
             val pi = MediaStore.createTrashRequest(contentResolver, uris, false)
             updateOperationState(MediaOperationState.Completed(OperationType.RESTORE, mediaList.size))
             return@withContext pi.intentSender
        }
        return@withContext null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun deleteForever(mediaList: List<Media>): android.content.IntentSender? = withContext(Dispatchers.IO) {
        if (mediaList.isEmpty()) return@withContext null
        updateOperationState(MediaOperationState.Running(OperationType.DELETE_FOREVER, 0, mediaList.size, ""))
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = mediaList.map { it.uri.toUri() }
            val pi = MediaStore.createDeleteRequest(contentResolver, uris)
            updateOperationState(MediaOperationState.Completed(OperationType.DELETE_FOREVER, mediaList.size))
            return@withContext pi.intentSender
        } else {
            try {
                var deletedCount = 0
                for ((index, media) in mediaList.withIndex()) {
                    updateOperationState(MediaOperationState.Running(OperationType.DELETE_FOREVER, index + 1, mediaList.size, media.name))
                    try {
                        contentResolver.delete(media.uri.toUri(), null, null)
                        deletedCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                 updateOperationState(MediaOperationState.Completed(OperationType.DELETE_FOREVER, deletedCount))
            } catch (e: android.app.RecoverableSecurityException) {
                updateOperationState(MediaOperationState.Error(OperationType.DELETE_FOREVER, "Permission required"))
                return@withContext e.userAction.actionIntent.intentSender
            }
            return@withContext null
        }
    }
    override suspend fun getMediaById(id: Long): Media? = withContext(Dispatchers.IO) {
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
        val selection = "${MediaStore.MediaColumns._ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        val queryUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        try {
            contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
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

                    val retrievedId = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val size = cursor.getLong(sizeColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    var dateTaken = cursor.getLong(dateTakenColumn)
                    if (dateTaken == 0L) {
                        dateTaken = dateAdded * 1000L
                    }
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
                        retrievedId
                    )
                    
                    return@withContext Media(
                        id = retrievedId,
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
                        relativePath = relativePath,
                        isFavorite = favoritesFlow.value.contains(retrievedId.toString())
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
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
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        val queryUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
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
                    var dateTaken = cursor.getLong(dateTakenColumn)
                    if (dateTaken == 0L) {
                        dateTaken = dateAdded * 1000L
                    }
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
        val mediaList = queryMedia() 
        mediaList.forEach { media ->
            if (!albums.containsKey(media.bucketId)) {
                albums[media.bucketId] = Album(
                    id = media.bucketId,
                    name = media.bucketName,
                    relativePath = media.relativePath, 
                    uri = media.uri,  
                    count = 1,
                    timestamp = media.dateTaken
                )
            } else {
                val currentAlbum = albums[media.bucketId]!!
                albums[media.bucketId] = currentAlbum.copy(
                    count = currentAlbum.count + 1,
                    timestamp = maxOf(currentAlbum.timestamp, media.dateTaken)  
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
            putString("android:query-arg-sql-sort-order", "${MediaStore.MediaColumns.DATE_ADDED} DESC")
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
                    var dateTaken = cursor.getLong(dateTakenColumn)
                    if (dateTaken == 0L) {
                        dateTaken = dateAdded * 1000L
                    }
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
    private val favoritesLock = kotlinx.coroutines.sync.Mutex()

    override suspend fun toggleFavorite(mediaId: String) {
        favoritesLock.withLock {
            val current = favoritesFlow.value.toMutableSet()
            if (current.contains(mediaId)) {
                current.remove(mediaId)
            } else {
                current.add(mediaId)
            }
            prefs.edit { putStringSet("favorite_ids", current) }
            favoritesFlow.value = current
        }
    }

    override fun getFavorites(): Flow<List<Media>> {
        return getMedia().map { list ->
            list.filter { it.isFavorite }
        }
    }

    override fun getScreenshots(): Flow<List<Media>> {
        return getMedia().map { list ->
            list.filter { 
                it.bucketName.contains("Screenshot", ignoreCase = true) || 
                (it.relativePath?.contains("Screenshot", ignoreCase = true) == true)
            }
        }
    }
}
