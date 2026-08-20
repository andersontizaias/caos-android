package io.github.andersontizaias.caos.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CaosPropsTest {
    // MARK: - hexColor

    @Test
    fun `hexColor accepts six digit hex`() {
        assertEquals("#FF5500", CaosProps(mapOf("color" to "#FF5500")).hexColor("color"))
    }

    @Test
    fun `hexColor accepts eight digit hex with alpha`() {
        assertEquals("#80FF5500", CaosProps(mapOf("color" to "#80FF5500")).hexColor("color"))
    }

    @Test
    fun `hexColor accepts three digit shorthand`() {
        assertEquals("#F50", CaosProps(mapOf("color" to "#F50")).hexColor("color"))
    }

    @Test
    fun `hexColor rejects invalid hex`() {
        assertNull(CaosProps(mapOf("color" to "notacolor")).hexColor("color"))
    }

    // MARK: - double

    @Test
    fun `double converts from int`() {
        assertEquals(42.0, CaosProps(mapOf("val" to 42)).double("val"))
    }

    @Test
    fun `double converts from string`() {
        assertEquals(3.14, CaosProps(mapOf("val" to "3.14")).double("val"), 0.001)
    }

    @Test
    fun `double defaults to zero when missing`() {
        assertEquals(0.0, CaosProps(emptyMap()).double("missing"))
    }

    // MARK: - bool

    @Test
    fun `bool reads literal true`() {
        assertEquals(true, CaosProps(mapOf("flag" to true)).bool("flag"))
    }

    @Test
    fun `bool reads literal false`() {
        assertEquals(false, CaosProps(mapOf("flag" to false)).bool("flag"))
    }

    @Test
    fun `bool reads string true`() {
        assertEquals(true, CaosProps(mapOf("flag" to "true")).bool("flag"))
    }

    @Test
    fun `bool reads string false`() {
        assertEquals(false, CaosProps(mapOf("flag" to "false")).bool("flag"))
    }

    @Test
    fun `bool is null when missing`() {
        assertNull(CaosProps(emptyMap()).bool("missing"))
    }

    // MARK: - string / nested / array

    @Test
    fun `string is null for missing key`() {
        assertNull(CaosProps(mapOf("a" to "1")).string("b"))
    }

    @Test
    fun `nested resolves a sub-map`() {
        val props = CaosProps(mapOf("sub" to mapOf("key" to "value")))
        assertEquals("value", props.nested("sub")?.string("key"))
    }

    @Test
    fun `nested is null when missing`() {
        assertNull(CaosProps(emptyMap()).nested("missing"))
    }

    @Test
    fun `array resolves a list of props`() {
        val props = CaosProps(mapOf("items" to listOf(mapOf("id" to "a"), mapOf("id" to "b"))))
        assertEquals(2, props.array("items")?.size)
    }

    @Test
    fun `array is null when missing`() {
        assertNull(CaosProps(emptyMap()).array("missing"))
    }
}
