package dev.strix.core.common.onboarding

import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.core.common.result.StrixResult

/**
 * Receives a [StreamSourceConfig] submitted during onboarding (e.g. from the
 * phone via the embedded server) and persists it securely. The implementation
 * lives in the data layer; onboarding depends only on this interface so it never
 * touches storage/crypto directly (DIP + ISP).
 */
fun interface CredentialReceiver {
    /**
     * Validates and stores [source]. Returns success once persisted, or a typed
     * failure (e.g. parsing/validation) otherwise.
     */
    suspend fun receive(source: StreamSourceConfig): StrixResult<Unit>
}
