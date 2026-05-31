package dev.strix.core.data

import androidx.paging.PagingData
import dev.strix.core.common.model.Channel
import kotlinx.coroutines.flow.Flow

/**
 * Android-facing paging API for channels. Separate from the pure-domain
 * `ChannelRepository` so that `:core:common` stays free of `androidx.paging`
 * while the UI still gets memory-bounded paging straight from Room.
 */
interface ChannelPagingRepository {
    /**
     * A paged stream of channels, optionally filtered by a search [query]
     * (prefix match via FTS). The UI only ever holds a window of rows.
     */
    fun pagedChannels(query: String? = null): Flow<PagingData<Channel>>
}
