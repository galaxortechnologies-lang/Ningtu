package com.nigutu.app.domain.usecase

import com.nigutu.app.domain.repository.MusicRepository
import javax.inject.Inject

/** Re-scans the device media store and caches the metadata in Room. */
class RefreshLibraryUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(): Result<Int> = musicRepository.scanDeviceLibrary()
}
