package dev.strix.core.common.onboarding

import dev.strix.core.common.model.StreamSourceConfig

/**
 * Read side of the stored credentials. Lets the app decide whether to show
 * onboarding (no source yet) or jump straight to channels. Paired with
 * [CredentialReceiver] (the write side).
 */
interface CredentialStore {
    /** The stored stream source, or null if onboarding has never completed. */
    suspend fun current(): StreamSourceConfig?
}
