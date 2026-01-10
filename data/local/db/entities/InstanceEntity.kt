package com.builder.data.local.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.builder.core.model.Instance
import com.builder.core.model.InstanceState

/**
 * Room entity for pack instances.
 */
@Entity(
    tableName = "instances",
    foreignKeys = [
        ForeignKey(
            entity = PackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packId")]
)
data class InstanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val packId: String,

    val name: String,

    val state: String, // "stopped" | "running" | "paused"

    val createdAt: Long,

    val startedAt: Long? = null,

    val stoppedAt: Long? = null,

    val lastExitCode: Int? = null,

    val lastExitReason: String? = null
) {
    companion object {
        /**
         * Creates InstanceEntity from Instance domain model.
         */
        fun from(instance: Instance): InstanceEntity {
            return InstanceEntity(
                id = instance.id,
                packId = instance.packId,
                name = instance.name,
                state = instance.state.name.lowercase(),
                createdAt = instance.createdAt,
                startedAt = instance.startedAt,
                stoppedAt = instance.stoppedAt,
                lastExitCode = instance.lastExitCode,
                lastExitReason = instance.lastExitReason
            )
        }
    }

    /**
     * Converts InstanceEntity to Instance domain model.
     */
    fun toDomain(): Instance {
        return Instance(
            id = id,
            packId = packId,
            name = name,
            state = InstanceState.valueOf(state.uppercase()),
            createdAt = createdAt,
            startedAt = startedAt,
            stoppedAt = stoppedAt,
            lastExitCode = lastExitCode,
            lastExitReason = lastExitReason
        )
    }
}
