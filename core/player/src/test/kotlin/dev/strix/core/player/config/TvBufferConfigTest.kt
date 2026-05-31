package dev.strix.core.player.config

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TvBufferConfigTest {
    @Test
    fun `defaults satisfy all invariants`() {
        val config = TvBufferConfig()
        assertThat(config.minBufferMs).isAtMost(config.maxBufferMs)
        assertThat(config.bufferForPlaybackMs).isAtMost(config.minBufferMs)
        assertThat(config.backBufferMs).isAtLeast(0)
    }

    @Test
    fun `min greater than max is rejected`() {
        assertFailsWithIae { TvBufferConfig(minBufferMs = 40_000, maxBufferMs = 30_000) }
    }

    @Test
    fun `playback buffer greater than min is rejected`() {
        assertFailsWithIae { TvBufferConfig(minBufferMs = 1_000, bufferForPlaybackMs = 2_000) }
    }

    @Test
    fun `negative back buffer is rejected`() {
        assertFailsWithIae { TvBufferConfig(backBufferMs = -1) }
    }

    private fun assertFailsWithIae(block: () -> Unit) {
        val thrown = runCatching { block() }.exceptionOrNull()
        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
    }
}
