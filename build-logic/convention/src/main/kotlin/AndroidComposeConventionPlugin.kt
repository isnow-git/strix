import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Enables Jetpack Compose: applies the Kotlin Compose compiler plugin, points it at
 * the shared stability config (so pure-Kotlin :core:model types are treated as
 * stable), and wires the Compose BOM + TV Material. Apply on top of
 * [strix.android.library] or [strix.android.application].
 *
 * Note: the TV-specific lazy containers (`androidx.tv.foundation`) are deprecated and
 * removed upstream — standard `androidx.compose.foundation` lazy lists are the TV path
 * now, so only `tv-material` is wired here.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ComposeCompilerGradlePluginExtension> {
                stabilityConfigurationFiles.add(
                    rootProject.layout.projectDirectory.file("config/compose/stability_config.conf"),
                )
            }

            // AGP registers the concrete extension type, not CommonExtension, so resolve
            // by the applied plugin rather than getByType<CommonExtension>().
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
                add("implementation", versionCatalog.findLibrary("androidx-tv-material").get())
                add("debugImplementation", versionCatalog.findLibrary("androidx-compose-ui-tooling").get())
            }
        }

    private fun enableCompose(extension: CommonExtension<*, *, *, *, *, *>) {
        extension.buildFeatures.compose = true
    }
}
