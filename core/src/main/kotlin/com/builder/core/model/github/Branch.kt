package com.builder.core.model.github

/**
 * GitHub branch response model.
 */
data class Branch(
    val name: String,
    val commit: BranchCommit,
    val protected: Boolean
)

data class BranchCommit(
    val sha: String,
    val url: String
)
