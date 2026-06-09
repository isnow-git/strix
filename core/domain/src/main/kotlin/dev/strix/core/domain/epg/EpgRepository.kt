package dev.strix.core.domain.epg

import dev.strix.core.model.Channel
import dev.strix.core.model.epg.EpgProgramme
import dev.strix.core.model.epg.NowNext

/** Provides EPG for a channel; best-effort. */
interface EpgRepository {
    /** @return now/next, or null if no EPG is available for the channel. */
    suspend fun nowNext(channel: Channel): NowNext?

    /**
     * Programmes for [channel] overlapping the window [fromSec, toSec) (epoch seconds),
     * for the timeline guide. Empty when no EPG is available. XMLTV-backed.
     */
    suspend fun timeline(
        channel: Channel,
        fromSec: Long,
        toSec: Long,
    ): List<EpgProgramme>

    /**
     * Ingests the external XMLTV guide for the user's channels (authoritative source).
     * Safe to call after each catalogue import; a no-op on failure.
     */
    suspend fun refresh()
}
