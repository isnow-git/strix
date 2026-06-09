plugins {
    alias(libs.plugins.strix.android.library)
    alias(libs.plugins.strix.android.hilt)
}

android {
    namespace = "dev.strix.core.player"
}

// Media3 wrapper: a factory that builds TV-tuned ExoPlayers (low-RAM LoadControl +
// calibrated ABR) and a deterministic playback controller (explicit picture state,
// generation-guarded timeouts) so the UI never has to reason about player races.

dependencies {
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.session)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
