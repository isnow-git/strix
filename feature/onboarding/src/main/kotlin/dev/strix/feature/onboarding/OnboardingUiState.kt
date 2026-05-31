package dev.strix.feature.onboarding

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/** Phases of the onboarding flow. */
enum class OnboardingPhase { Starting, WaitingForPhone, Importing, Done, Error }

/** Immutable MVI state for the onboarding screen. */
@Immutable
data class OnboardingUiState(
    val phase: OnboardingPhase = OnboardingPhase.Starting,
    val pairingUrl: String? = null,
    val qrCode: ImageBitmap? = null,
    val message: String? = null,
)
