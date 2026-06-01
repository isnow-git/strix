package dev.strix.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.strix.core.common.dispatcher.DefaultDispatcherProvider
import dev.strix.core.common.dispatcher.DispatcherProvider
import dev.strix.core.common.epg.EpgRepository
import dev.strix.core.common.repository.ChannelRepository
import dev.strix.core.data.ChannelPagingRepository
import dev.strix.core.data.ChannelRepositoryImpl
import dev.strix.core.data.EpgRepositoryImpl
import javax.inject.Singleton

/** Binds the data-layer repository implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindChannelPagingRepository(impl: ChannelRepositoryImpl): ChannelPagingRepository

    @Binds
    @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository
}

/** Provides the production dispatchers (no Hilt in pure-Kotlin :core:common). */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}
