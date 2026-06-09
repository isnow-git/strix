package dev.strix.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.strix.core.common.dispatcher.DefaultDispatcherProvider
import dev.strix.core.common.dispatcher.DispatcherProvider
import dev.strix.core.data.ChannelPagingRepository
import dev.strix.core.data.ChannelRepositoryImpl
import dev.strix.core.data.EpgRepositoryImpl
import dev.strix.core.data.credentials.SecureCredentialStore
import dev.strix.core.domain.epg.EpgRepository
import dev.strix.core.domain.onboarding.CredentialReceiver
import dev.strix.core.domain.onboarding.CredentialStore
import dev.strix.core.domain.repository.ChannelRepository
import javax.inject.Singleton

/** Binds the data-layer repository implementations to their domain contracts. */
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

    @Binds
    @Singleton
    abstract fun bindCredentialStore(impl: SecureCredentialStore): CredentialStore

    @Binds
    @Singleton
    abstract fun bindCredentialReceiver(impl: SecureCredentialStore): CredentialReceiver
}

/** Provides the production dispatchers (no Hilt in the pure-Kotlin :core:common). */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}
