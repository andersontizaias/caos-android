package io.github.andersontizaias.caos.core

/** A container's padding. */
public data class CaosEdgeInsets(
    val top: Double = 0.0,
    val left: Double = 0.0,
    val bottom: Double = 0.0,
    val right: Double = 0.0,
) {
    public companion object {
        public val Zero: CaosEdgeInsets = CaosEdgeInsets()
    }
}

/**
 * A screen's container configuration.
 *
 * `type` stays a free-form string (`"vertical"` | `"horizontal"` | `"grid"`), not an enum, to
 * keep direct parity with the YAML schema.
 */
public data class CaosContainer(
    val type: String = "vertical",
    val spacing: Double = 0.0,
    val padding: CaosEdgeInsets = CaosEdgeInsets.Zero,
)

/** A resolved screen from the Caos schema. */
public data class CaosScreen(
    val id: String = "",
    val containerConfig: CaosContainer = CaosContainer(),
    val shardList: List<CaosShard> = emptyList(),
)
