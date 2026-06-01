package dev.strix.core.database.mapper

import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelClassifier
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
        // Read straight from the row: the (regex-heavy) clean-up ran once at import.
        displayName = displayName,
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
        // Reuse the parsed quality so the name is only scanned once per import.
        displayName = ChannelQuality.displayName(name, quality),
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
        // Prefer the category resolved at import (iptv-org); else keyword fallback.
        category = category.ifBlank { ChannelClassifier.classify(name, group).label },
    )
}

fun Channel.toFtsEntity(): ChannelFtsEntity =
    ChannelFtsEntity(
        channelId = id.value,
        name = name,
        groupTitle = group,
    )
