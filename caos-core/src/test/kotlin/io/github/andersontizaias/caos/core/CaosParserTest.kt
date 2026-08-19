package io.github.andersontizaias.caos.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CaosParserTest {
    private fun fixture(name: String): String {
        val stream =
            javaClass.getResourceAsStream("/fixtures/$name.yaml")
                ?: error("Fixture not found: $name.yaml")
        return stream.bufferedReader(Charsets.UTF_8).readText()
    }

    // MARK: - Happy path (valid_v1.yaml)

    @Test
    fun `parses correct screen count`() {
        val schema = CaosParser.parse(fixture("valid_v1"))
        assertEquals(1, schema.version)
        assertEquals(2, schema.screens.size)
    }

    @Test
    fun `first screen has correct id`() {
        val schema = CaosParser.parse(fixture("valid_v1"))
        assertEquals("home", schema.screens[0].id)
    }

    @Test
    fun `first screen has correct container type`() {
        val schema = CaosParser.parse(fixture("valid_v1"))
        assertEquals("vertical", schema.screens[0].containerConfig.type)
    }

    @Test
    fun `first screen has correct container spacing`() {
        val schema = CaosParser.parse(fixture("valid_v1"))
        assertEquals(16.0, schema.screens[0].containerConfig.spacing)
    }

    @Test
    fun `first screen has correct shard count`() {
        val schema = CaosParser.parse(fixture("valid_v1"))
        assertEquals(2, schema.screens[0].shardList.size)
    }

    @Test
    fun `first shard has correct type and id`() {
        val shard = CaosParser.parse(fixture("valid_v1")).screens[0].shardList[0]
        assertEquals("CardView", shard.type)
        assertEquals("card_balance", shard.id)
    }

    @Test
    fun `first shard props expose string`() {
        val props =
            CaosParser
                .parse(fixture("valid_v1"))
                .screens[0]
                .shardList[0]
                .props
        assertEquals("Saldo disponível", props.string("title"))
    }

    @Test
    fun `first shard props expose double`() {
        val props =
            CaosParser
                .parse(fixture("valid_v1"))
                .screens[0]
                .shardList[0]
                .props
        assertEquals(12.0, props.double("cornerRadius"))
    }

    @Test
    fun `second screen is horizontal`() {
        val schema = CaosParser.parse(fixture("valid_v1"))
        assertEquals("horizontal", schema.screens[1].containerConfig.type)
        assertEquals(8.0, schema.screens[1].containerConfig.spacing)
    }

    // MARK: - Error cases

    @Test
    fun `missing version throws MissingVersion`() {
        val exception =
            assertFailsWith<CaosParseException> {
                CaosParser.parse(fixture("invalid_no_version"))
            }
        assertEquals(CaosError.MissingVersion, exception.error)
    }

    @Test
    fun `unsupported version throws UnsupportedVersion`() {
        val exception =
            assertFailsWith<CaosParseException> {
                CaosParser.parse("version: 99\nscreens: []")
            }
        assertEquals(CaosError.UnsupportedVersion(99), exception.error)
    }

    // MARK: - Edge cases (edge_cases.yaml)

    @Test
    fun `edge cases empty shards`() {
        val schema = CaosParser.parse(fixture("edge_cases"))
        assertEquals(0, schema.screens[0].shardList.size)
    }

    @Test
    fun `edge cases all prop types`() {
        val props =
            CaosParser
                .parse(fixture("edge_cases"))
                .screens[1]
                .shardList[0]
                .props
        assertEquals("hello world", props.string("stringProp"))
        assertEquals(42, props.int("intProp"))
        assertEquals(3.14, props.double("doubleProp"), 0.001)
        assertEquals(true, props.bool("boolPropTrue"))
        assertEquals(false, props.bool("boolPropFalse"))
        assertEquals("#FF5500", props.hexColor("colorProp"))
        assertEquals("#80FF5500", props.hexColor("colorWithAlpha"))
    }

    // MARK: - YAML syntax edge cases

    @Test
    fun `inline comments are stripped`() {
        val yaml = "version: 1 # required field\nscreens: [] # empty"
        assertEquals(1, CaosParser.parse(yaml).version)
    }

    @Test
    fun `top-level comment lines are skipped`() {
        val yaml = "# top comment\nversion: 1\n# mid comment\nscreens: []"
        assertEquals(1, CaosParser.parse(yaml).version)
    }

    @Test
    fun `spacing accepts decimal values`() {
        val yaml =
            """
            version: 1
            screens:
              - id: s
                container:
                  type: vertical
                  spacing: 16.5
                shards: []
            """.trimIndent()
        val schema = CaosParser.parse(yaml)
        assertEquals(16.5, schema.screens[0].containerConfig.spacing, 0.001)
    }

    @Test
    fun `padding accepts partial keys, rest default to zero`() {
        val yaml =
            """
            version: 1
            screens:
              - id: s
                container:
                  type: vertical
                  padding:
                    top: 10
                shards: []
            """.trimIndent()
        val schema = CaosParser.parse(yaml)
        assertEquals(
            10.0,
            schema.screens[0]
                .containerConfig.padding.top,
        )
        assertEquals(
            0.0,
            schema.screens[0]
                .containerConfig.padding.left,
        )
    }

    @Test
    fun `orphan key with no following content is ignored`() {
        val yaml = "version: 1\norphan:\nscreens: []"
        assertEquals(1, CaosParser.parse(yaml).version)
    }

    @Test
    fun `deeper indent inside mapping is skipped as malformed`() {
        val yaml = "version: 1\n    extra: malformed\nscreens: []"
        assertEquals(1, CaosParser.parse(yaml).version)
    }

    @Test
    fun `sequence with nested mapping props`() {
        val yaml =
            """
            version: 1
            screens:
              - id: home
                container:
                  type: vertical
                shards:
                  - type: CardView
                    id: c1
                    props:
                      label: Hello
                      count: 3
            """.trimIndent()
        val schema = CaosParser.parse(yaml)
        assertEquals(1, schema.screens[0].shardList.size)
        assertEquals(
            "Hello",
            schema.screens[0]
                .shardList[0]
                .props
                .string("label"),
        )
        assertEquals(
            3,
            schema.screens[0]
                .shardList[0]
                .props
                .int("count"),
        )
    }

    @Test
    fun `deeper indent inside sequence is skipped as malformed`() {
        val yaml =
            "version: 1\nscreens:\n  - id: first\n   malformed_deeper: val\n  - id: second\n" +
                "    container:\n      type: vertical\n    shards: []"
        val schema = CaosParser.parse(yaml)
        assertEquals(2, schema.screens.size)
    }

    @Test
    fun `blank line before first sequence item`() {
        val yaml = "version: 1\nscreens:\n\n  - id: first\n    container:\n      type: vertical\n    shards: []\n"
        val schema = CaosParser.parse(yaml)
        assertEquals(1, schema.screens.size)
        assertEquals("first", schema.screens[0].id)
    }

    @Test
    fun `trailing blank lines after orphan key`() {
        val yaml = "version: 1\nscreens: []\norphan:\n\n"
        assertEquals(1, CaosParser.parse(yaml).version)
    }

    @Test
    fun `sequence item with empty inline value continues on next lines`() {
        val yaml = "version: 1\nscreens:\n  - id:\n\n    container:\n      type: vertical\n    shards: []\n"
        val schema = CaosParser.parse(yaml)
        assertEquals(1, schema.screens.size)
        assertEquals("vertical", schema.screens[0].containerConfig.type)
    }
}
