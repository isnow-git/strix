package dev.strix.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.strix.core.database.entity.ChannelEntity
import dev.strix.core.database.entity.ChannelFtsEntity
import kotlinx.coroutines.flow.Flow

/** One keyset page of the catalogue: the window's rows plus the overall total. */
data class ChannelPage(
    val total: Int,
    val rows: List<ChannelEntity>,
)

/**
 * Channel data access.
 *
 * Performance notes:
 * - Lists are exposed as [PagingSource]s (never `List`) so the UI only ever holds a
 *   window of rows in memory.
 * - Writes go through batched, transactional methods so a streamed playlist is
 *   flushed in chunks rather than held whole in RAM.
 */
@Dao
interface ChannelDao {
    /** Total channels in the fixed catalogue (those with a `channelNumber`). */
    @Query("SELECT COUNT(*) FROM channels WHERE channelNumber > 0")
    suspend fun numberedCount(): Int

    /**
     * One window of the catalogue by keyset: the [limit] channels whose `channelNumber`
     * is `>= startNumber`, in order. `channelNumber` is dense (1..N) and indexed, so
     * this is an index seek + page read — O(log n + limit) at any position, unlike
     * OFFSET which re-reads everything before the window.
     */
    @Query("SELECT * FROM channels WHERE channelNumber >= :startNumber ORDER BY channelNumber ASC LIMIT :limit")
    suspend fun numberedWindow(
        startNumber: Int,
        limit: Int,
    ): List<ChannelEntity>

    /** Count + one keyset window read atomically, for a consistent paged snapshot. */
    @Transaction
    suspend fun numberedPage(
        startNumber: Int,
        limit: Int,
    ): ChannelPage {
        val total = numberedCount()
        val start = startNumber.coerceIn(1, maxOf(1, total))
        return ChannelPage(total = total, rows = numberedWindow(start, limit))
    }

    /** Representative rows of a single canonical category, in playlist order. */
    @Query("SELECT * FROM channels WHERE isPrimary = 1 AND category = :category ORDER BY sortIndex ASC")
    fun pagingSourceByCategory(category: String): PagingSource<Int, ChannelEntity>

    /** All quality variants of the channel [channelId] belongs to, best first. */
    @Query(
        "SELECT * FROM channels WHERE baseKey = " +
            "(SELECT baseKey FROM channels WHERE channelId = :channelId) " +
            "ORDER BY qualityRank ASC, sortIndex ASC",
    )
    suspend fun variantsOf(channelId: String): List<ChannelEntity>

    /**
     * Distinct categories for the rail — excludes Adulte (hidden) and Général (the
     * "Toutes" view already shows generalist channels first).
     */
    @Query("SELECT DISTINCT category FROM channels WHERE isPrimary = 1 AND category NOT IN ('Adulte', 'Général')")
    fun observeCategories(): Flow<List<String>>

    /**
     * Prefix search via the FTS index. [match] must be an FTS MATCH expression built
     * by `FtsQuery.prefixMatch`.
     */
    @Query(
        """
        SELECT c.* FROM channels AS c
        JOIN channels_fts AS f ON f.channelId = c.channelId
        WHERE channels_fts MATCH :match AND c.isPrimary = 1 AND c.category != 'Adulte'
        ORDER BY c.sortIndex ASC
        """,
    )
    fun searchPagingSource(match: String): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM channels ORDER BY sortIndex ASC")
    fun observeAll(): Flow<List<ChannelEntity>>

