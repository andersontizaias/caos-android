package io.github.andersontizaias.caos.lint

/** Resultado de uma checagem de lint — espelha `LintResult` do `caos-lint` Swift. */
public data class LintResult(
    val errors: MutableList<String> = mutableListOf(),
    val warnings: MutableList<String> = mutableListOf(),
)
