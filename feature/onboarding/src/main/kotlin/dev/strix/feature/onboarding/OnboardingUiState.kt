package dev.strix.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/** Phases of the onboarding flow. */
enum class OnboardingPhase { Starting, WaitingForPhone, Importing, Done, Error }

/**
 * Immutable MVI state for the onboarding screen.
 *
 * Status/error copy is carried as a `@StringRes` id (resolved by the screen) so the
 * ViewModel never touches `Context` or hardcoded text. [importedCount] feeds the
 * "N channels imported" plural on the done screen.
 */
@Immutable
data class OnboardingUiState(
    val phase: OnboardingPhase = OnboardingPhase.Starting,
    val pairingUrl: String? = null,
    val qrCode: ImageBitmap? = null,
    @StringRes val messageRes: Int? = null,
    val importedCount: Int? = null,
)
