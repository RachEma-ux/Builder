package com.builder.data.local.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.builder.core.model.Log
import com.builder.core.model.LogLevel
import com.builder.core.model.LogSource

/**
 * Room entity for storing log entries
 *
 * Logs are stored with foreign keys to instances for automatic cleanup when instances are deleted.
 * Indices on instanceId, packId, and timestamp enable efficient queries.
 */
@Entity(
    tableName = "logs",
    foreignKeys = [
        ForeignKey(
            entity = InstanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["instanceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("instanceId"),
        Index("packId"),
        Index("timestamp"),
        Index("level")
    ]
)
data class LogEntity(
    @PrimaryKey
    val id: String,
    val instanceId: String,
    val packId: String,
    val timestamp: Long,
    val level: String,
    val message: String,
    val source: String,
    val metadata: Map<String, String>
) {
    fun toDomain(): Log {
        return Log(
            id = id,
            instanceId = instanceId,
            packId = packId,
            timestamp = timestamp,
            level = LogLevel.fromString(level),
            message = message,
            source = LogSource.fromString(source),
            metadata = metadata
        )
    }

    companion object {
        fun fromDomain(log: Log): LogEntity {
            return LogEntity(
                id = log.id,
                instanceId = log.instanceId,
                packId = log.packId,
                timestamp = log.timestamp,
                level = log.level.name,
                message = log.message,
                source = log.source.name,
                metadata = log.metadata
            )
        }
    }
}
