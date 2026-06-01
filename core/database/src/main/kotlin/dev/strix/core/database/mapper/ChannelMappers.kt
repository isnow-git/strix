package dev.strix.core.database.mapper

import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelId
import dev.strix.core.common.model.ChannelQuality
import dev.strix.core.database.entity.ChannelEntity
import dev.strix.core.database.entity.ChannelFtsEntity

/**
 * Entity <-> domain mapping. Kept as a single, direct transformation (no
 * intermediate DTO) to avoid per-item allocations on list scroll (ADR-0002).
 */

fun ChannelEntity.toDomain(): Channel =
    Channel(
        id = ChannelId(channelId),
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        group = groupTitle,
        number = number,
        qualityLabel = qualityLabel,
        displayName = ChannelQuality.displayName(name),
        epgChannelId = epgChannelId,
    )

/**
 * Maps a domain [Channel] to a row. [sortIndex] is supplied by the importer so
 * playlist order is preserved without a runtime sort. `rowid` is left at its
 * default so SQLite assigns it (or reuses it on conflict-replace by channelId).
 *
 * The grouping fields ([ChannelEntity.baseKey], rank, label) are derived here
 * from the name; [ChannelEntity.isPrimary] is left false and set in a single
 * pass after the import (see `ChannelDao.finalizeGroups`).
 */
fun Channel.toEntity(sortIndex: Int): ChannelEntity {
    val quality = ChannelQuality.parse(name)
    return ChannelEntity(
        channelId = id.value,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        groupTitle = ChannelQuality.cleanCategory(group),
        number = number,
        sortIndex = sortIndex,
        baseKey = ChannelQuality.groupKey(epgChannelId, quality),
        qualityRank = quality.qualityRank,
        qualityLabel = quality.qualityLabel,
        epgChannelId = epgChannelId,
        timeshift = quality.timeshift,
        epgBaseKey = ChannelQuality.epgBaseKey(quality),
    )
}

fun Channel.toFtsEntity(): ChannelFtsEntity =
    ChannelFtsEntity(
        channelId = id.value,
        name = name,
        groupTitle = group,
    )
