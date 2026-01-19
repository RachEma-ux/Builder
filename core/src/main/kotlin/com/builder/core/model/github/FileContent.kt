package com.builder.core.model.github

import com.google.gson.annotations.SerializedName
import java.util.Base64

/**
 * GitHub file content response model.
 */
data class FileContent(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long,
    val type: String,
    val content: String?,
    val encoding: String?,
    @SerializedName("download_url")
    val downloadUrl: String?,
    @SerializedName("html_url")
    val htmlUrl: String?
) {
    /**
     * Decodes base64 content to string.
     */
    fun decodeContent(): String? {
        return content?.let {
            try {
                String(Base64.getDecoder().decode(it.replace("\n", "")))
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Request body for creating or updating a file.
 */
data class FileUpdateRequest(
    val message: String,
    val content: String,  // Base64 encoded content
    val sha: String? = null,  // Required for updates, null for creates
    val branch: String? = null
) {
    companion object {
        /**
         * Creates a request with base64 encoded content.
         */
        fun create(
            message: String,
            rawContent: String,
            sha: String? = null,
            branch: String? = null
        ): FileUpdateRequest {
            val encoded = Base64.getEncoder().encodeToString(rawContent.toByteArray())
            return FileUpdateRequest(message, encoded, sha, branch)
        }
    }
}

/**
 * Response for file creation/update.
 */
data class FileUpdateResponse(
    val content: FileContent?,
    val commit: CommitInfo
)

data class CommitInfo(
    val sha: String,
    val message: String,
    @SerializedName("html_url")
    val htmlUrl: String?
)
