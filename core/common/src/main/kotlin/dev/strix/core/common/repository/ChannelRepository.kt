package dev.strix.core.common.repository

import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelId
import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.core.common.result.StrixResult
import kotlinx.coroutines.flow.Flow

/**
 * Read/refresh contract for channels. Implemented by the data layer (Room +
 * network); the domain and presentation layers depend only on this interface.
 *
 * Kept paging-agnostic on purpose: `:core:common` is pure Kotlin and must not
 * depend on `androidx.paging`. The Android `PagingSource` is exposed by the
 * data/feature layer on top of this contract.
 */
interface ChannelRepository {
    /**
     * Observes channels, optionally filtered by a search [query] (prefix match).
     * Emits a new list whenever the underlying store changes.
     */
    fun observeChannels(query: String? = null): Flow<List<Channel>>

    /** Looks up a single channel by id, or `null` if unknown. */
    suspend fun channelById(id: ChannelId): Channel?

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
