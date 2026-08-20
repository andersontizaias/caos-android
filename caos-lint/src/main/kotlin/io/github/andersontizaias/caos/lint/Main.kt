package io.github.andersontizaias.caos.lint

import kotlin.system.exitProcess

/**
 * Entry point of the `caos-lint` CLI.
 *
 * Usage: `caos-lint <file.yaml>`
 */
public fun main(args: Array<String>) {
    exitProcess(runCaosLint(args))
}

/**
 * Runs the CLI and returns the exit code, without terminating the JVM process — separated from
 * [main] to be testable (`exitProcess` is only called in [main]).
 */
internal fun runCaosLint(
    args: Array<String>,
    output: (String) -> Unit = ::println,
): Int {
    if (args.isEmpty()) {
        output("Usage: caos-lint <yaml-file>")
        output("Example: caos-lint caos.yaml")
        return 1
    }

    val result = lint(args[0], output)

    result.warnings.forEach { output("⚠ $it") }
    result.errors.forEach { output("✗ $it") }

    return when {
        result.errors.isEmpty() && result.warnings.isEmpty() -> {
            output("✅ No issues found")
            0
        }

        result.errors.isEmpty() -> {
            output("✅ No errors (${result.warnings.size} warning(s))")
            0
        }

        else -> 1
    }
}
