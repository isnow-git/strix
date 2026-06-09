plugins {
    alias(libs.plugins.strix.android.library)
    alias(libs.plugins.strix.android.compose)
    alias(libs.plugins.strix.android.hilt)
}

android {
    namespace = "dev.strix.feature.epg"
}

// The program guide: a channels x clock timeline backed by the XMLTV programmes already
// ingested into Room. Channels page vertically (keyset, O(log n)); each row's programmes
// load lazily for the visible time window.

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
