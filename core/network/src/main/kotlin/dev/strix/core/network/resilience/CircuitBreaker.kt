package dev.strix.core.network.resilience

/**
 * A minimal circuit breaker for dead/flapping streams. After [failureThreshold]
 * consecutive failures the circuit OPENs and rejects attempts for [openDurationMs];
 * it then allows a single HALF_OPEN probe — success closes it, another failure
 * re-opens it.
 *
 * Keeps the player from hammering a dead channel (battery, network, jank on TV).
 * [now] is injectable for deterministic tests.
 */
class CircuitBreaker(
    private val failureThreshold: Int = 3,
    private val openDurationMs: Long = 60_000,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    enum class State { CLOSED, OPEN, HALF_OPEN }

    private var state = State.CLOSED
    private var consecutiveFailures = 0
    private var openedAt = 0L

    /** Current state, after applying any pending OPEN -> HALF_OPEN timeout. */
    @Synchronized
    fun state(): State {
        refresh()
        return state
    }

    /** True if a call may proceed (circuit CLOSED or probing in HALF_OPEN). */
    @Synchronized
    fun canAttempt(): Boolean {
        refresh()
        return state != State.OPEN
    }

    /** Records a successful call: resets failures and closes the circuit. */
    @Synchronized
    fun recordSuccess() {
        consecutiveFailures = 0
        state = State.CLOSED
    }

    /** Records a failed call: trips OPEN at the threshold, or re-opens a probe. */
    @Synchronized
    fun recordFailure() {
        refresh()
        consecutiveFailures++
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = State.OPEN
            openedAt = now()
        }
    }

    private fun refresh() {
        if (state == State.OPEN && now() - openedAt >= openDurationMs) {
            state = State.HALF_OPEN
        }
    }
}
