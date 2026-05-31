plugins {
    alias(libs.plugins.strix.android.library)
    alias(libs.plugins.strix.android.compose)
}

android {
    namespace = "dev.strix.core.ui"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
}
