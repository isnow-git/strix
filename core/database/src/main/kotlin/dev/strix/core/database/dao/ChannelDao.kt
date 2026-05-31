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

/**
 * Channel data access.
 *
 * Performance notes:
 * - Lists are exposed as [PagingSource]s (never `List`) so the UI only ever holds
 *   a window of rows in memory.
 * - Writes go through batched, transactional methods so a streamed playlist is
 *   flushed in chunks rather than held whole in RAM.
 */
@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY sortIndex ASC")
    fun pagingSource(): PagingSource<Int, ChannelEntity>

    /**
     * Prefix search via the FTS index (ADR-0007). [match] must be an FTS MATCH
     * expression built by `FtsQuery.prefixMatch`.
     */
    @Query(
        """
        SELECT c.* FROM channels AS c
        JOIN channels_fts AS f ON f.channelId = c.channelId
        WHERE channels_fts MATCH :match
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

    /** The next channel in playlist order, for D-pad zapping. Null at the end. */
    @Query(
        "SELECT * FROM channels WHERE sortIndex > " +
            "(SELECT sortIndex FROM channels WHERE channelId = :channelId) " +
            "ORDER BY sortIndex ASC LIMIT 1",
    )
    suspend fun findNext(channelId: String): ChannelEntity?

    /** The previous channel in playlist order. Null at the start. */
    @Query(
        "SELECT * FROM channels WHERE sortIndex < " +
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

    /**
     * Appends one streamed batch of channels + their FTS rows in a single
     * transaction. Call repeatedly while parsing a playlist.
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
