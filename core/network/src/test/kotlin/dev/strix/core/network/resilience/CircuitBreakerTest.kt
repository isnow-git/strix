package dev.strix.core.network.resilience

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CircuitBreakerTest {
    private var clock = 0L

    private fun breaker(
        threshold: Int = 3,
        openMs: Long = 1000,
    ) = CircuitBreaker(failureThreshold = threshold, openDurationMs = openMs, now = { clock })

    @Test
    fun `starts closed and allows attempts`() {
        val cb = breaker()
        assertThat(cb.state()).isEqualTo(CircuitBreaker.State.CLOSED)
        assertThat(cb.canAttempt()).isTrue()
    }

    @Test
    fun `opens after consecutive failures reach the threshold`() {
        val cb = breaker(threshold = 3)
        repeat(3) { cb.recordFailure() }
        assertThat(cb.state()).isEqualTo(CircuitBreaker.State.OPEN)
        assertThat(cb.canAttempt()).isFalse()
    }

    @Test
    fun `success resets the failure count before the threshold`() {
        val cb = breaker(threshold = 3)
        cb.recordFailure()
        cb.recordFailure()
        cb.recordSuccess()
        cb.recordFailure()
        assertThat(cb.canAttempt()).isTrue()
    }

    @Test
    fun `moves to half-open after the open duration elapses`() {
        val cb = breaker(threshold = 2, openMs = 1000)
        repeat(2) { cb.recordFailure() }
        assertThat(cb.canAttempt()).isFalse()

        clock += 1000
        assertThat(cb.canAttempt()).isTrue()
        assertThat(cb.state()).isEqualTo(CircuitBreaker.State.HALF_OPEN)
    }

    @Test
    fun `half-open success closes the circuit`() {
        val cb = breaker(threshold = 2, openMs = 1000)
        repeat(2) { cb.recordFailure() }
        clock += 1000
        cb.canAttempt() // transition to HALF_OPEN
        cb.recordSuccess()
        assertThat(cb.state()).isEqualTo(CircuitBreaker.State.CLOSED)
    }

    @Test
    fun `half-open failure re-opens immediately`() {
        val cb = breaker(threshold = 2, openMs = 1000)
        repeat(2) { cb.recordFailure() }
        clock += 1000
        cb.canAttempt() // HALF_OPEN
        cb.recordFailure()
        assertThat(cb.state()).isEqualTo(CircuitBreaker.State.OPEN)
    }
}
