package dev.strix.core.network.resilience

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class RetryTest {
    @Test
    fun `succeeds on the first attempt without delaying`() =
        runTest {
            val delays = mutableListOf<Long>()
            val result = retryWithBackoff(maxAttempts = 3, delayFn = { delays += it }) { "ok" }
            assertThat(result).isEqualTo("ok")
            assertThat(delays).isEmpty()
        }

    @Test
    fun `retries until success and backs off between attempts`() =
        runTest {
            val delays = mutableListOf<Long>()
            var calls = 0
            val result =
                retryWithBackoff(
                    maxAttempts = 5,
                    policy = BackoffPolicy(baseDelayMs = 100, maxDelayMs = 1000, factor = 2.0),
                    delayFn = { delays += it },
                ) {
                    calls++
                    if (calls < 3) throw IOException("boom") else "done"
                }
            assertThat(result).isEqualTo("done")
            assertThat(calls).isEqualTo(3)
            assertThat(delays).hasSize(2) // delayed before attempts 2 and 3
        }

    @Test
    fun `does not retry when shouldRetry rejects the error`() =
        runTest {
            var calls = 0
            val thrown =
                runCatching {
                    retryWithBackoff(
                        maxAttempts = 5,
                        shouldRetry = { it !is IllegalStateException },
                        delayFn = { },
                    ) {
                        calls++
                        error("fatal")
                    }
                }
            assertThat(calls).isEqualTo(1)
            assertThat(thrown.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        }

    @Test
    fun `propagates the last error after exhausting attempts`() =
        runTest {
            var calls = 0
            val thrown =
                runCatching {
                    retryWithBackoff(maxAttempts = 3, delayFn = { }) {
                        calls++
                        throw IOException("attempt $calls")
                    }
                }
            assertThat(calls).isEqualTo(3)
            assertThat(thrown.exceptionOrNull()).hasMessageThat().isEqualTo("attempt 3")
        }
}
