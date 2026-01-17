package com.builder.core.model

/**
 * Represents a secret (environment variable / credential) stored securely.
 * The actual value is stored encrypted and only accessed when needed.
 */
data class Secret(
    /**
     * Unique key for the secret (e.g., "OPENAI_API_KEY", "DATABASE_URL").
     * Must match the name in pack.json requiredEnv.
     */
    val key: String,

    /**
     * Human-readable description of what this secret is for.
     */
    val description: String = "",

    /**
     * When the secret was created/last updated (ISO 8601 timestamp).
     */
    val updatedAt: String,

    /**
     * Whether the secret has a value set (without exposing the value).
     */
    val hasValue: Boolean = false
)

/**
 * Metadata about a secret without the actual value.
 * Used for listing secrets in the UI.
 */
data class SecretMetadata(
    val key: String,
    val description: String,
    val updatedAt: String,
    val usedByPacks: List<String> = emptyList()
)

/**
 * Request to create or update a secret.
 */
data class SecretInput(
    val key: String,
    val value: String,
    val description: String = ""
) {
    init {
        require(key.isNotBlank()) { "Secret key cannot be blank" }
        require(key.matches(Regex("^[A-Z][A-Z0-9_]*$"))) {
            "Secret key must be uppercase with underscores (e.g., API_KEY)"
        }
    }
}
