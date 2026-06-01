package dev.strix.core.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import dev.strix.core.database.dao.ChannelDao
import dev.strix.core.database.entity.ChannelEntity

/**
 * Keyset [PagingSource] for the full "Toutes" catalogue.
 *
 * Pages by the dense, indexed `channelNumber` (1..N): every window is
 * `WHERE channelNumber >= key LIMIT pageSize`, an index seek + page read, so a load
 * costs O(log n + pageSize) **at any position**. Room's generated source pages by
 * `OFFSET`, which re-reads every row before the window — fine near the top but
 * janky deep in a ~10k-row list. Placeholders ([LoadResult.Page.itemsBefore] /
 * [LoadResult.Page.itemsAfter]) let the UI jump straight to any row.
 *
 * Invalidated like Room's own sources: it observes the `channels` table and
 * reloads when an import rewrites it.
 */
internal class ChannelCatalogPagingSource(
    private val dao: ChannelDao,
    db: RoomDatabase,
) : PagingSource<Int, ChannelEntity>() {
    private val observer =
        object : InvalidationTracker.Observer("channels") {
            override fun onInvalidated(tables: Set<String>) = invalidate()
        }

    init {
        db.invalidationTracker.addObserver(observer)
        registerInvalidatedCallback { db.invalidationTracker.removeObserver(observer) }
    }

    // The key is a channelNumber (1-based); a far jump re-loads one window there.
    override val jumpingSupported: Boolean = true

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChannelEntity> {
        val start = (params.key ?: 1).coerceAtLeast(1)
        val page = dao.numberedPage(startNumber = start, limit = params.loadSize)
        val rows = page.rows
        if (rows.isEmpty()) {
            return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
        }
        // channelNumber is dense, so the window's rows are a contiguous run.
        val firstNumber = rows.first().channelNumber
        val lastNumber = rows.last().channelNumber
        return LoadResult.Page(
            data = rows,
            prevKey = if (firstNumber <= 1) null else (firstNumber - params.loadSize).coerceAtLeast(1),
            nextKey = if (lastNumber >= page.total) null else lastNumber + 1,
            itemsBefore = firstNumber - 1,
            itemsAfter = (page.total - lastNumber).coerceAtLeast(0),
        )
    }

    override fun getRefreshKey(state: PagingState<Int, ChannelEntity>): Int? {
        // anchorPosition is a 0-based list position; channelNumber = position + 1.
        val anchor = state.anchorPosition ?: return null
        return (anchor + 1 - state.config.initialLoadSize / 2).coerceAtLeast(1)
    }
}
