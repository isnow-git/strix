import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Wires Hilt + KSP codegen (zero runtime reflection). */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("com.google.dagger.hilt.android")
            }

            dependencies {
                add("implementation", versionCatalog.findLibrary("hilt-android").get())
                add("ksp", versionCatalog.findLibrary("hilt-compiler").get())
            }
        }
}
