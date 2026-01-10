package com.builder.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.builder.core.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room entity for installed packs.
 */
@Entity(tableName = "packs")
data class PackEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    val version: String,

    val type: String, // "wasm" | "workflow"

    val installMode: String, // "DEV" | "PROD"

    val sourceRef: String, // branch/commit or tag

    val sourceUrl: String,

    val installedPath: String,

    val installedAt: Long,

    val manifestJson: String, // Full pack.json serialized

    val checksumSha256: String
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Creates PackEntity from Pack domain model.
         */
        fun from(pack: Pack): PackEntity {
            return PackEntity(
                id = pack.id,
                name = pack.name,
                version = pack.version,
                type = pack.type.name.lowercase(),
                installMode = pack.installSource.mode,
                sourceRef = pack.installSource.sourceRef,
                sourceUrl = pack.installSource.sourceUrl,
                installedPath = pack.installPath,
                installedAt = pack.installSource.installedAt,
                manifestJson = json.encodeToString(pack.manifest),
                checksumSha256 = pack.checksumSha256
            )
        }
    }

    /**
     * Converts PackEntity to Pack domain model.
     */
    fun toDomain(): Pack {
        return Pack(
            id = id,
            name = name,
            version = version,
            type = PackType.valueOf(type.uppercase()),
            manifest = json.decodeFromString<PackManifest>(manifestJson),
            installSource = InstallSource(
                mode = installMode,
                sourceRef = sourceRef,
                sourceUrl = sourceUrl,
                installedAt = installedAt
            ),
            installPath = installedPath,
            checksumSha256 = checksumSha256
        )
    }
}
