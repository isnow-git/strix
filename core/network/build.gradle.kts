plugins {
    alias(libs.plugins.strix.android.library)
    alias(libs.plugins.strix.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.strix.core.network"
}

// HTTP + streaming parsers (M3U / Xtream / XMLTV / iptv-org) and the resilience
// primitives (backoff, retry, circuit breaker). Parsers are streaming (O(1) memory)
// so an arbitrarily large playlist/guide never lands whole in RAM.

dependencies {
    implementation(projects.core.model)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}
