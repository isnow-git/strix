import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Single source of truth for SDK levels across every Android module. */
object StrixSdk {
    const val COMPILE = 36
    const val TARGET = 36
    const val MIN = 26
}

// NOTE: deliberately NOT named `libs` — a top-level `Project.libs` would shadow
// Gradle's generated `libs` catalog accessor inside module build scripts, breaking
// every `libs.androidx.*` reference there.
val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Shared Android + Kotlin config applied by both the app and library convention plugins. */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = StrixSdk.COMPILE

        defaultConfig {
            minSdk = StrixSdk.MIN
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    configureQuality()
}

/** Applies ktlint + detekt with a shared config. Called by every module. */
internal fun Project.configureQuality() {
    with(pluginManager) {
        apply("org.jlleitschuh.gradle.ktlint")
        apply("io.gitlab.arturbosch.detekt")
    }

    extensions.configure(org.jlleitschuh.gradle.ktlint.KtlintExtension::class.java) {
        version.set("1.4.1")
        android.set(true)
        ignoreFailures.set(false)
    }

    extensions.configure(io.gitlab.arturbosch.detekt.extensions.DetektExtension::class.java) {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        parallel = true
    }
}
