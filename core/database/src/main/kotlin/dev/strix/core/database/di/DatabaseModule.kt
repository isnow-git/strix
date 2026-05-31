package dev.strix.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.strix.core.database.StrixDatabase
import dev.strix.core.database.dao.ChannelDao
import javax.inject.Singleton

/** Provides the Room database and DAOs as application-scoped singletons. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): StrixDatabase =
        Room
            .databaseBuilder(context, StrixDatabase::class.java, StrixDatabase.NAME)
            .build()

    @Provides
    fun provideChannelDao(database: StrixDatabase): ChannelDao = database.channelDao()
}
