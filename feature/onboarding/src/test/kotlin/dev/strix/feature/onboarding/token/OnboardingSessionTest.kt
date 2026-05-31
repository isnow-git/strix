package dev.strix.feature.onboarding.token

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnboardingSessionTest {
    private var clock = 0L
    private var counter = 0

    private fun session(ttlMs: Long = 1000) =
        OnboardingSession(ttlMs = ttlMs, now = { clock }, generator = { "token-${counter++}" })

    @Test
    fun `a freshly issued token is valid`() {
        val s = session()
        val token = s.issue()
        assertThat(s.isValid(token.value)).isTrue()
    }

    @Test
    fun `a wrong token is rejected`() {
        val s = session()
        s.issue()
        assertThat(s.isValid("nope")).isFalse()
    }

    @Test
    fun `validation before issuing fails`() {
        assertThat(session().isValid("anything")).isFalse()
    }

    @Test
    fun `an expired token is rejected`() {
        val s = session(ttlMs = 1000)
        val token = s.issue()
        clock += 1001
        assertThat(s.isValid(token.value)).isFalse()
    }

    @Test
    fun `consume succeeds once then the token is burned`() {
        val s = session()
        val token = s.issue()
        assertThat(s.consume(token.value)).isTrue()
        assertThat(s.consume(token.value)).isFalse()
        assertThat(s.isValid(token.value)).isFalse()
    }

    @Test
    fun `re-issuing resets the consumed flag`() {
        val s = session()
        val first = s.issue()
        s.consume(first.value)
        val second = s.issue()
        assertThat(s.isValid(second.value)).isTrue()
    }
}
