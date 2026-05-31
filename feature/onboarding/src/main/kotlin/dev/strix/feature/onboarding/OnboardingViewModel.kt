package dev.strix.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.core.common.onboarding.CredentialReceiver
import dev.strix.core.common.repository.ChannelRepository
import dev.strix.core.common.result.StrixResult
import dev.strix.feature.onboarding.net.LocalAddressFinder
import dev.strix.feature.onboarding.qr.QrGenerator
import dev.strix.feature.onboarding.server.OnboardingServer
import dev.strix.feature.onboarding.token.OnboardingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives onboarding: starts the embedded server, shows a QR for the phone, and
 * on submission stores the credentials and imports the playlist. The server is
 * stopped from the screen's lifecycle (see [stop]) so it has zero cost once
 * onboarding is done (ADR-0005).
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val credentialReceiver: CredentialReceiver,
        private val channelRepository: ChannelRepository,
    ) : ViewModel() {
        private val session = OnboardingSession()
        private var server: OnboardingServer? = null

        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        fun start() {
            if (server != null) return
            viewModelScope.launch(Dispatchers.IO) {
                val ip = LocalAddressFinder.siteLocalIpv4()
                if (ip == null) {
                    fail(
                        "No local network found. Connect the TV to Wi-Fi or Ethernet. " +
                            "Guest networks with client isolation won't work.",
                    )
                    return@launch
                }
                val html =
                    runCatching {
                        context.assets
                            .open(ASSET_FORM)
                            .bufferedReader()
                            .use { it.readText() }
                    }.getOrElse {
                        fail("Could not load the onboarding form.")
                        return@launch
                    }

                val token = session.issue()
                val httpServer = OnboardingServer(ip, session, html, ::onCredentials)
                runCatching { httpServer.start() }.onFailure {
                    fail("Could not start the pairing server.")
                    return@launch
                }
                server = httpServer

                val url = "http://$ip:${httpServer.listeningPort}/?t=${token.value}"
                _uiState.value =
                    OnboardingUiState(
                        phase = OnboardingPhase.WaitingForPhone,
                        pairingUrl = url,
                        qrCode = QrGenerator.encode(url),
                    )
            }
        }

        /** Called from the server thread when the phone submits credentials. */
        private fun onCredentials(source: StreamSourceConfig) {
            viewModelScope.launch {
                _uiState.update { it.copy(phase = OnboardingPhase.Importing, message = null) }
                when (val stored = credentialReceiver.receive(source)) {
                    is StrixResult.Failure -> {
                        fail(stored.error.message ?: "Failed to save credentials.")
                        return@launch
                    }
                    is StrixResult.Success -> Unit
                }
                when (val refreshed = channelRepository.refreshFrom(source)) {
                    is StrixResult.Failure ->
                        fail(refreshed.error.message ?: "Saved, but could not load channels.")
                    is StrixResult.Success ->
                        _uiState.value =
                            OnboardingUiState(
                                phase = OnboardingPhase.Done,
                                message = "Imported ${refreshed.value} channels.",
                            )
                }
                stop()
            }
        }

        fun stop() {
            server?.stop()
            server = null
        }

        override fun onCleared() {
            stop()
        }

        private fun fail(message: String) {
            _uiState.value = OnboardingUiState(phase = OnboardingPhase.Error, message = message)
        }

        private companion object {
            const val ASSET_FORM = "onboarding.html"
        }
    }
