package io.github.andersontizaias.caos.core

/** Padding de um container. */
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
 * Configuração de container de uma tela.
 *
 * `type` permanece string livre (`"vertical"` | `"horizontal"` | `"grid"`), não enum, para manter
 * paridade direta com o schema YAML.
 */
public data class CaosContainer(
    val type: String = "vertical",
    val spacing: Double = 0.0,
    val padding: CaosEdgeInsets = CaosEdgeInsets.Zero,
)

/** Uma tela resolvida do schema Caos. */
public data class CaosScreen(
    val id: String = "",
    val containerConfig: CaosContainer = CaosContainer(),
    val shardList: List<CaosShard> = emptyList(),
)
