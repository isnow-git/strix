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
     * A paged stream of channels. A non-blank [query] runs an FTS prefix search
     * across all channels; otherwise a non-null [category] filters by group. The
     * UI only ever holds a window of rows.
     */
    fun pagedChannels(
        query: String? = null,
        category: String? = null,
    ): Flow<PagingData<Channel>>

    /** Distinct category names for the filter rail. */
    fun categories(): Flow<List<String>>
}
