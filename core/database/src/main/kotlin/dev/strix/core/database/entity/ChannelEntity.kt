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
)
