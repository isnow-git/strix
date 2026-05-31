package dev.strix.feature.player

import androidx.media3.common.PlaybackException

/**
 * Maps a [PlaybackException] error code to a short, cause-specific French
 * message. The raw `errorCodeName` is useless to a viewer; this tells them
 * whether the stream is dead, blocked, or unsupported — and whether retrying is
 * worth it.
 *
 * Takes the raw `errorCode` (a compile-time constant) rather than the exception
 * so it stays a pure function, unit-testable without constructing a
 * [PlaybackException] (whose constructor touches Android stubs).
 */
internal fun playbackErrorMessage(errorCode: Int): String =
    when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        ->
            "Connexion au flux impossible. Vérifie le réseau."

        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            "Le serveur a refusé le flux (souvent géo-bloqué ou expiré)."

        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "Flux introuvable (404). La chaîne n'existe plus."

        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED,
        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
        ->
            "Accès refusé (protégé ou non autorisé)."

        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        ->
            "Flux illisible (format invalide)."

        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        ->
            "Format de flux non supporté."

        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        ->
            "Décodage impossible sur cet appareil."

        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
            "Codec non supporté par cette TV."

        PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
            "Erreur réseau pendant la lecture."

        else ->
            "Lecture impossible."
    }
