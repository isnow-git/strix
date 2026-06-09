package dev.strix.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text search index for channels. A standalone FTS4 table kept in sync with
 * [ChannelEntity] inside the same write transaction.
 *
 * Performance notes:
 * - Only `name` and `groupTitle` are indexed; `channelId` is stored but `notIndexed`
 *   (it is just the join key back to `channels`), keeping the FTS index small.
 * - Living on disk means search adds no steady-state heap cost on low-RAM TVs.
 */
@Fts4(notIndexed = ["channelId"])
@Entity(tableName = "channels_fts")
data class ChannelFtsEntity(
    val channelId: String,
    val name: String,
    @ColumnInfo(name = "groupTitle")
    val groupTitle: String?,
)
