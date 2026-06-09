import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Configures an Android library module with the shared Kotlin/Android settings. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                // Library code is shrunk by R8 (full mode) at the :app level. Modules add
                // their own consumer-rules.pro only when they need reflective keeps.
            }
        }
}
