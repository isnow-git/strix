plugins {
    alias(libs.plugins.strix.android.application)
    alias(libs.plugins.strix.android.compose)
    alias(libs.plugins.strix.android.hilt)
}

android {
    namespace = "dev.strix"

    defaultConfig {
        applicationId = "dev.strix"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Local-testing convenience: sign the optimized (R8 full-mode) release build
            // with the debug key so it installs on the TV. Swap for a real upload key
            // before publishing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(projects.feature.channels)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.epg)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    // Coil 3: the app installs the shared singleton ImageLoader (network fetcher comes
    // transitively from :core:designsystem).
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
