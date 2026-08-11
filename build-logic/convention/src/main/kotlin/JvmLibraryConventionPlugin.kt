import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(17)
                compilerOptions {
                    jvmTarget = JvmTarget.JVM_17
                    allWarningsAsErrors = providers.gradleProperty("warningsAsErrors").map(String::toBoolean).orElse(false)
                    freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
                }
            }
        }
    }
}
