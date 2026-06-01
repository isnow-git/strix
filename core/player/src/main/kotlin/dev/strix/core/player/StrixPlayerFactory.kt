package dev.strix.core.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.strix.core.player.config.AbrConfig
import dev.strix.core.player.config.TvBufferConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Creates fully-configured [ExoPlayer] instances for Strix.
 *
 * Each call returns a new player wired with the TV-tuned [TvBufferConfig]
 * `LoadControl`, the calibrated [AbrConfig] adaptive track selection, and an
 * OkHttp-backed data source so media requests reuse the shared connection pool
 * (ADR-0004).
 *
 * Ownership: the caller owns the returned player and **must** call
 * `ExoPlayer.release()` when its host leaves the screen (bind to the lifecycle in
 * the feature layer, e.g. a Compose `DisposableEffect` releasing on dispose).
 * This is what prevents leaks and decoder exhaustion while zapping.
 */
interface StrixPlayerFactory {
    fun create(): ExoPlayer
}

@UnstableApi
class DefaultStrixPlayerFactory
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val okHttpClient: OkHttpClient,
        private val bufferConfig: TvBufferConfig,
        private val abrConfig: AbrConfig,
    ) : StrixPlayerFactory {
        // A live stream is one open-ended HTTP response, so it needs a longer read
        // timeout than the shared client's API-sized one (the base client has no
        // overall call timeout, which would otherwise force-close the feed). The
        // connection pool/dispatcher are reused; built once and shared by every
        // player rather than rebuilt on each create().
        private val dataSourceFactory by lazy {
            val streamingClient =
                okHttpClient
                    .newBuilder()
                    .readTimeout(STREAM_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build()
            OkHttpDataSource.Factory(streamingClient)
        }

        override fun create(): ExoPlayer {
            val loadControl =
                DefaultLoadControl
                    .Builder()
                    .setBufferDurationsMs(
                        bufferConfig.minBufferMs,
                        bufferConfig.maxBufferMs,
                        bufferConfig.bufferForPlaybackMs,
                        bufferConfig.bufferForPlaybackAfterRebufferMs,
                    ).setPrioritizeTimeOverSizeThresholds(true)
                    // second arg retainBackBufferFromKeyframe = true
                    .setBackBuffer(bufferConfig.backBufferMs, true)
                    .build()

            val trackSelector =
                DefaultTrackSelector(
                    context,
                    AdaptiveTrackSelection.Factory(
                        abrConfig.minDurationForQualityIncreaseMs,
                        abrConfig.maxDurationForQualityDecreaseMs,
                        abrConfig.minDurationToRetainAfterDiscardMs,
                        abrConfig.bandwidthFraction,
                    ),
                )

            return ExoPlayer
                .Builder(context)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build()
        }

        private companion object {
            // A stalled feed still fails on read even with no overall call timeout.
            const val STREAM_READ_TIMEOUT_SECONDS = 30L
        }
    }
