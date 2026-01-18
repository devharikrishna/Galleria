package com.irah.galleria.domain.usecase

import android.content.IntentSender
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.repository.MediaRepository
import javax.inject.Inject

class DeleteMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaList: List<Media>): IntentSender? {
        return repository.deleteMedia(mediaList)
    }
}
