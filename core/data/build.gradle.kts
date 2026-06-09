plugins {
    alias(libs.plugins.strix.android.library)
    alias(libs.plugins.strix.android.hilt)
}

android {
    namespace = "dev.strix.core.data"
}

// Implementation layer: wires Room + network behind the :core:domain contracts.
// Reads come straight from Room (paging + flows); a refresh streams the source and
// writes in batches (PlaylistImporter) so a large playlist never lands whole in RAM.

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.core.network)

    implementation(libs.paging.runtime)
    implementation(libs.room.runtime)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}
