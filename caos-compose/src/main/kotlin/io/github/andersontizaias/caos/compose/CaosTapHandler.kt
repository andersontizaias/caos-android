package io.github.andersontizaias.caos.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Espelha `CaosTapAction` / `@Environment(\.caosTapAction)` do Caos iOS
 * (Sources/Caos/SwiftUI/CaosTapHandler.swift). O `id` vem do campo `id:` do shard no YAML.
 */
public typealias CaosTapAction = (id: String, context: Map<String, Any?>) -> Unit

private val NoOpCaosTapAction: CaosTapAction = { _, _ -> }

/** Espelha `EnvironmentValues.caosTapAction`, com o mesmo default no-op do Swift. */
public val LocalCaosTapAction: ProvidableCompositionLocal<CaosTapAction> =
    staticCompositionLocalOf { NoOpCaosTapAction }
