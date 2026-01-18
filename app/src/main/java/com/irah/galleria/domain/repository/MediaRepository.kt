package com.irah.galleria.domain.repository

import com.irah.galleria.domain.model.Album
import com.irah.galleria.domain.model.Media
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMedia(): Flow<List<Media>>
    fun getAlbums(): Flow<List<Album>>
    fun getMediaByAlbumId(albumId: Long): Flow<List<Media>>
    
    suspend fun deleteMedia(mediaList: List<Media>): android.content.IntentSender?
    suspend fun moveMedia(mediaList: List<Media>, targetPath: String): android.content.IntentSender?
    fun getTrashedMedia(): Flow<List<Media>>
    suspend fun restoreMedia(mediaList: List<Media>): android.content.IntentSender?
    suspend fun deleteForever(mediaList: List<Media>): android.content.IntentSender?
}
