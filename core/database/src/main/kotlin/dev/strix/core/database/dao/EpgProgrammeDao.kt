package dev.strix.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.strix.core.database.entity.EpgProgrammeEntity

/** Access to the ingested XMLTV programmes. */
@Dao
interface EpgProgrammeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBatch(rows: List<EpgProgrammeEntity>)

    @Query("DELETE FROM epg_programmes")
    suspend fun clear()

    @Query("DELETE FROM epg_programmes WHERE stopSec < :beforeSec")
    suspend fun pruneOlderThan(beforeSec: Long)

    /** Programmes still running or upcoming at [refSec], earliest first. */
    @Query(
        "SELECT * FROM epg_programmes WHERE normChannelId = :normChannelId " +
            "AND stopSec > :refSec ORDER BY startSec ASC LIMIT :limit",
    )
    suspend fun around(
        normChannelId: String,
        refSec: Long,
        limit: Int,
    ): List<EpgProgrammeEntity>

    @Query("SELECT COUNT(*) FROM epg_programmes")
    suspend fun count(): Int
}
