package io.github.andersontizaias.caos.core

/** Raiz do schema Caos v1 — espelha `CaosSchema.swift`. */
public data class CaosSchema(
    val version: Int,
    val screens: List<CaosScreen>,
)
