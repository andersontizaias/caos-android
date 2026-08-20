package io.github.andersontizaias.caos.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** O `id` vem do campo `id:` do shard no YAML. */
public typealias CaosTapAction = (id: String, context: Map<String, Any?>) -> Unit

private val NoOpCaosTapAction: CaosTapAction = { _, _ -> }

/** Handler de tap padrão, no-op, até um [CaosScreenView] prover um real. */
public val LocalCaosTapAction: ProvidableCompositionLocal<CaosTapAction> =
    staticCompositionLocalOf { NoOpCaosTapAction }
