package dev.strix.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import dev.strix.core.designsystem.theme.StrixPalette

/**
 * Onboarding screen: shows a QR the phone scans to open the credential form served by
 * the TV. The embedded server is started here and stopped on dispose, so it costs
 * nothing once onboarding is over.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }
    DisposableEffect(Unit) { onDispose { viewModel.stop() } }
    LaunchedEffect(state.phase) {
        if (state.phase == OnboardingPhase.Done) onDone()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state.phase) {
            OnboardingPhase.Starting ->
                Text(
                    text = stringResource(R.string.onboarding_starting),
                    color = StrixPalette.OnBackground,
                    fontSize = 20.sp,
                )

            OnboardingPhase.WaitingForPhone -> {
                Text(
                    text = stringResource(R.string.onboarding_scan_title),
                    color = StrixPalette.OnBackground,
                    fontSize = 28.sp,
                )
                Text(
                    text = stringResource(R.string.onboarding_scan_subtitle),
                    color = StrixPalette.Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                state.qrCode?.let { qr ->
                    Image(
                        bitmap = qr,
                        contentDescription = stringResource(R.string.onboarding_qr_cd),
                        modifier =
                            Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(12.dp),
                    )
                }
                state.pairingUrl?.let { url ->
                    Text(
                        text = url,
                        color = StrixPalette.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }

            OnboardingPhase.Importing ->
                Text(
                    text = stringResource(R.string.onboarding_importing),
                    color = StrixPalette.OnBackground,
                    fontSize = 20.sp,
                )

            OnboardingPhase.Done -> {
                val text =
                    state.importedCount
                        ?.let { pluralStringResource(R.plurals.onboarding_imported, it, it) }
                        ?: stringResource(R.string.onboarding_done)
                Text(text = text, color = StrixPalette.OnBackground, fontSize = 20.sp)
            }

            OnboardingPhase.Error -> {
                val text =
                    state.messageRes?.let { stringResource(it) }
                        ?: stringResource(R.string.onboarding_error_generic)
                Text(text = text, color = StrixPalette.Error, fontSize = 18.sp)
            }
        }
    }
}
