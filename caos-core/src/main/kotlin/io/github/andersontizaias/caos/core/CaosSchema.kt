package io.github.andersontizaias.caos.core

/** Raiz do schema Caos v1. */
public data class CaosSchema(
    val version: Int,
    val screens: List<CaosScreen>,
)
