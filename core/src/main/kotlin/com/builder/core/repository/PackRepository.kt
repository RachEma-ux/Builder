package com.builder.core.repository

import com.builder.core.model.InstallSource
import com.builder.core.model.Pack
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for pack operations.
 */
interface PackRepository {
    /**
     * Installs a pack from a URL.
     */
    suspend fun installFromUrl(
        downloadUrl: String,
        installSource: InstallSource,
        expectedChecksum: String? = null
    ): Result<Pack>

    /**
     * Gets a pack by ID.
     */
    suspend fun getPackById(packId: String): Pack?

    /**
     * Gets a pack by ID as Flow.
     */
    fun getPackByIdFlow(packId: String): Flow<Pack?>

    /**
     * Gets all installed packs.
     */
    suspend fun getAllPacks(): List<Pack>

    /**
     * Gets all installed packs as Flow.
     */
    fun getAllPacksFlow(): Flow<List<Pack>>

    /**
     * Deletes a pack.
     */
    suspend fun deletePack(packId: String): Result<Unit>

    /**
     * Checks if a pack is installed.
     */
    suspend fun isPackInstalled(packId: String): Boolean
}
