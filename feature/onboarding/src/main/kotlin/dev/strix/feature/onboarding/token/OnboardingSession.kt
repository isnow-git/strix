package dev.strix.feature.onboarding.token

import java.util.UUID

/** An issued pairing token with its absolute expiry. */
data class OnboardingToken(
    val value: String,
    val expiresAtMs: Long,
)

/**
 * Single pairing session: issues a token encoded in the QR, then accepts exactly
 * **one** valid submission within a short TTL. Security properties (ADR-0005):
 * single-use, short-lived, one submission only.
 *
 * [now] and [generator] are injectable for deterministic tests.
 */
class OnboardingSession(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val generator: () -> String = { UUID.randomUUID().toString().replace("-", "") },
) {
    private var token: OnboardingToken? = null
    private var consumed = false

    /** Issues (or re-issues) the token, resetting the consumed flag. */
    fun issue(): OnboardingToken {
        val issued = OnboardingToken(value = generator(), expiresAtMs = now() + ttlMs)
        token = issued
        consumed = false
        return issued
    }

    /** True if [candidate] matches the live, unexpired, unconsumed token. */
    fun isValid(candidate: String): Boolean {
        val current = token ?: return false
        return !consumed && now() <= current.expiresAtMs && candidate == current.value
    }

    /** Validates and burns the token (single-use). Returns false if invalid. */
    fun consume(candidate: String): Boolean {
        if (!isValid(candidate)) return false
        consumed = true
        return true
    }

    private companion object {
        const val DEFAULT_TTL_MS = 5 * 60 * 1000L
    }
}
