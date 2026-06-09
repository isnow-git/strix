plugins {
    alias(libs.plugins.strix.android.library)
    alias(libs.plugins.strix.android.compose)
    alias(libs.plugins.strix.android.hilt)
}

android {
    namespace = "dev.strix.feature.onboarding"
}

// No-cloud onboarding: the TV hosts a one-page form on the LAN (NanoHTTPD), the phone
// scans a QR (ZXing) and submits the IPTV source — no painful remote typing. Credential
// storage + import live in :core:data; this module only drives the pairing flow.

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.nanohttpd)
    implementation(libs.zxing.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
