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

/** Provides now/next EPG for a channel; best-effort. */
interface EpgRepository {
    /** @return now/next, or null if no EPG is available for the channel. */
    suspend fun nowNext(channel: Channel): NowNext?

    /**
     * Ingests the external XMLTV guide for the user's channels (authoritative
     * source). Safe to call after each catalogue import; no-op on failure.
     */
    suspend fun refresh()
}
