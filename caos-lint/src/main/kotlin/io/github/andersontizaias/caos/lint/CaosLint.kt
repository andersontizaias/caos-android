package io.github.andersontizaias.caos.lint

import io.github.andersontizaias.caos.core.CaosError
import io.github.andersontizaias.caos.core.CaosParseException
import io.github.andersontizaias.caos.core.CaosParser
import java.io.File
import java.io.IOException

/**
 * Validates a Caos YAML v1 schema file and reports issues — shard missing `type`, duplicate
 * `id`, warning for a shard with no `id`.
 *
 * [output] receives each progress line (`✓ ...`) — prints to stdout by default; tests can
 * substitute a lambda that just collects the lines, without capturing `System.out`.
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
