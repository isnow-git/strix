package dev.strix.feature.onboarding.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.core.common.onboarding.CredentialReceiver
import dev.strix.core.common.result.StrixError
import dev.strix.core.common.result.StrixResult
import dev.strix.core.common.result.asFailure
import dev.strix.core.common.result.asSuccess
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the provider credentials with [EncryptedSharedPreferences] (AES-256),
 * so an M3U URL or Xtream username/password are never stored in clear text.
 * Implements [CredentialReceiver] for the onboarding flow.
 */
@Singleton
class SecureCredentialStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : CredentialReceiver {
        private val prefs: SharedPreferences by lazy {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        @Suppress("TooGenericExceptionCaught") // Keystore/crypto failures surface as a typed error.
        override suspend fun receive(source: StreamSourceConfig): StrixResult<Unit> =
            try {
                prefs
                    .edit()
                    .apply {
                        when (source) {
                            is StreamSourceConfig.M3u -> {
                                putString(KEY_TYPE, TYPE_M3U)
                                putString(KEY_URL, source.url)
                            }
                            is StreamSourceConfig.Xtream -> {
                                putString(KEY_TYPE, TYPE_XTREAM)
                                putString(KEY_HOST, source.host)
                                putString(KEY_USER, source.username)
                                putString(KEY_PASS, source.password)
                            }
                        }
                    }.commit()
                Unit.asSuccess()
            } catch (e: Exception) {
                StrixError.Unknown("Failed to store credentials", e).asFailure()
            }

        /** Reads the stored source, or null if onboarding never completed. */
        fun current(): StreamSourceConfig? =
            when (prefs.getString(KEY_TYPE, null)) {
                TYPE_M3U -> prefs.getString(KEY_URL, null)?.let(StreamSourceConfig::M3u)
                TYPE_XTREAM -> {
                    val host = prefs.getString(KEY_HOST, null)
                    val user = prefs.getString(KEY_USER, null)
                    val pass = prefs.getString(KEY_PASS, null)
                    if (host != null && user != null && pass != null) {
                        StreamSourceConfig.Xtream(host, user, pass)
                    } else {
                        null
                    }
                }
                else -> null
            }

        private companion object {
            const val FILE_NAME = "strix_credentials"
            const val KEY_TYPE = "type"
            const val KEY_URL = "url"
            const val KEY_HOST = "host"
            const val KEY_USER = "user"
            const val KEY_PASS = "pass"
            const val TYPE_M3U = "m3u"
            const val TYPE_XTREAM = "xtream"
        }
    }
