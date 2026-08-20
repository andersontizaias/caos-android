package io.github.andersontizaias.caos.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CaosYamlParserTest {
    // MARK: - parse()

    @Test
    fun `parse returns a mapping with typed values`() {
        val result = CaosYamlParser.parse("version: 1\nscreens: []")

        @Suppress("UNCHECKED_CAST")
        val map = result as Map<String, Any?>
        assertEquals(1, map["version"])
    }

    // MARK: - leadingSpaces

    @Test
    fun `leadingSpaces is zero for unindented line`() {
        assertEquals(0, CaosYamlParser.leadingSpaces("hello"))
    }

    @Test
    fun `leadingSpaces counts indentation`() {
        assertEquals(3, CaosYamlParser.leadingSpaces("   hello"))
    }

    // MARK: - parseScalar

    @Test
    fun `parseScalar handles empty sequence shorthand`() {
        assertEquals(emptyList<Map<String, Any?>>(), CaosYamlParser.parseScalar("[]"))
    }

    @Test
    fun `parseScalar handles empty mapping shorthand`() {
        assertEquals(emptyMap<String, Any?>(), CaosYamlParser.parseScalar("{}"))
    }

    @Test
    fun `parseScalar handles null literal`() {
        assertNull(CaosYamlParser.parseScalar("null"))
    }

    @Test
    fun `parseScalar handles tilde as null`() {
        assertNull(CaosYamlParser.parseScalar("~"))
    }

    @Test
    fun `parseScalar strips double quotes`() {
        assertEquals("hello world", CaosYamlParser.parseScalar("\"hello world\""))
    }

    @Test
    fun `parseScalar strips single quotes`() {
        assertEquals("hello world", CaosYamlParser.parseScalar("'hello world'"))
    }

    @Test
    fun `parseScalar parses doubles`() {
        assertEquals(3.14, CaosYamlParser.parseScalar("3.14") as Double, 0.001)
    }

    // MARK: - findKeyColon (via parseMapping, quoted keys)

    @Test
    fun `quoted key with internal colon is not split early`() {
        val lines = listOf("'key:colon': value")
        val cursor = YamlCursor(0)

        @Suppress("UNCHECKED_CAST")
        val result = CaosYamlParser.parseMapping(lines, cursor, 0) as Map<String, Any?>
        // The key is stored with its surrounding quotes, since the parser doesn't strip them.
        assertEquals("value", result["'key:colon'"])
    }

    @Test
    fun `double-quoted key with internal colon is not split early`() {
        val lines = listOf("\"key:double\": value")
        val cursor = YamlCursor(0)

        @Suppress("UNCHECKED_CAST")
        val result = CaosYamlParser.parseMapping(lines, cursor, 0) as Map<String, Any?>
        assertEquals("value", result["\"key:double\""])
    }

    @Test
    fun `line without a valid key colon is skipped`() {
        val lines = listOf("no colon here", "valid: yes")
        val cursor = YamlCursor(0)

        @Suppress("UNCHECKED_CAST")
        val result = CaosYamlParser.parseMapping(lines, cursor, 0) as Map<String, Any?>
        assertEquals("yes", result["valid"])
    }

    // MARK: - stripInlineComment (via parse, quoted values containing '#')

    @Test
    fun `double-quoted value containing hash is not treated as comment`() {
        val yaml = "version: 1\nscreens: []\ntitle: \"value # not comment\" # real comment"

        @Suppress("UNCHECKED_CAST")
        val result = CaosYamlParser.parse(yaml) as Map<String, Any?>
        assertEquals("value # not comment", result["title"])
    }

    @Test
    fun `single-quoted value containing hash is not treated as comment`() {
        val yaml = "version: 1\nscreens: []\ntitle: 'value # not a comment'"

        @Suppress("UNCHECKED_CAST")
        val result = CaosYamlParser.parse(yaml) as Map<String, Any?>
        assertEquals("value # not a comment", result["title"])
    }
}
