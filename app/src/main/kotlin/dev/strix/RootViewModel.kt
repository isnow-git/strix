package dev.strix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.strix.core.domain.onboarding.CredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Decides the start screen: onboarding when no source is stored, else channels. */
@HiltViewModel
class RootViewModel
    @Inject
    constructor(
        private val credentialStore: CredentialStore,
    ) : ViewModel() {
        private val mutableStartRoute = MutableStateFlow<String?>(null)

        /** Null until resolved, then [Routes.CHANNELS] or [Routes.ONBOARDING]. */
        val startRoute: StateFlow<String?> = mutableStartRoute.asStateFlow()

        init {
            viewModelScope.launch {
                mutableStartRoute.value =
                    if (credentialStore.current() != null) Routes.CHANNELS else Routes.ONBOARDING
            }
        }
    }
