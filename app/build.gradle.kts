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
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.database)
    implementation(projects.core.network)
    implementation(projects.core.player)

    implementation(projects.feature.channels)
    implementation(projects.feature.player)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.epg)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
