package com.builder.core.model.github

import com.google.gson.annotations.SerializedName

/**
 * GitHub release response model.
 */
data class Release(
    val id: Long,
    @SerializedName("tag_name")
    val tagName: String,
    val name: String?,
    val body: String?,
    val draft: Boolean,
    val prerelease: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("published_at")
    val publishedAt: String?,
    @SerializedName("html_url")
    val htmlUrl: String,
    val assets: List<ReleaseAsset>,
    @SerializedName("upload_url")
    val uploadUrl: String
) {
    /**
     * Finds a release asset by name.
     */
    fun findAsset(assetName: String): ReleaseAsset? {
        return assets.find { it.name == assetName }
    }

    /**
     * Gets the packs.index.json asset if present.
     */
    fun getPacksIndex(): ReleaseAsset? {
        return findAsset("packs.index.json")
    }

    /**
     * Gets the checksums.sha256 asset if present.
     */
    fun getChecksums(): ReleaseAsset? {
        return findAsset("checksums.sha256")
    }
}

data class ReleaseAsset(
    val id: Long,
    val name: String,
    val label: String?,
    @SerializedName("content_type")
    val contentType: String,
    val size: Long,
    @SerializedName("download_count")
    val downloadCount: Int,
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)
