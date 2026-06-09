package dev.strix.core.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Deterministic playback over a single [ExoPlayer].
 *
 * This exists to kill the "works one time in two" class of bug. The old screen tracked
 * playback with a scatter of `mutableStateOf` booleans (`showVideo`, `playbackError`,
 * …) flipped from `LaunchedEffect`s whose keys it mutated mid-flight, so a stale
 * callback or a self-cancelled effect could leave the UI in a wrong state. Here:
 *
 * - There is **one** explicit [Picture] state, never a soup of booleans.
 * - Every [play]/[stop] bumps a monotonic [generation]; a scheduled "unavailable"
 *   timeout only fires if its captured generation is still current, so a timer armed
 *   for a channel the user has already left can never flip the picture.
 * - There is exactly one media item at a time, so the [Player.Listener] callbacks are
 *   unambiguously about the current stream.
 *
 * Threading: confine calls to the main thread (as ExoPlayer requires). [scope] should
 * be a main-dispatched scope owned by the caller; [release] tears everything down.
 */
@UnstableApi
class StreamPlaybackController(
    val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val unavailableTimeoutMs: Long = DEFAULT_UNAVAILABLE_TIMEOUT_MS,
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
) {
    /** The single, explicit picture state the UI renders. */
    enum class Picture { Connecting, Playing, Unavailable }

    private val mutablePicture = MutableStateFlow(Picture.Connecting)
    val picture: StateFlow<Picture> = mutablePicture.asStateFlow()

    private val mutableIsPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = mutableIsPlaying.asStateFlow()

    // Monotonic request id. Bumped on every play()/stop(); a scheduled timeout captures
    // its value and only acts while it is still current.
    private var generation = 0L
    private var retries = 0
    private var currentUrl: String? = null
    private var timeoutJob: Job? = null

    private val listener =
        object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    retries = 0
                    mutablePicture.value = Picture.Playing
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                mutableIsPlaying.value = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                // Many IPTV feeds drop the first connection; retry a couple of times
                // before declaring the channel unavailable.
                if (retries < maxRetries) {
                    retries++
                    player.prepare()
                } else {
                    mutablePicture.value = Picture.Unavailable
                }
            }
        }

    init {
        player.addListener(listener)
    }

    /**
     * Plays [url]. Idempotent: re-issuing the current url is a no-op so recomposition or
     * a repeated focus event never restarts a healthy stream.
     */
    fun play(url: String) {
        if (url == currentUrl) return
        startRequest(url)
    }

    /** Re-attempts the current url (e.g. user retries an unavailable channel). */
    fun retry() {
        currentUrl?.let(::startRequest)
    }

    private fun startRequest(url: String) {
        val requestGeneration = ++generation
        currentUrl = url
        retries = 0
        mutablePicture.value = Picture.Connecting
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true

        timeoutJob?.cancel()
        timeoutJob =
            scope.launch {
                delay(unavailableTimeoutMs)
                // Generation-guarded: only declare unavailable if this is still the
                // current request and no picture has arrived.
                if (requestGeneration == generation && mutablePicture.value == Picture.Connecting) {
                    mutablePicture.value = Picture.Unavailable
                }
            }
    }

    /** Toggles play/pause on the current stream. */
    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    /** Pauses (e.g. on lifecycle stop) without forgetting the current stream. */
    fun pause() {
        player.pause()
    }

    /** Resumes a paused stream if one is loaded. */
    fun resume() {
        if (player.mediaItemCount > 0) player.play()
    }

    /** Stops playback and forgets the current stream; invalidates any pending timeout. */
    fun stop() {
        generation++
        timeoutJob?.cancel()
        timeoutJob = null
        currentUrl = null
        player.stop()
        mutablePicture.value = Picture.Connecting
    }

    /** Releases the player and detaches the listener. The controller is unusable after. */
    fun release() {
        generation++
        timeoutJob?.cancel()
        timeoutJob = null
        player.removeListener(listener)
        player.release()
    }

    private companion object {
        // Wait this long for a first frame before declaring a channel unavailable rather
        // than spinning forever.
        const val DEFAULT_UNAVAILABLE_TIMEOUT_MS = 12_000L
        const val DEFAULT_MAX_RETRIES = 2
    }
}
