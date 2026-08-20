package io.github.andersontizaias.caos.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * `CaosScreenView` provê este valor via [CompositionLocalProvider] para toda a árvore de shards
 * descendente — não há acesso a um [CaosStore] antes de um `CaosScreenView` ser composto.
 */
public val LocalCaosStore: ProvidableCompositionLocal<CaosStore> =
    staticCompositionLocalOf {
        error("LocalCaosStore não foi provido — CaosScreenView deve envolver a árvore de shards.")
    }
