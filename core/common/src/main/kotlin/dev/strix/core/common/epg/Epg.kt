package dev.strix.core.common.epg

import dev.strix.core.common.model.Channel

/** One programme in the guide. Times are Unix epoch seconds. */
data class EpgProgramme(
    val title: String,
    val startEpochSec: Long,
    val endEpochSec: Long,
)

/** The current and upcoming programme for a channel. */
data class NowNext(
    val current: EpgProgramme?,
    val next: EpgProgramme?,
)

/** Provides now/next EPG for a channel (Xtream `get_short_epg`); best-effort. */
interface EpgRepository {
    /** @return now/next, or null if the source has no EPG (e.g. plain M3U). */
    suspend fun nowNext(channel: Channel): NowNext?
}
