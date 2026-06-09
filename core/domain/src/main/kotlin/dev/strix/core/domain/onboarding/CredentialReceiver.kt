package dev.strix.core.domain.onboarding

import dev.strix.core.common.result.StrixResult
import dev.strix.core.model.StreamSourceConfig

/**
 * Receives a [StreamSourceConfig] submitted during onboarding (e.g. from the phone
 * via the embedded server) and persists it securely. The implementation lives in the
 * data layer; onboarding depends only on this interface so it never touches
 * storage/crypto directly (Dependency Inversion + Interface Segregation).
 */
fun interface CredentialReceiver {
    /**
     * Validates and stores [source]. Returns success once persisted, or a typed
     * failure (e.g. parsing/validation) otherwise.
     */
    suspend fun receive(source: StreamSourceConfig): StrixResult<Unit>
}
