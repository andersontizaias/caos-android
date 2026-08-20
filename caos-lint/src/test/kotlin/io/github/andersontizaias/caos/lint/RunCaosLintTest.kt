package io.github.andersontizaias.caos.lint

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCaosLintTest {
    @TempDir
    lateinit var tempDir: Path

    private fun yamlFile(
        name: String,
        content: String,
    ): String {
        val file = tempDir.resolve(name)
        file.writeText(content)
        return file.toString()
    }

    @Test
    fun `prints usage and exits 1 when no arguments are given`() {
        val lines = mutableListOf<String>()
        val exitCode = runCaosLint(emptyArray()) { lines.add(it) }

        assertEquals(1, exitCode)
        assertEquals(listOf("Usage: caos-lint <yaml-file>", "Example: caos-lint caos.yaml"), lines)
    }

    @Test
    fun `exits 0 and prints a success message when there are no issues`() {
        val path = yamlFile("valid.yaml", "version: 1\nscreens: []")
        val lines = mutableListOf<String>()

        val exitCode = runCaosLint(arrayOf(path)) { lines.add(it) }

        assertEquals(0, exitCode)
        assertEquals("✅ No issues found", lines.last())
    }

    @Test
    fun `exits 0 and reports the warning count when there are only warnings`() {
        val yaml =
            """
            version: 1
            screens:
              - id: home
                container:
                  type: vertical
                shards:
                  - type: BannerView
            """.trimIndent()
        val path = yamlFile("warning_only.yaml", yaml)
        val lines = mutableListOf<String>()

        val exitCode = runCaosLint(arrayOf(path)) { lines.add(it) }

        assertEquals(0, exitCode)
        assertEquals("⚠ Shard of type 'BannerView' in screen 'home' has no id", lines[lines.size - 2])
        assertEquals("✅ No errors (1 warning(s))", lines.last())
    }

    @Test
    fun `exits 1 and prints errors when there are issues`() {
        val yaml =
            """
            version: 1
            screens:
              - id: home
                container:
                  type: vertical
                shards:
                  - type: ""
            """.trimIndent()
        val path = yamlFile("with_error.yaml", yaml)
        val lines = mutableListOf<String>()

        val exitCode = runCaosLint(arrayOf(path)) { lines.add(it) }

        assertEquals(1, exitCode)
        assertTrue(lines.any { it == "✗ Screen 'home': shard missing 'type' field" })
    }
}
