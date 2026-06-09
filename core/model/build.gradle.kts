plugins {
    alias(libs.plugins.strix.kotlin.library)
}

// Pure-Kotlin domain models. No Android, no third-party runtime deps, so the rest of
// the app can depend on the vocabulary of the domain without pulling in a framework.

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
