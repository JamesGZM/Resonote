import com.resonote.buildlogic.CheckDocumentationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

class DocumentationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("base")

        val checkDocumentation = tasks.register(
            "checkDocumentation",
            CheckDocumentationTask::class.java,
        ) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Checks documentation entry points and relative Markdown links."
            repositoryDirectory.set(layout.projectDirectory)
            markdownFiles.from(
                fileTree(layout.projectDirectory) {
                    include("*.md", "design/**/*.md", "docs/**/*.md")
                    exclude("**/build/**", "**/node_modules/**")
                },
            )
            requiredEntryPoints.set(
                listOf(
                    "AGENTS.md",
                    "README.md",
                    "design/COMPONENT_SYSTEM.md",
                    "design/FOUNDATION.md",
                    "design/PRODUCT_REQUIREMENTS.md",
                    "docs/README.md",
                    "docs/ARCHITECTURE.md",
                    "docs/DEVELOPMENT.md",
                    "docs/api/PROTOCOL.md",
                    "docs/api/README.md",
                ),
            )
            forbiddenReferences.set(
                listOf(
                    "DESIGN_SYSTEM_PLAN.md",
                    "HOME_IMPLEMENTATION_BASELINE.md",
                    "design/approved/home/",
                    "docs/api/catalog.yaml",
                    "docs/api/endpoints/",
                    "docs/api/schemas/",
                    "docs/api/tools/",
                ),
            )
            reportFile.set(layout.buildDirectory.file("reports/documentation/checkDocumentation.txt"))
        }

        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
            dependsOn(checkDocumentation)
        }
    }
}
