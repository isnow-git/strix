package dev.strix.core.database.mapper

import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelId
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
    )

/**
 * Maps a domain [Channel] to a row. [sortIndex] is supplied by the importer so
 * playlist order is preserved without a runtime sort. `rowid` is left at its
 * default so SQLite assigns it (or reuses it on conflict-replace by channelId).
 */
fun Channel.toEntity(sortIndex: Int): ChannelEntity =
    ChannelEntity(
        channelId = id.value,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        groupTitle = group,
        number = number,
        sortIndex = sortIndex,
    )

fun Channel.toFtsEntity(): ChannelFtsEntity =
    ChannelFtsEntity(
        channelId = id.value,
        name = name,
        groupTitle = group,
    )
