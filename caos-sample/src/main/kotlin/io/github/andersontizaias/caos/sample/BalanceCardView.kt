package io.github.andersontizaias.caos.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.andersontizaias.caos.compose.LocalCaosStore
import io.github.andersontizaias.caos.compose.LocalCaosTapAction
import io.github.andersontizaias.caos.core.CaosProps

/**
 * Shard de exemplo, do Quick Start do README. Registrado no
 * [io.github.andersontizaias.caos.compose.CaosStore] via `register(type = "BalanceCard") { ... }`
 * (ver [MainActivity]).
 */
@Composable
internal fun BalanceCardView(props: CaosProps) {
    val store = LocalCaosStore.current
    val onTap = LocalCaosTapAction.current
    val balance = store.resolve<String>(props.string("dataKey") ?: "") ?: "--"

    Column(
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
        modifier =
            Modifier
                .background(Color.White, RoundedCornerShape(props.double("cornerRadius").dp))
                .clickable { onTap(props.string("id") ?: "", emptyMap()) }
                .padding(CARD_PADDING.dp),
    ) {
        Text(text = props.string("title") ?: "", style = MaterialTheme.typography.titleMedium)
        Text(text = balance, style = MaterialTheme.typography.headlineSmall)
    }
}

private const val CARD_SPACING = 4
private const val CARD_PADDING = 16
