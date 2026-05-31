package dev.strix.core.player.config

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AbrConfigTest {
    @Test
    fun `defaults satisfy all invariants`() {
        val config = AbrConfig()
        assertThat(config.minDurationToRetainAfterDiscardMs)
            .isAtLeast(config.maxDurationForQualityDecreaseMs)
        assertThat(config.bandwidthFraction).isGreaterThan(0f)
        assertThat(config.bandwidthFraction).isAtMost(1f)
    }

    @Test
    fun `retain shorter than max-decrease is rejected`() {
        assertFailsWithIae {
            AbrConfig(maxDurationForQualityDecreaseMs = 25_000, minDurationToRetainAfterDiscardMs = 10_000)
        }
    }

    @Test
    fun `bandwidth fraction out of range is rejected`() {
        assertFailsWithIae { AbrConfig(bandwidthFraction = 0f) }
        assertFailsWithIae { AbrConfig(bandwidthFraction = 1.5f) }
    }

    private fun assertFailsWithIae(block: () -> Unit) {
        val thrown = runCatching { block() }.exceptionOrNull()
        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
    }
}
