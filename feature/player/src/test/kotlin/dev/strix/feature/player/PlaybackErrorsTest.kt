package dev.strix.feature.player

import androidx.media3.common.PlaybackException
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackErrorsTest {
    @Test
    fun `bad http status hints at geo-block or expiry`() {
        val message = playbackErrorMessage(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        assertThat(message).contains("géo-bloqué")
    }

    @Test
    fun `unsupported codec is reported as such`() {
        val message = playbackErrorMessage(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED)
        assertThat(message).contains("Codec")
    }

    @Test
    fun `unknown code falls back to a generic message`() {
        val message = playbackErrorMessage(PlaybackException.ERROR_CODE_UNSPECIFIED)
        assertThat(message).isEqualTo("Lecture impossible.")
    }
}
