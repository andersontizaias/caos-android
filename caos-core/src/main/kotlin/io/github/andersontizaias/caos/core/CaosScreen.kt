package io.github.andersontizaias.caos.core

/** Espelha `CaosEdgeInsets` (Sources/Caos/Schema/CaosScreen.swift). */
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
 * Configuração de container de uma tela — espelha `CaosContainer`.
 *
 * `type` permanece string livre (`"vertical"` | `"horizontal"` | `"grid"`), não enum, para manter
 * paridade de schema com o YAML e com o Swift (que também usa `String`).
 */
public data class CaosContainer(
    val type: String = "vertical",
    val spacing: Double = 0.0,
    val padding: CaosEdgeInsets = CaosEdgeInsets.Zero,
)

/**
 * Uma tela resolvida do schema Caos — espelha `CaosScreen`.
 *
 * No Swift, `CaosScreen` é uma classe mutável preenchida incrementalmente durante o parse; em
 * Kotlin não há necessidade dessa mutabilidade — o parser constrói a instância imutável direto.
 */
public data class CaosScreen(
    val id: String = "",
    val containerConfig: CaosContainer = CaosContainer(),
    val shardList: List<CaosShard> = emptyList(),
)
