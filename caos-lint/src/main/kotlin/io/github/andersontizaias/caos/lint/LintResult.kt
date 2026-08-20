package io.github.andersontizaias.caos.lint

/** Resultado de uma checagem de lint. */
public data class LintResult(
    val errors: MutableList<String> = mutableListOf(),
    val warnings: MutableList<String> = mutableListOf(),
)
