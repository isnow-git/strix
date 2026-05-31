package dev.strix.feature.onboarding.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.strix.core.common.onboarding.CredentialReceiver
import dev.strix.feature.onboarding.security.SecureCredentialStore

/** Binds the onboarding credential store. */
@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {
    @Binds
    abstract fun bindCredentialReceiver(impl: SecureCredentialStore): CredentialReceiver
}
