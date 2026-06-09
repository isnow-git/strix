plugins {
    alias(libs.plugins.strix.android.library)
    alias(libs.plugins.strix.android.compose)
    alias(libs.plugins.strix.android.hilt)
}

android {
    namespace = "dev.strix.feature.channels"
}

// The channel browser: a decomposed, recomposition-isolated Compose UI driven by a
// single ViewModel state machine, with deterministic keypad zapping and a graphicsLayer
// preview->fullscreen morph. No screen-local player/zap state soup (that was the source
// of the old "works one time in two" behaviour).

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.player)
    implementation(projects.core.designsystem)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    implementation(libs.coil.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}
