package dev.strix.core.network.resilience

import kotlinx.coroutines.delay

/**
 * Runs [block] up to [maxAttempts] times, backing off via [policy] between
 * tries. Only retries when [shouldRetry] accepts the thrown error; otherwise the
 * error propagates immediately.
 *
 * [delayFn] is injectable so tests can assert the backoff schedule without
 * real-time waits.
 */
@Suppress("TooGenericExceptionCaught") // Retrying is generic by design; callers narrow via shouldRetry.
suspend fun <T> retryWithBackoff(
    maxAttempts: Int,
    policy: BackoffPolicy = BackoffPolicy(),
    shouldRetry: (Throwable) -> Boolean = { true },
    delayFn: suspend (Long) -> Unit = { delay(it) },
    block: suspend (attempt: Int) -> T,
): T {
    require(maxAttempts >= 1) { "maxAttempts must be >= 1, was $maxAttempts" }
    var attempt = 1
    while (true) {
        try {
            return block(attempt)
        } catch (t: Throwable) {
            if (attempt >= maxAttempts || !shouldRetry(t)) throw t
            delayFn(policy.delayFor(attempt))
            attempt++
        }
    }
}
