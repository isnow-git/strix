plugins {
    alias(libs.plugins.strix.kotlin.library)
}

// Pure-Kotlin domain contracts (repository + onboarding ports). The data layer
// implements these; presentation depends only on them (Dependency Inversion). No
// Android and no androidx.paging here — the Android PagingSource is exposed by
// :core:data on top of these interfaces.

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
