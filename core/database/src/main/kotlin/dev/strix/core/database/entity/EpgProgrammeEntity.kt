package dev.strix.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A programme from an ingested XMLTV guide, keyed by a [normChannelId]
 * (see `normalizeEpgId`) and indexed by (channel, start) for fast now/next.
 */
@Entity(
    tableName = "epg_programmes",
    indices = [Index(value = ["normChannelId", "startSec"])],
)
data class EpgProgrammeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val normChannelId: String,
    val startSec: Long,
    val stopSec: Long,
    val title: String,
    val description: String? = null,
)
