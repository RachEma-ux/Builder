package com.builder.core.model.github

import com.google.gson.annotations.SerializedName

/**
 * GitHub repository response model.
 */
data class Repository(
    val id: Long,
    val name: String,
    @SerializedName("full_name")
    val fullName: String,
    val owner: Owner,
    val description: String?,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("default_branch")
    val defaultBranch: String,
    val private: Boolean,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class Owner(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url")
    val avatarUrl: String,
    val type: String
)
