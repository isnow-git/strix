package dev.strix.core.model

/**
 * Where a user's channels come from. Captured during onboarding and persisted
 * (encrypted) by the credential store.
 */
sealed interface StreamSourceConfig {
    /** A plain M3U/M3U8 playlist URL. */
    data class M3u(
        val url: String,
    ) : StreamSourceConfig

    /** Xtream Codes account: the panel host plus credentials. */
    data class Xtream(
        val host: String,
        val username: String,
        val password: String,
    ) : StreamSourceConfig
}
