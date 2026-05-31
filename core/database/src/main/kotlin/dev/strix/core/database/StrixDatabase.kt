package dev.strix.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.strix.core.database.dao.ChannelDao
import dev.strix.core.database.entity.ChannelEntity
import dev.strix.core.database.entity.ChannelFtsEntity

/**
 * Room database root. Schemas are exported (see `room.schemaLocation` in the
 * module build) so migrations can be written and tested from version 1 onward.
 */
@Database(
    entities = [
        ChannelEntity::class,
        ChannelFtsEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class StrixDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao

    companion object {
        const val NAME = "strix.db"
    }
}
