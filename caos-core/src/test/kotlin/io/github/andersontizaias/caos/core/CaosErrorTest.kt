package io.github.andersontizaias.caos.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CaosErrorTest {
    @Test
    fun `missingVersion has expected message`() {
        assertEquals(
            "Caos: YAML must include 'version' as the first non-empty field.",
            CaosError.MissingVersion.message,
        )
    }

    @Test
    fun `invalidYaml has expected message`() {
        val error = CaosError.InvalidYaml(line = 5, reason = "unexpected token")
        assertEquals("Caos: Invalid YAML at line 5: unexpected token", error.message)
    }

    @Test
    fun `unsupportedVersion has expected message`() {
        assertEquals(
            "Caos: Unsupported schema version 42. Expected 1.",
            CaosError.UnsupportedVersion(42).message,
        )
    }

    @Test
    fun `CaosParseException carries the original error`() {
        val exception = CaosParseException(CaosError.MissingVersion)
        assertEquals(CaosError.MissingVersion, exception.error)
        assertEquals(CaosError.MissingVersion.message, exception.message)
    }
}
