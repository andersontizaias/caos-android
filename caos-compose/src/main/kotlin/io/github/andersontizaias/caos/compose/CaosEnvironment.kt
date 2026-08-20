package io.github.andersontizaias.caos.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * `CaosScreenView` provides this value via [CompositionLocalProvider] to the whole descendant
 * shard tree — there's no access to a [CaosStore] before a `CaosScreenView` is composed.
 */
public val LocalCaosStore: ProvidableCompositionLocal<CaosStore> =
    staticCompositionLocalOf {
        error("LocalCaosStore was not provided — CaosScreenView must wrap the shard tree.")
    }
