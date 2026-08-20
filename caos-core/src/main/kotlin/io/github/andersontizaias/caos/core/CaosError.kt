package io.github.andersontizaias.caos.core

/** Errors from parsing the Caos schema. */
public sealed class CaosError {
    public data object MissingVersion : CaosError()

    public data class InvalidYaml(
        val line: Int,
        val reason: String,
    ) : CaosError()

    public data class UnsupportedVersion(
        val version: Int,
    ) : CaosError()

    public val message: String
        get() =
            when (this) {
                is MissingVersion -> "Caos: YAML must include 'version' as the first non-empty field."
                is InvalidYaml -> "Caos: Invalid YAML at line $line: $reason"
                is UnsupportedVersion -> "Caos: Unsupported schema version $version. Expected 1."
            }
}

/**
 * Exception thrown by [CaosParser.parse] when the YAML is invalid.
 *
 * `CaosError` is a plain data type (doesn't extend `Throwable`), so the parser throws this
 * exception carrying it.
 */
public class CaosParseException(
    public val error: CaosError,
) : Exception(error.message)
