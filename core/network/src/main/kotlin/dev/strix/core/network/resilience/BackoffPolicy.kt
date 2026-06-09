package dev.strix.core.network.resilience

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Exponential backoff with "equal jitter" for retrying flaky stream/playlist
 * requests. Jitter spreads concurrent retries so a flapping provider isn't hit by a
 * synchronised thundering herd.
 *
 * For attempt `n` the base delay is `baseDelayMs * factor^n`, capped at [maxDelayMs];
 * the returned value is `cap/2 + random(0, cap/2)`.
 */
data class BackoffPolicy(
    val baseDelayMs: Long = 500,
    val maxDelayMs: Long = 30_000,
    val factor: Double = 2.0,
) {
    /** Delay before [attempt] (1-based). Always within `[0, maxDelayMs]`. */
    fun delayFor(
        attempt: Int,
        random: Random = Random.Default,
    ): Long {
        require(attempt >= 1) { "attempt must be >= 1, was $attempt" }
        val exponential = baseDelayMs.toDouble() * factor.pow(attempt - 1)
        val cap = min(exponential, maxDelayMs.toDouble())
        val half = cap / 2.0
        return (half + random.nextDouble() * half).toLong().coerceIn(0, maxDelayMs)
    }
}