    /** Observes the FTS prefix-search results. [match] is an `FtsQuery.prefixMatch` expression. */
    @Query(
        """
        SELECT c.* FROM channels AS c
        JOIN channels_fts AS f ON f.channelId = c.channelId
        WHERE channels_fts MATCH :match
        ORDER BY c.sortIndex ASC
        """,
    )
    fun searchObserve(match: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE channelId = :channelId LIMIT 1")
    suspend fun findByChannelId(channelId: String): ChannelEntity?

    /** The channel with a given fixed keypad number (see [channelNumbersInOrder]). */
    @Query("SELECT * FROM channels WHERE isPrimary = 1 AND channelNumber = :number LIMIT 1")
    suspend fun findByNumber(number: Int): ChannelEntity?

    /**
     * 0-based position of [channelId] within its category's list (the same order
     * [pagingSourceByCategory] uses), to anchor the paged list when zapping to a
     * channel that is in the current category.
     */
    @Query(
        "SELECT COUNT(*) FROM channels WHERE isPrimary = 1 AND category = :category " +
            "AND sortIndex < (SELECT sortIndex FROM channels WHERE channelId = :channelId)",
    )
    suspend fun positionInCategory(
        category: String,
        channelId: String,
    ): Int

    /** A live sibling of the same logical channel that has a provider EPG id. */
    @Query(
        "SELECT * FROM channels WHERE epgBaseKey = :epgBaseKey " +
            "AND epgChannelId IS NOT NULL AND timeshift = 0 ORDER BY qualityRank ASC LIMIT 1",
    )
    suspend fun epgSibling(epgBaseKey: String): ChannelEntity?

    /** Distinct provider EPG ids present, to filter an external XMLTV guide. */
    @Query("SELECT DISTINCT epgChannelId FROM channels WHERE epgChannelId IS NOT NULL AND epgChannelId != ''")
    suspend fun distinctEpgChannelIds(): List<String>

    /** The next channel (representative) in playlist order, for D-pad zapping. */
    @Query(
        "SELECT * FROM channels WHERE isPrimary = 1 AND sortIndex > " +
            "(SELECT sortIndex FROM channels WHERE channelId = :channelId) " +
            "ORDER BY sortIndex ASC LIMIT 1",
    )
    suspend fun findNext(channelId: String): ChannelEntity?

    /** The previous channel (representative) in playlist order. */
    @Query(
        "SELECT * FROM channels WHERE isPrimary = 1 AND sortIndex < " +
            "(SELECT sortIndex FROM channels WHERE channelId = :channelId) " +
            "ORDER BY sortIndex DESC LIMIT 1",
    )
    suspend fun findPrevious(channelId: String): ChannelEntity?

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(rows: List<ChannelFtsEntity>)

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Query("DELETE FROM channels_fts")
    suspend fun clearFts()

    @Query("UPDATE channels SET isPrimary = 0")
    suspend fun clearPrimaryFlags()

    /**
     * Marks one representative per [ChannelEntity.baseKey] as primary: the
     * earliest-listed variant (smallest `sortIndex`), so the grouped channel keeps its
     * original list position, category and name. Default playback still uses the best
     * quality (see `variantsOf`, ordered by rank).
     */
    @Query(
        "UPDATE channels SET isPrimary = 1 WHERE rowid IN (" +
            "SELECT rowid FROM channels AS c WHERE c.sortIndex = " +
            "(SELECT MIN(sortIndex) FROM channels WHERE baseKey = c.baseKey) " +
            "GROUP BY c.baseKey)",
    )
    suspend fun markPrimaries()

    /**
     * Primary, non-adult channels in the canonical browse order (Général first, then
     * base playlist order) — the order their fixed [ChannelEntity.channelNumber]
     * follows, so position N in this list becomes channel number N.
     */
    @Query(
        "SELECT channelId FROM channels WHERE isPrimary = 1 AND category != 'Adulte' " +
            "ORDER BY (category = 'Général') DESC, sortIndex ASC",
    )
    suspend fun channelNumbersInOrder(): List<String>

    @Query("UPDATE channels SET channelNumber = :number WHERE channelId = :channelId")
    suspend fun setChannelNumber(
        channelId: String,
        number: Int,
    )

    /**
     * Assigns each representative channel its fixed catalogue number (1-based) in the
     * canonical order. A single ordered read plus a reused update statement — O(n), so
     * it scales to large catalogues unlike a correlated-rank UPDATE.
     */
    @Transaction
    suspend fun assignChannelNumbers() {
        var number = 1
        for (channelId in channelNumbersInOrder()) {
            setChannelNumber(channelId, number)
            number++
        }
    }

    /** Recomputes grouping + fixed numbering for every channel. Call once after an import. */
    @Transaction
    suspend fun finalizeGroups() {
        clearPrimaryFlags()
        markPrimaries()
        assignChannelNumbers()
    }

    /**
     * Appends one streamed batch of channels + their FTS rows in a single transaction.
     * Call repeatedly while parsing a playlist.
     */
    @Transaction
    suspend fun insertBatch(
        channels: List<ChannelEntity>,
        fts: List<ChannelFtsEntity>,
    ) {
        insertChannels(channels)
        insertFts(fts)
    }

    /** Atomically replaces the whole catalogue (used at the end of a full refresh). */
    @Transaction
    suspend fun replaceAll(
        channels: List<ChannelEntity>,
        fts: List<ChannelFtsEntity>,
    ) {
        clearChannels()
        clearFts()
        insertChannels(channels)
        insertFts(fts)
    }
}
