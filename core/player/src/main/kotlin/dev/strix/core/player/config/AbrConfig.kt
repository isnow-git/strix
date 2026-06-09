package dev.strix.core.player.config

/**
 * Adaptive-bitrate (ABR) parameters for `AdaptiveTrackSelection`, calibrated for TV
 * networks.
 *
 * - [bandwidthFraction] < 1 leaves headroom so we don't over-commit to a bitrate the
 *   link can't sustain (fewer rebuffers, less jank).
 * - Quality decreases are allowed sooner than increases (asymmetric) to react quickly
 *   when bandwidth drops.
 *
 * Invariants mirror Media3's `AdaptiveTrackSelection` requirements.
 */
data class AbrConfig(
    val minDurationForQualityIncreaseMs: Int = 10_000,
    val maxDurationForQualityDecreaseMs: Int = 25_000,
    val minDurationToRetainAfterDiscardMs: Int = 25_000,
    val bandwidthFraction: Float = 0.7f,
) {
    init {
        require(minDurationForQualityIncreaseMs >= 0) { "minDurationForQualityIncreaseMs must be >= 0" }
        require(maxDurationForQualityDecreaseMs >= 0) { "maxDurationForQualityDecreaseMs must be >= 0" }
        require(minDurationToRetainAfterDiscardMs >= maxDurationForQualityDecreaseMs) {
            "minDurationToRetainAfterDiscardMs ($minDurationToRetainAfterDiscardMs) must be >= " +
                "maxDurationForQualityDecreaseMs ($maxDurationForQualityDecreaseMs)"
        }
        require(bandwidthFraction > 0f && bandwidthFraction <= 1f) {
            "bandwidthFraction must be in (0, 1], was $bandwidthFraction"
        }
    }
}
