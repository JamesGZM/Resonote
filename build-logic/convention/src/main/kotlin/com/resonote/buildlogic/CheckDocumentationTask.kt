package com.resonote.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class CheckDocumentationTask : DefaultTask() {
    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val markdownFiles: ConfigurableFileCollection

    @get:Input
    abstract val requiredEntryPoints: ListProperty<String>

    @get:Input
    abstract val forbiddenReferences: ListProperty<String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun check() {
        val root = repositoryDirectory.get().asFile
        val violations = validateDocumentation(
            root = root,
            markdownFiles = markdownFiles.files,
            requiredEntryPoints = requiredEntryPoints.get(),
            forbiddenReferences = forbiddenReferences.get(),
        )
        reportFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                if (violations.isEmpty()) {
                    "Documentation check passed for ${markdownFiles.files.size} Markdown files.\n"
                } else {
                    violations.joinToString(prefix = "Documentation check failed:\n", separator = "\n", postfix = "\n")
                },
            )
        }
        if (violations.isNotEmpty()) {
            throw GradleException(violations.joinToString(prefix = "Documentation check failed:\n", separator = "\n"))
        }
    }
}

internal fun validateDocumentation(
    root: File,
    markdownFiles: Set<File>,
    requiredEntryPoints: List<String>,
    forbiddenReferences: List<String>,
): List<String> = buildList {
    requiredEntryPoints.forEach { path ->
        if (!root.resolve(path).isFile) add("Missing required documentation entry point: $path")
    }

    markdownFiles.sortedBy { it.relativeTo(root).invariantSeparatorsPath }.forEach { markdown ->
        val relativeMarkdown = markdown.relativeTo(root).invariantSeparatorsPath
        val content = markdown.readText()
        forbiddenReferences.forEach { reference ->
            if (reference in content) add("$relativeMarkdown references deleted documentation: $reference")
        }
        MARKDOWN_LINK.findAll(content).forEach { match ->
            val rawTarget = match.groupValues[1].trim().removeSurrounding("<", ">")
            val path = rawTarget.substringBefore('#').substringBefore('?')
            if (path.isBlank() || path.startsWith('/') || EXTERNAL_SCHEME.matches(path)) return@forEach
            val target = markdown.parentFile.resolve(path).normalize()
            if (!target.exists()) add("$relativeMarkdown contains a broken relative link: $rawTarget")
        }
    }
}.distinct().sorted()

private val MARKDOWN_LINK = Regex("""!?\[[^]]*]\(([^)]+)\)""")
private val EXTERNAL_SCHEME = Regex("""[a-zA-Z][a-zA-Z0-9+.-]*:.*""")
