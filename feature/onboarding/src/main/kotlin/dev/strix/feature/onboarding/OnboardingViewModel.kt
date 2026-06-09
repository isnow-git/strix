package dev.strix.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.strix.core.common.result.StrixResult
import dev.strix.core.domain.epg.EpgRepository
import dev.strix.core.domain.onboarding.CredentialReceiver
import dev.strix.core.domain.repository.ChannelRepository
import dev.strix.core.model.StreamSourceConfig
import dev.strix.feature.onboarding.net.LocalAddressFinder
import dev.strix.feature.onboarding.qr.QrGenerator
import dev.strix.feature.onboarding.server.OnboardingServer
import dev.strix.feature.onboarding.token.OnboardingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives onboarding: starts the embedded server, shows a QR for the phone, and on
 * submission stores the credentials and imports the playlist. The server is stopped from
 * the screen's lifecycle (see [stop]) so it has zero cost once onboarding is done.
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val credentialReceiver: CredentialReceiver,
        private val channelRepository: ChannelRepository,
        private val epgRepository: EpgRepository,
    ) : ViewModel() {
        private val session = OnboardingSession()
        private var server: OnboardingServer? = null

        private val mutableUiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

        fun start() {
            if (server != null) return
            viewModelScope.launch(Dispatchers.IO) {
                val ip = LocalAddressFinder.siteLocalIpv4()
                if (ip == null) {
                    fail(
                        "Aucun réseau local trouvé. Connecte la TV au Wi-Fi ou à l'Ethernet. " +
                            "Les réseaux invités avec isolation client ne marchent pas.",
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
                        fail("Impossible de charger le formulaire d'onboarding.")
                        return@launch
                    }

                val token = session.issue()
                val httpServer = OnboardingServer(ip, session, html, ::onCredentials)
                runCatching { httpServer.start() }.onFailure {
                    fail("Impossible de démarrer le serveur d'appairage.")
                    return@launch
                }
                server = httpServer

                val url = "http://$ip:${httpServer.listeningPort}/?t=${token.value}"
                mutableUiState.value =
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
                mutableUiState.update { it.copy(phase = OnboardingPhase.Importing, message = null) }
                when (val stored = credentialReceiver.receive(source)) {
                    is StrixResult.Failure -> {
                        fail(stored.error.message ?: "Échec de l'enregistrement des identifiants.")
                        return@launch
                    }
                    is StrixResult.Success -> Unit
                }
                when (val refreshed = channelRepository.refreshFrom(source)) {
                    is StrixResult.Failure ->
                        fail(refreshed.error.message ?: "Enregistré, mais impossible de charger les chaînes.")
                    is StrixResult.Success -> {
                        mutableUiState.value =
                            OnboardingUiState(
                                phase = OnboardingPhase.Done,
                                message = "${refreshed.value} chaînes importées.",
                            )
                        // Ingest the EPG guide in the background, deferred so it doesn't
                        // compete with the first browse right after import.
                        viewModelScope.launch {
                            delay(EPG_INGEST_DELAY_MS)
                            epgRepository.refresh()
                        }
                    }
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
            mutableUiState.value = OnboardingUiState(phase = OnboardingPhase.Error, message = message)
        }

        private companion object {
            const val ASSET_FORM = "onboarding.html"
            const val EPG_INGEST_DELAY_MS = 8_000L
        }
    }
