package io.github.andersontizaias.caos.lint

/** Result of a lint check. */
public data class LintResult(
    val errors: MutableList<String> = mutableListOf(),
    val warnings: MutableList<String> = mutableListOf(),
)
