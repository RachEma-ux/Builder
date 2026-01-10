package com.builder.data.repository

import com.builder.core.model.InstallSource
import com.builder.core.model.Pack
import com.builder.core.repository.PackRepository
import com.builder.data.local.db.dao.PackDao
import com.builder.data.local.db.entities.PackEntity
import com.builder.data.local.storage.PackInstaller
import com.builder.data.local.storage.PackStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PackRepository.
 */
@Singleton
class PackRepositoryImpl @Inject constructor(
    private val packDao: PackDao,
    private val packStorage: PackStorage,
    private val packInstaller: PackInstaller
) : PackRepository {

    override suspend fun installFromUrl(
        downloadUrl: String,
        installSource: InstallSource,
        expectedChecksum: String?
    ): Result<Pack> {
        return try {
            Timber.i("Installing pack from URL: $downloadUrl")

            // Install the pack
            val installResult = packInstaller.installFromUrl(
                downloadUrl = downloadUrl,
                installSource = installSource,
                expectedChecksum = expectedChecksum
            ).getOrElse {
                return Result.failure(it)
            }

            // Create Pack domain object
            val pack = Pack(
                id = installResult.manifest.id,
                name = installResult.manifest.name,
                version = installResult.manifest.version,
                type = installResult.manifest.type,
                manifest = installResult.manifest,
                installSource = installResult.installSource,
                installPath = installResult.installPath,
                checksumSha256 = installResult.checksumSha256
            )

            // Save to database
            packDao.insert(PackEntity.from(pack))

            Timber.i("Pack installed and saved: ${pack.id}")
            Result.success(pack)
        } catch (e: Exception) {
            Timber.e(e, "Failed to install pack from URL")
            Result.failure(e)
        }
    }

    override suspend fun getPackById(packId: String): Pack? {
        return packDao.getById(packId)?.toDomain()
    }

    override fun getPackByIdFlow(packId: String): Flow<Pack?> {
        return packDao.getByIdFlow(packId).map { it?.toDomain() }
    }

    override suspend fun getAllPacks(): List<Pack> {
        return packDao.getAll().map { it.toDomain() }
    }

    override fun getAllPacksFlow(): Flow<List<Pack>> {
        return packDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun deletePack(packId: String): Result<Unit> {
        return try {
            // Delete from database
            packDao.deleteById(packId)

            // Delete pack files
            packStorage.deletePack(packId).getOrThrow()

            Timber.i("Pack deleted: $packId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete pack: $packId")
            Result.failure(e)
        }
    }

    override suspend fun isPackInstalled(packId: String): Boolean {
        return packDao.exists(packId)
    }
}
