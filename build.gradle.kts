// Root build file. Modules configure themselves through the `strix.*` convention
// plugins in `build-logic/`; nothing is configured here. Plugins are declared with
// `apply false` so their versions resolve from the catalog when a module applies them.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// Strix lives under OneDrive, whose background sync intermittently locks freshly-written
// build artifacts (e.g. classes.jar) and fails the build with a FileSystemException.
// Redirect every module's build output to a non-synced location under the user home
// (next to the Gradle home) so builds are reliable. Source stays in the repo.
val strixBuildRoot = file("${System.getProperty("user.home")}/.strix-build/${rootProject.name}")
allprojects {
    layout.buildDirectory.set(
        layout.dir(
            provider {
                strixBuildRoot.resolve(path.removePrefix(":").replace(':', '/').ifEmpty { "root" })
            },
        ),
    )
}

