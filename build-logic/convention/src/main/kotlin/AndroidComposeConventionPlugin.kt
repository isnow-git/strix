import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Enables Jetpack Compose + the Kotlin compose compiler plugin and wires the
 * Compose BOM + TV artifacts. Apply on top of [strix.android.library] or
 * [strix.android.application].
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        // Mark the pure-Kotlin domain models stable for Compose (they can't be
        // inferred because :core:common isn't processed by the Compose compiler).
        extensions.configure<ComposeCompilerGradlePluginExtension> {
            stabilityConfigurationFiles.add(
                rootProject.layout.projectDirectory.file("config/compose/stability_config.conf"),
            )
        }

        // AGP registers the extension under the concrete type (ApplicationExtension
        // or LibraryExtension), not CommonExtension, so we resolve by the applied
        // plugin instead of getByType<CommonExtension>().
        when {
            pluginManager.hasPlugin("com.android.application") ->
                extensions.configure<ApplicationExtension> { enableCompose(this) }
            pluginManager.hasPlugin("com.android.library") ->
                extensions.configure<LibraryExtension> { enableCompose(this) }
        }

        dependencies {
            add("implementation", platform(versionCatalog.findLibrary("androidx-compose-bom").get()))
            add("androidTestImplementation", platform(versionCatalog.findLibrary("androidx-compose-bom").get()))

            add("implementation", versionCatalog.findLibrary("androidx-compose-ui").get())
            add("implementation", versionCatalog.findLibrary("androidx-compose-ui-graphics").get())
            add("implementation", versionCatalog.findLibrary("androidx-compose-ui-tooling-preview").get())
            add("implementation", versionCatalog.findLibrary("androidx-compose-foundation").get())
            add("implementation", versionCatalog.findLibrary("androidx-tv-foundation").get())
            add("implementation", versionCatalog.findLibrary("androidx-tv-material").get())
            add("debugImplementation", versionCatalog.findLibrary("androidx-compose-ui-tooling").get())
        }
    }

    private fun enableCompose(extension: CommonExtension<*, *, *, *, *, *>) {
        extension.buildFeatures.compose = true
    }
}
