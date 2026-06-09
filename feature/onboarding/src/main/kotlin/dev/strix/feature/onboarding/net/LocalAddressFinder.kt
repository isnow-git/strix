package dev.strix.feature.onboarding.net

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Finds the TV's site-local IPv4 address so the phone can reach the embedded onboarding
 * server over the LAN. Enumerating [NetworkInterface] covers both Ethernet and Wi-Fi.
 */
object LocalAddressFinder {
    /**
     * Picks the first site-local IPv4 (`10.x`, `172.16-31.x`, `192.168.x`) from
     * [addresses], ignoring IPv6 and public/loopback addresses. Pure so it can be
     * unit-tested without real interfaces.
     */
    fun pickSiteLocalIpv4(addresses: Sequence<InetAddress>): String? =
        addresses
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress

    /** Resolves the device's site-local IPv4 across all up, non-loopback interfaces. */
    fun siteLocalIpv4(): String? =
        pickSiteLocalIpv4(
            NetworkInterface
                .getNetworkInterfaces()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() },
        )
}
