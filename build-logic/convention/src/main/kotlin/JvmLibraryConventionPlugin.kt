import com.resonote.buildlogic.configureSpotlessForJvm
import com.resonote.buildlogic.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            apply(plugin = "resonote.android.lint")
            configureSpotlessForJvm()
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
            extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(17)
                compilerOptions {
                    jvmTarget = JvmTarget.JVM_11
                    allWarningsAsErrors = providers.gradleProperty("warningsAsErrors").map(String::toBoolean).orElse(false)
                    freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
                }
            }
            dependencies {
                "testImplementation"(libs.findLibrary("kotlin.test").get())
            }
        }
    }
}
