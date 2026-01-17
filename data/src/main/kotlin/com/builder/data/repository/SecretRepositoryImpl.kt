package com.builder.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.builder.core.model.SecretInput
import com.builder.core.model.SecretMetadata
import com.builder.core.repository.SecretRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SecretRepository using EncryptedSharedPreferences.
 * Secrets are stored with AES-256 encryption.
 */
@Singleton
class SecretRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SecretRepository {

    companion object {
        private const val PREFS_NAME = "builder_secrets"
        private const val METADATA_PREFS_NAME = "builder_secrets_metadata"
        private const val KEY_PREFIX_VALUE = "secret_value_"
        private const val KEY_PREFIX_DESC = "secret_desc_"
        private const val KEY_PREFIX_UPDATED = "secret_updated_"
        private const val KEY_ALL_KEYS = "all_secret_keys"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Encrypted storage for secret values
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Regular prefs for metadata (descriptions, timestamps - not sensitive)
    private val metadataPrefs: SharedPreferences = context.getSharedPreferences(
        METADATA_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _secretsFlow = MutableStateFlow<List<SecretMetadata>>(emptyList())
    override val secrets: Flow<List<SecretMetadata>> = _secretsFlow.asStateFlow()

    init {
        // Load initial secrets
        refreshSecretsFlow()
    }

    private fun refreshSecretsFlow() {
        _secretsFlow.value = getAllSecretKeys().map { key ->
            SecretMetadata(
                key = key,
                description = metadataPrefs.getString("${KEY_PREFIX_DESC}$key", "") ?: "",
                updatedAt = metadataPrefs.getString("${KEY_PREFIX_UPDATED}$key", "") ?: ""
            )
        }
    }

    private fun getAllSecretKeys(): Set<String> {
        val keysString = metadataPrefs.getString(KEY_ALL_KEYS, "") ?: ""
        return if (keysString.isBlank()) {
            emptySet()
        } else {
            keysString.split(",").toSet()
        }
    }

    private fun saveAllSecretKeys(keys: Set<String>) {
        metadataPrefs.edit()
            .putString(KEY_ALL_KEYS, keys.joinToString(","))
            .apply()
    }

    override suspend fun listSecrets(): List<SecretMetadata> = withContext(Dispatchers.IO) {
        getAllSecretKeys().map { key ->
            SecretMetadata(
                key = key,
                description = metadataPrefs.getString("${KEY_PREFIX_DESC}$key", "") ?: "",
                updatedAt = metadataPrefs.getString("${KEY_PREFIX_UPDATED}$key", "") ?: ""
            )
        }.sortedBy { it.key }
    }

    override suspend fun getSecretValue(key: String): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString("${KEY_PREFIX_VALUE}$key", null)
    }

    override suspend fun setSecret(input: SecretInput): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val timestamp = Instant.now().toString()

            // Store encrypted value
            encryptedPrefs.edit()
                .putString("${KEY_PREFIX_VALUE}${input.key}", input.value)
                .apply()

            // Store metadata
            metadataPrefs.edit()
                .putString("${KEY_PREFIX_DESC}${input.key}", input.description)
                .putString("${KEY_PREFIX_UPDATED}${input.key}", timestamp)
                .apply()

            // Update keys list
            val keys = getAllSecretKeys().toMutableSet()
            keys.add(input.key)
            saveAllSecretKeys(keys)

            Timber.i("Secret saved: ${input.key}")
            refreshSecretsFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save secret: ${input.key}")
            Result.failure(e)
        }
    }

    override suspend fun deleteSecret(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Remove encrypted value
            encryptedPrefs.edit()
                .remove("${KEY_PREFIX_VALUE}$key")
                .apply()

            // Remove metadata
            metadataPrefs.edit()
                .remove("${KEY_PREFIX_DESC}$key")
                .remove("${KEY_PREFIX_UPDATED}$key")
                .apply()

            // Update keys list
            val keys = getAllSecretKeys().toMutableSet()
            keys.remove(key)
            saveAllSecretKeys(keys)

            Timber.i("Secret deleted: $key")
            refreshSecretsFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete secret: $key")
            Result.failure(e)
        }
    }

    override suspend fun hasSecret(key: String): Boolean = withContext(Dispatchers.IO) {
        encryptedPrefs.contains("${KEY_PREFIX_VALUE}$key")
    }

    override suspend fun getMissingSecrets(requiredEnv: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            requiredEnv.filter { key ->
                !encryptedPrefs.contains("${KEY_PREFIX_VALUE}$key")
            }
        }

    override suspend fun getSecretsForPack(requiredEnv: List<String>): Map<String, String> =
        withContext(Dispatchers.IO) {
            requiredEnv.mapNotNull { key ->
                val value = encryptedPrefs.getString("${KEY_PREFIX_VALUE}$key", null)
                if (value != null) key to value else null
            }.toMap()
        }
}
