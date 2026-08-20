package io.github.andersontizaias.caos.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** The `id` comes from the shard's `id:` field in the YAML. */
public typealias CaosTapAction = (id: String, context: Map<String, Any?>) -> Unit

private val NoOpCaosTapAction: CaosTapAction = { _, _ -> }

/** Default no-op tap handler, until a [CaosScreenView] provides a real one. */
public val LocalCaosTapAction: ProvidableCompositionLocal<CaosTapAction> =
    staticCompositionLocalOf { NoOpCaosTapAction }
