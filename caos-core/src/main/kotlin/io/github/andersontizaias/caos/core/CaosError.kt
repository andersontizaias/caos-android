package io.github.andersontizaias.caos.core

/** Erros de parsing do schema Caos. */
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
 * Exceção lançada por [CaosParser.parse] quando o YAML é inválido.
 *
 * `CaosError` é um tipo de dado puro (sem herdar de `Throwable`), então o parser lança esta
 * exceção carregando-o.
 */
public class CaosParseException(
    public val error: CaosError,
) : Exception(error.message)
