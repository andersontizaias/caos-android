package io.github.andersontizaias.caos.lint

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaosLintTest {
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
    fun `reports an error when the file cannot be read`() {
        val result = lint("$tempDir/does_not_exist.yaml") { }
        assertEquals(listOf("Could not read file: $tempDir/does_not_exist.yaml"), result.errors)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `reports a friendly error when version is missing`() {
        val path = yamlFile("no_version.yaml", "screens: []")
        val result = lint(path) { }
        assertEquals(
            listOf("Missing 'version' field — YAML v1 requires version: 1 as the first key"),
            result.errors,
        )
    }

    @Test
    fun `reports a friendly error when the schema version is unsupported`() {
        val path = yamlFile("bad_version.yaml", "version: 99\nscreens: []")
        val result = lint(path) { }
        assertEquals(listOf("Unsupported schema version 99 — expected 1"), result.errors)
    }

    @Test
    fun `a valid file with no issues reports zero errors and warnings`() {
        val yaml =
            """
            version: 1
            screens:
              - id: home
                container:
                  type: vertical
                shards:
                  - type: CardView
                    id: card_1
            """.trimIndent()
        val path = yamlFile("valid.yaml", yaml)

        val lines = mutableListOf<String>()
        val result = lint(path) { lines.add(it) }

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertEquals(listOf("✓ version: 1", "✓ screens: 1 found", "✓ shards: 1 found"), lines)
    }

    @Test
    fun `reports an error for a shard missing its type`() {
        val yaml =
            """
            version: 1
            screens:
              - id: home
                container:
                  type: vertical
                shards:
                  - type: ""
                    id: c1
            """.trimIndent()
        val path = yamlFile("missing_type.yaml", yaml)

        val result = lint(path) { }

        assertEquals(listOf("Screen 'home': shard missing 'type' field"), result.errors)
    }

    @Test
    fun `reports an error for duplicate shard ids in the same screen`() {
        val yaml =
            """
            version: 1
            screens:
              - id: home
                container:
                  type: vertical
                shards:
                  - type: CardView
                    id: dup
                  - type: BannerView
                    id: dup
            """.trimIndent()
        val path = yamlFile("duplicate_id.yaml", yaml)

        val result = lint(path) { }

        assertEquals(listOf("Duplicate shard id 'dup' in screen 'home'"), result.errors)
    }

    @Test
    fun `warns about a shard with no id`() {
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
        val path = yamlFile("no_id.yaml", yaml)

        val result = lint(path) { }

        assertTrue(result.errors.isEmpty())
        assertEquals(listOf("Shard of type 'BannerView' in screen 'home' has no id"), result.warnings)
    }

    @Test
    fun `lint defaults to printing progress lines to stdout`() {
        val path = yamlFile("default_output.yaml", "version: 1\nscreens: []")
        // No custom lambda — exercises the default `::println`.
        val result = lint(path)
        assertTrue(result.errors.isEmpty())
    }
}
