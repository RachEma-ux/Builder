package com.builder.core.model.github

import com.google.gson.annotations.SerializedName

/**
 * GitHub repository public key for encrypting secrets.
 */
data class PublicKey(
    @SerializedName("key_id")
    val keyId: String,
    val key: String
)

/**
 * Request body for creating or updating a repository secret.
 */
data class CreateSecretRequest(
    @SerializedName("encrypted_value")
    val encryptedValue: String,
    @SerializedName("key_id")
    val keyId: String
)
