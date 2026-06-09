package dev.strix.core.domain.repository

import dev.strix.core.common.result.StrixResult
import dev.strix.core.model.Channel
import dev.strix.core.model.ChannelId
import dev.strix.core.model.StreamSourceConfig
import kotlinx.coroutines.flow.Flow

/**
 * Read/refresh contract for channels. Implemented by the data layer (Room + network);
 * the domain and presentation layers depend only on this interface.
 *
 * Kept paging-agnostic on purpose: this module is pure Kotlin and must not depend on
 * androidx.paging. The Android `PagingSource` is exposed by :core:data on top of this.
 */
interface ChannelRepository {
    /**
     * Observes channels, optionally filtered by a search [query] (prefix match).
     * Emits a new list whenever the underlying store changes.
     */
    fun observeChannels(query: String? = null): Flow<List<Channel>>

    /** Looks up a single channel by id, or `null` if unknown. */
    suspend fun channelById(id: ChannelId): Channel?

    /** Looks up a channel by its fixed keypad [number] (remote zapping), or `null`. */
    suspend fun channelByNumber(number: Int): Channel?

    /** Number of channels currently stored; 0 means the catalogue needs importing. */
    suspend fun channelCount(): Int

    /** 0-based position of channel [id] within [category]'s list, for anchoring paging. */
    suspend fun positionInCategory(
        category: String,
        id: ChannelId,
    ): Int

    /** The next channel in playlist order (for zapping), or `null` at the end. */
    suspend fun nextChannel(id: ChannelId): Channel?

    /** The previous channel in playlist order, or `null` at the start. */
    suspend fun previousChannel(id: ChannelId): Channel?

    /** All quality variants of the channel [id] belongs to, best quality first. */
    suspend fun variants(id: ChannelId): List<Channel>

    /**
     * Refreshes the local store from [source] (streaming parse + batched write).
     * Returns the number of channels imported on success.
     */
    suspend fun refreshFrom(source: StreamSourceConfig): StrixResult<Int>
}
