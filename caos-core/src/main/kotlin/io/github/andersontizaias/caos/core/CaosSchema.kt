package io.github.andersontizaias.caos.core

/** Root of the Caos v1 schema. */
public data class CaosSchema(
    val version: Int,
    val screens: List<CaosScreen>,
)
