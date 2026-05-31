package dev.strix.core.network.resilience

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class BackoffPolicyTest {
    private val policy = BackoffPolicy(baseDelayMs = 500, maxDelayMs = 30_000, factor = 2.0)

    @Test
    fun `delay stays within equal-jitter bounds of the exponential cap`() {
        // attempt 1 cap = 500, attempt 2 = 1000, attempt 3 = 2000 ...
        val caps = mapOf(1 to 500L, 2 to 1000L, 3 to 2000L, 4 to 4000L)
        val random = Random(42)
        caps.forEach { (attempt, cap) ->
            repeat(50) {
                val delay = policy.delayFor(attempt, random)
                assertThat(delay).isAtLeast(cap / 2)
                assertThat(delay).isAtMost(cap)
            }
        }
    }

    @Test
    fun `delay is capped at maxDelayMs for large attempts`() {
        val delay = policy.delayFor(attempt = 20, random = Random(1))
        assertThat(delay).isAtMost(30_000)
        assertThat(delay).isAtLeast(15_000) // cap/2 of the max
    }

    @Test
    fun `attempt below one is rejected`() {
        runCatching { policy.delayFor(0) }
            .also { assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java) }
    }
}
