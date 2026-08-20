package io.github.andersontizaias.caos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Renderizado quando um tipo de shard não está registrado no [CaosStore]. Em debug mostra aviso
 * visível; em release renderiza nada — espelha `CaosUnknownShardView` do Caos iOS
 * (`#if DEBUG` / `EmptyView()`).
 */
@Composable
public fun CaosUnknownShardView(
    type: String,
    modifier: Modifier = Modifier,
) {
    if (BuildConfig.DEBUG) {
        Text(
            text = "⚠ Shard '$type' não registrado",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(UNKNOWN_SHARD_CORNER_RADIUS.dp),
                    ).padding(UNKNOWN_SHARD_PADDING.dp),
        )
    }
}

private const val UNKNOWN_SHARD_CORNER_RADIUS = 6
private const val UNKNOWN_SHARD_PADDING = 8
