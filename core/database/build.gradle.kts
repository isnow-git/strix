plugins {
    alias(libs.plugins.strix.android.library)
    alias(libs.plugins.strix.android.hilt)
}

android {
    namespace = "dev.strix.core.database"
}

ksp {
    // Export Room schemas so migrations can be written and verified from v1.
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    implementation(libs.paging.runtime)
    add("ksp", libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
