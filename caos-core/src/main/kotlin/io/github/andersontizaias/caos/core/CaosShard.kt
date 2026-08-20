package io.github.andersontizaias.caos.core

/** Um shard declarado no YAML — espelha `CaosShard.swift`. */
public data class CaosShard(
    val type: String,
    val id: String = "",
    val props: CaosProps = CaosProps(),
)
