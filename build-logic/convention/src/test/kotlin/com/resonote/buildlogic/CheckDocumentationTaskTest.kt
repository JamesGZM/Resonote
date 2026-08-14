package com.resonote.buildlogic

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckDocumentationTaskTest {
    @Test
    fun acceptsExistingRelativeLinks() {
        val root = createTempDirectory().toFile()
        val index = root.write("README.md", "[API](docs/api/README.md)")
        val api = root.write("docs/api/README.md", "# API")

        val violations = validateDocumentation(root, setOf(index, api), listOf("README.md"), emptyList())

        assertTrue(violations.isEmpty())
    }

    @Test
    fun reportsMissingEntryBrokenLinkAndDeletedReference() {
        val root = createTempDirectory().toFile()
        val index = root.write("README.md", "[old](docs/removed.md)\nold-plan.md")

        val violations = validateDocumentation(
            root = root,
            markdownFiles = setOf(index),
            requiredEntryPoints = listOf("AGENTS.md"),
            forbiddenReferences = listOf("old-plan.md"),
        )

        assertEquals(3, violations.size)
        assertTrue(violations.any { "Missing required" in it })
        assertTrue(violations.any { "broken relative link" in it })
        assertTrue(violations.any { "references deleted documentation" in it })
    }
}

private fun File.write(path: String, content: String): File = resolve(path).apply {
    parentFile.mkdirs()
    writeText(content)
}
