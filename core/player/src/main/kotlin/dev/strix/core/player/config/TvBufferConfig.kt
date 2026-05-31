package dev.strix.core.player.config

/**
 * Buffering parameters for the custom `LoadControl`, tuned for low-RAM TVs.
 *
 * Tuned for live IPTV jitter without stutter. Playback starts near-live (a small
 * [bufferForPlaybackMs]); but after any rebuffer it rebuilds a large cushion
 * ([bufferForPlaybackAfterRebufferMs]) before resuming, so the stream falls a few
 * seconds behind live once and then absorbs further jitter smoothly instead of
 * stalling again. A live feed is paced at ~1x so the buffer can't actually grow
 * beyond the cushion, keeping memory bounded despite the high [maxBufferMs].
 *
 * Invariants mirror Media3's `DefaultLoadControl` requirements and are checked up
 * front so a bad config fails fast rather than deep inside the player.
 */
data class TvBufferConfig(
    val minBufferMs: Int = 30_000,
    val maxBufferMs: Int = 60_000,
    val bufferForPlaybackMs: Int = 2_500,
    val bufferForPlaybackAfterRebufferMs: Int = 12_000,
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
