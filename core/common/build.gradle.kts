plugins {
    alias(libs.plugins.strix.kotlin.library)
}

// Pure-Kotlin cross-cutting utilities: the Result/Error taxonomy and the dispatcher
// abstraction. No Android so it stays unit-testable on the JVM.

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}
