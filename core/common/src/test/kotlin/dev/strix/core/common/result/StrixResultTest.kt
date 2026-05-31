package dev.strix.core.common.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StrixResultTest {
    private val success: StrixResult<Int> = StrixResult.Success(2)
    private val failure: StrixResult<Int> = StrixResult.Failure(StrixError.NotFound("missing"))

    @Test
    fun `isSuccess reflects variant`() {
        assertThat(success.isSuccess).isTrue()
        assertThat(failure.isSuccess).isFalse()
    }

    @Test
    fun `getOrNull returns value or null`() {
        assertThat(success.getOrNull()).isEqualTo(2)
        assertThat(failure.getOrNull()).isNull()
    }

    @Test
    fun `errorOrNull returns error or null`() {
        assertThat(success.errorOrNull()).isNull()
        assertThat(failure.errorOrNull()).isEqualTo(StrixError.NotFound("missing"))
    }

    @Test
    fun `map transforms success and preserves failure`() {
        assertThat(success.map { it * 10 }).isEqualTo(StrixResult.Success(20))
        assertThat(failure.map { it * 10 }).isEqualTo(failure)
    }

    @Test
    fun `map does not invoke transform on failure`() {
        var called = false
        failure.map {
            called = true
            it
        }
        assertThat(called).isFalse()
    }

    @Test
    fun `flatMap chains success and short-circuits failure`() {
        val chained = success.flatMap { StrixResult.Success(it + 1) }
        assertThat(chained).isEqualTo(StrixResult.Success(3))

        val shortCircuit = failure.flatMap { StrixResult.Success(it + 1) }
        assertThat(shortCircuit).isEqualTo(failure)
    }

    @Test
    fun `fold collapses both branches`() {
        assertThat(success.fold(onSuccess = { "v=$it" }, onFailure = { "e" })).isEqualTo("v=2")
        assertThat(failure.fold(onSuccess = { "v=$it" }, onFailure = { it.message })).isEqualTo("missing")
    }

    @Test
    fun `getOrElse supplies fallback only on failure`() {
        assertThat(success.getOrElse { -1 }).isEqualTo(2)
        assertThat(failure.getOrElse { -1 }).isEqualTo(-1)
    }

    @Test
    fun `asSuccess and asFailure wrap values`() {
        assertThat(5.asSuccess()).isEqualTo(StrixResult.Success(5))
        assertThat(StrixError.Timeout().asFailure()).isEqualTo(StrixResult.Failure(StrixError.Timeout()))
    }
}
