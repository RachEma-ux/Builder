package com.builder.core.model.github

import com.google.gson.annotations.SerializedName

/**
 * GitHub Gist model.
 */
data class Gist(
    val id: String,
    val url: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    val description: String?,
    val public: Boolean,
    val files: Map<String, GistFile>,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val owner: GistOwner?
)

/**
 * Gist file content.
 */
data class GistFile(
    val filename: String?,
    val type: String?,
    val language: String?,
    @SerializedName("raw_url")
    val rawUrl: String?,
    val size: Long?,
    val content: String?
)

/**
 * Gist owner info.
 */
data class GistOwner(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url")
    val avatarUrl: String?
)

/**
 * Request body for creating a Gist.
 */
data class CreateGistRequest(
    val description: String?,
    val public: Boolean,
    val files: Map<String, GistFileContent>
)

/**
 * Gist file content for create/update requests.
 */
data class GistFileContent(
    val content: String
)

/**
 * Request body for updating a Gist.
 */
data class UpdateGistRequest(
    val description: String? = null,
    val files: Map<String, GistFileContent?>
)
