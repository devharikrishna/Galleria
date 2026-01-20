package com.irah.galleria.domain.usecase
import android.content.IntentSender
import com.irah.galleria.domain.model.Media
import com.irah.galleria.domain.repository.MediaRepository
import javax.inject.Inject
import com.irah.galleria.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
class DeleteMediaUseCase @Inject constructor(
    private val repository: MediaRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(mediaList: List<Media>): IntentSender? {
        val trashEnabled = settingsRepository.settings.first().trashEnabled
        return if (trashEnabled) {
            repository.deleteMedia(mediaList)
        } else {
            repository.deleteForever(mediaList)
        }
    }
}