package io.github.andersontizaias.caos.lint

import io.github.andersontizaias.caos.core.CaosError
import io.github.andersontizaias.caos.core.CaosParseException
import io.github.andersontizaias.caos.core.CaosParser
import java.io.File
import java.io.IOException

/**
 * Valida um arquivo de schema YAML v1 do Caos e reporta problemas.
 *
 * Espelha a função `lint` do `caos-lint` Swift (Sources/CaosLint/main.swift) — mesmas regras
 * (shard sem `type`, `id` duplicado, warning de shard sem `id`) e mesmas mensagens, pra manter a
 * documentação de instalação idêntica nos dois READMEs.
 *
 * [output] recebe cada linha de progresso (`✓ ...`) — por padrão imprime no stdout; testes podem
 * substituir por uma lambda que apenas coleta as linhas, sem precisar capturar `System.out`.
 */
@Suppress("NestedBlockDepth")
public fun lint(
    filePath: String,
    output: (String) -> Unit = ::println,
): LintResult {
    val result = LintResult()

    val content =
        try {
            File(filePath).readText(Charsets.UTF_8)
        } catch (
            @Suppress("SwallowedException") exception: IOException,
        ) {
            result.errors.add("Could not read file: $filePath")
            return result
        }

    val schema =
        try {
            CaosParser.parse(content)
        } catch (exception: CaosParseException) {
            result.errors.add(lintErrorMessage(exception.error))
            return result
        }

    output("✓ version: ${schema.version}")
    output("✓ screens: ${schema.screens.size} found")

    var totalShards = 0
    val shardIds = mutableSetOf<String>()

    for (screen in schema.screens) {
        for (shard in screen.shardList) {
            totalShards++

            if (shard.type.isEmpty()) {
                result.errors.add("Screen '${screen.id}': shard missing 'type' field")
            }

            if (shard.id.isNotEmpty()) {
                if (!shardIds.add(shard.id)) {
                    result.errors.add("Duplicate shard id '${shard.id}' in screen '${screen.id}'")
                }
            } else {
                result.warnings.add("Shard of type '${shard.type}' in screen '${screen.id}' has no id")
            }
        }
    }

    output("✓ shards: $totalShards found")
    return result
}

private fun lintErrorMessage(error: CaosError): String =
    when (error) {
        is CaosError.MissingVersion -> "Missing 'version' field — YAML v1 requires version: 1 as the first key"
        is CaosError.UnsupportedVersion -> "Unsupported schema version ${error.version} — expected 1"
        is CaosError.InvalidYaml -> "Parse error: ${error.message}"
    }
