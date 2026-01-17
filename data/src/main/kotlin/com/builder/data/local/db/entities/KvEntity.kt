package com.builder.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index

/**
 * Room entity for key-value storage.
 *
 * Stores per-pack key-value pairs for workflow state persistence.
 * Composite primary key of (packId, key) ensures uniqueness.
 */
@Entity(
    tableName = "kv_store",
    primaryKeys = ["packId", "key"],
    indices = [
        Index("packId")
    ]
)
data class KvEntity(
    val packId: String,
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)
