package dev.strix.core.data

import androidx.paging.PagingData
import dev.strix.core.model.Channel
import kotlinx.coroutines.flow.Flow

/**
 * Android-facing paging API for channels. Separate from the pure-domain
 * `ChannelRepository` so that the domain stays free of `androidx.paging` while the UI
 * still gets memory-bounded paging straight from Room.
 */
interface ChannelPagingRepository {
    /**
     * A paged stream of channels. A non-blank [query] runs an FTS prefix search across
     * all channels; otherwise a non-null [category] filters by group.
     *
     * Placeholders are enabled so the list reports its full size: the UI can jump
     * (scrollToItem) to any row — e.g. a keypad-zapped channel far down — and only the
     * window around it is actually loaded.
     */
    fun pagedChannels(
        query: String? = null,
        category: String? = null,
    ): Flow<PagingData<Channel>>

    /** Distinct category names for the filter rail. */
    fun categories(): Flow<List<String>>
}
