package com.builder.domain.pack

import com.builder.core.model.InstallSource
import com.builder.core.model.Pack
import com.builder.core.repository.PackRepository
import javax.inject.Inject

/**
 * Use case for installing a pack.
 * See Builder_Final.md §3 for pack installation requirements.
 */
class InstallPackUseCase @Inject constructor(
    private val packRepository: PackRepository
) {
    /**
     * Installs a pack from a download URL.
     *
     * @param downloadUrl URL to download the pack
     * @param installSource Source information (dev or prod)
     * @param expectedChecksum Optional checksum (mandatory for prod)
     * @return Result with installed Pack
     */
    suspend operator fun invoke(
        downloadUrl: String,
        installSource: InstallSource,
        expectedChecksum: String? = null
    ): Result<Pack> {
        return packRepository.installFromUrl(
            downloadUrl = downloadUrl,
            installSource = installSource,
            expectedChecksum = expectedChecksum
        )
    }
}
