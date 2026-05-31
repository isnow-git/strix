plugins {
    alias(libs.plugins.strix.android.library)
    alias(libs.plugins.strix.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.strix.core.network"
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}
