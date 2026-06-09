package dev.strix.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Provides a single shared [OkHttpClient].
 *
 * Performance notes:
 * - One client app-wide so the connection pool, thread pool, and TLS session cache
 *   are reused across every request.
 * - A small, time-bounded [ConnectionPool] keeps idle sockets low on TV RAM while
 *   still amortising TLS handshakes during zapping.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val MAX_IDLE_CONNECTIONS = 5
    private const val KEEP_ALIVE_MINUTES = 5L
    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 20L
    private const val WRITE_TIMEOUT_SECONDS = 15L

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectionPool(ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_MINUTES, TimeUnit.MINUTES))
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // No overall call timeout: a large playlist or guide download can take
            // longer than a single API call without being a stall (the read timeout
            // still guards a truly stuck transfer).
            .retryOnConnectionFailure(true)
            .build()
}
