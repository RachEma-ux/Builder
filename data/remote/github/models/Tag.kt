package com.builder.data.remote.github.models

/**
 * GitHub tag response model.
 */
data class Tag(
    val name: String,
    val commit: TagCommit,
    val zipball_url: String,
    val tarball_url: String
)

data class TagCommit(
    val sha: String,
    val url: String
)
