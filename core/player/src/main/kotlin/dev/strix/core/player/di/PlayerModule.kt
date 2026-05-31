package dev.strix.core.player.di

import androidx.media3.common.util.UnstableApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.strix.core.player.DefaultStrixPlayerFactory
import dev.strix.core.player.StrixPlayerFactory
import dev.strix.core.player.config.AbrConfig
import dev.strix.core.player.config.TvBufferConfig
import javax.inject.Singleton

/** Provides the TV-tuned player configuration. */
@Module
@InstallIn(SingletonComponent::class)
object PlayerConfigModule {
    @Provides
    fun provideTvBufferConfig(): TvBufferConfig = TvBufferConfig()

    @Provides
    fun provideAbrConfig(): AbrConfig = AbrConfig()
}

/** Binds the player factory implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {
    @Binds
    @Singleton
    @OptIn(UnstableApi::class)
    abstract fun bindStrixPlayerFactory(impl: DefaultStrixPlayerFactory): StrixPlayerFactory
}
