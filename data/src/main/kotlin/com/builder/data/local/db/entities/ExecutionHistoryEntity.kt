package com.builder.data.local.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing WASM pack execution history.
 */
@Entity(
    tableName = "execution_history",
    foreignKeys = [
        ForeignKey(
            entity = PackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("packId"),
        Index("executedAt"),
        Index("status")
    ]
)
data class ExecutionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val packId: String,

    val packName: String,

    val runId: Long,

    val status: String, // SUCCESS, FAILURE, CANCELLED, UNKNOWN

    val output: String,

    val executedAt: Long, // timestamp in millis

    val duration: Long?, // in milliseconds

    val artifactUrl: String?,

    val sourceRef: String, // The ref/tag used for execution

    val workflowName: String = "ci.yml"
)
