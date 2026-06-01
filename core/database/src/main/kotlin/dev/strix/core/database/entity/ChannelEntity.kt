package dev.strix.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted channel row.
 *
 * Performance notes:
 * - `rowid` is an `INTEGER PRIMARY KEY` so it aliases SQLite's rowid (no extra
 *   index, fastest lookups and the natural join key for paging).
 * - `channelId` carries a unique index for upsert-by-id during a refresh.
 * - `sortIndex` preserves playlist order so list queries never sort at runtime.
 * - `groupTitle` is indexed for fast group filtering.
 */
@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["channelId"], unique = true),
        Index(value = ["groupTitle"]),
        Index(value = ["sortIndex"]),
        Index(value = ["baseKey"]),
        Index(value = ["isPrimary"]),
        Index(value = ["epgBaseKey"]),
    ],
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid")
    val rowid: Long = 0,
    val channelId: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val groupTitle: String?,
    val number: Int?,
    val sortIndex: Int,
    /** Quality-independent grouping key shared by a channel's variants. */
    val baseKey: String,
    /** Quality sort rank, lower = better. */
    val qualityRank: Int,
    /** Quality label of this variant (e.g. "FHD"), or null. */
    val qualityLabel: String?,
    /** True for the representative (best-quality) variant of each [baseKey]. */
    val isPrimary: Boolean = false,
    /** Provider EPG id (empty when unmapped), used to fetch/inherit EPG. */
    val epgChannelId: String?,
    /** Time-shift in hours (0 = live, >0 delayed, -1 unknown), for EPG offset. */
    val timeshift: Int,
    /** Logical-channel key ignoring time-shift, for inheriting an EPG source. */
    val epgBaseKey: String,
)
