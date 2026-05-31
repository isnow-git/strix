package dev.strix.core.player.config

/**
 * Buffering parameters for the custom `LoadControl`, tuned for low-RAM TVs.
 *
 * Defaults are deliberately conservative: a modest forward buffer keeps memory
 * pressure (and therefore GC pauses / jank) low, and the back buffer is disabled
 * so already-played media is freed immediately. Time is prioritised over size so
 * playback starts promptly even on small streams.
 *
 * Invariants mirror Media3's `DefaultLoadControl` requirements and are checked up
 * front so a bad config fails fast rather than deep inside the player.
 */
data class TvBufferConfig(
    val minBufferMs: Int = 15_000,
    val maxBufferMs: Int = 30_000,
    val bufferForPlaybackMs: Int = 2_500,
    val bufferForPlaybackAfterRebufferMs: Int = 5_000,
    val backBufferMs: Int = 0,
) {
    init {
        require(minBufferMs >= 0) { "minBufferMs must be >= 0" }
        require(bufferForPlaybackMs >= 0) { "bufferForPlaybackMs must be >= 0" }
        require(backBufferMs >= 0) { "backBufferMs must be >= 0" }
        require(minBufferMs <= maxBufferMs) {
            "minBufferMs ($minBufferMs) must be <= maxBufferMs ($maxBufferMs)"
        }
        require(bufferForPlaybackMs <= minBufferMs) {
            "bufferForPlaybackMs ($bufferForPlaybackMs) must be <= minBufferMs ($minBufferMs)"
        }
        require(bufferForPlaybackAfterRebufferMs <= minBufferMs) {
            "bufferForPlaybackAfterRebufferMs ($bufferForPlaybackAfterRebufferMs) must be <= minBufferMs ($minBufferMs)"
        }
    }
}
