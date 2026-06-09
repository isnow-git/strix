plugins {
    alias(libs.plugins.strix.android.library)
    alias(libs.plugins.strix.android.compose)
}

android {
    namespace = "dev.strix.core.designsystem"
}

// The Strix design language: dark palette, Outfit type scale, cheap "glass" surface,
// D-pad focus helpers. Compose UI lives here so features share one visual vocabulary.

dependencies {
    implementation(libs.androidx.core.ktx)

    // Coil 3: the shared image loader builder lives here; coil-network-okhttp is an
    // implementation dep so its network fetcher auto-registers on the app runtime path.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
