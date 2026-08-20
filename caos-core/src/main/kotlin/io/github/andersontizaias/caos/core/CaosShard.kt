package io.github.andersontizaias.caos.core

/** A shard declared in the YAML. */
public data class CaosShard(
    val type: String,
    val id: String = "",
    val props: CaosProps = CaosProps(),
)
