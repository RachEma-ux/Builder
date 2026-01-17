package com.builder.core.repository

import com.builder.core.model.Secret
import com.builder.core.model.SecretInput
import com.builder.core.model.SecretMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing secrets (environment variables / credentials).
 * Secrets are stored encrypted and only exposed to authorized packs at runtime.
 */
interface SecretRepository {

    /**
     * Observable flow of all secret metadata (without values).
     */
    val secrets: Flow<List<SecretMetadata>>

    /**
     * Lists all secret keys with metadata (without values).
     */
    suspend fun listSecrets(): List<SecretMetadata>

    /**
     * Gets a secret value by key.
     * Returns null if the secret doesn't exist.
     *
     * @param key The secret key
     * @return The secret value or null
     */
    suspend fun getSecretValue(key: String): String?

    /**
     * Creates or updates a secret.
     *
     * @param input The secret key, value, and optional description
     * @return Result indicating success or failure
     */
    suspend fun setSecret(input: SecretInput): Result<Unit>

    /**
     * Deletes a secret by key.
     *
     * @param key The secret key to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteSecret(key: String): Result<Unit>

    /**
     * Checks if a secret exists.
     *
     * @param key The secret key
     * @return True if the secret exists
     */
    suspend fun hasSecret(key: String): Boolean

    /**
     * Gets secrets required by a pack that are missing.
     *
     * @param requiredEnv List of required environment variable names from pack.json
     * @return List of missing secret keys
     */
    suspend fun getMissingSecrets(requiredEnv: List<String>): List<String>

    /**
     * Gets all secret values for a pack's required environment.
     * Only returns secrets that exist.
     *
     * @param requiredEnv List of required environment variable names
     * @return Map of key to value for existing secrets
     */
    suspend fun getSecretsForPack(requiredEnv: List<String>): Map<String, String>
}
