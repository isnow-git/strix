package dev.strix.feature.onboarding.net

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.net.InetAddress

class LocalAddressFinderTest {
    private fun addr(host: String): InetAddress = InetAddress.getByName(host)

    @Test
    fun `picks the site-local IPv4`() {
        val result =
            LocalAddressFinder.pickSiteLocalIpv4(
                sequenceOf(addr("8.8.8.8"), addr("192.168.1.42"), addr("10.0.0.5")),
            )
        assertThat(result).isEqualTo("192.168.1.42")
    }

    @Test
    fun `ignores public and loopback IPv4`() {
        val result =
            LocalAddressFinder.pickSiteLocalIpv4(
                sequenceOf(addr("8.8.8.8"), addr("127.0.0.1")),
            )
        assertThat(result).isNull()
    }

    @Test
    fun `ignores IPv6 addresses`() {
        val result = LocalAddressFinder.pickSiteLocalIpv4(sequenceOf(addr("fe80::1"), addr("::1")))
        assertThat(result).isNull()
    }

    @Test
    fun `recognises the 10-dot and 172-16 ranges`() {
        assertThat(LocalAddressFinder.pickSiteLocalIpv4(sequenceOf(addr("10.1.2.3")))).isEqualTo("10.1.2.3")
        assertThat(LocalAddressFinder.pickSiteLocalIpv4(sequenceOf(addr("172.16.5.9")))).isEqualTo("172.16.5.9")
    }
}
