package io.github.andersontizaias.caos.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.andersontizaias.caos.core.CaosScreen
import androidx.compose.foundation.lazy.grid.items as gridItems

/**
 * Renders a [CaosScreen] using native Compose containers. Picks
 * `LazyColumn`/`LazyRow`/`LazyVerticalGrid` based on `container.type` — all three are already
 * their own scroll container, no explicit wrapping `ScrollView` needed.
 */
@Composable
public fun CaosContainerView(
    screen: CaosScreen,
    modifier: Modifier = Modifier,
) {
    val store = LocalCaosStore.current
    val container = screen.containerConfig
    val contentPadding =
        PaddingValues(
            start = container.padding.left.dp,
            top = container.padding.top.dp,
            end = container.padding.right.dp,
            bottom = container.padding.bottom.dp,
        )
    val spacing = container.spacing.dp

    when (container.type) {
        "horizontal" ->
            LazyRow(
                modifier = modifier,
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                items(screen.shardList) { shard -> store.Render(type = shard.type, props = shard.props) }
            }

        "grid" ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_COLUMNS),
                modifier = modifier,
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                gridItems(screen.shardList) { shard -> store.Render(type = shard.type, props = shard.props) }
            }

        else ->
            LazyColumn(
                modifier = modifier,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                items(screen.shardList) { shard -> store.Render(type = shard.type, props = shard.props) }
            }
    }
}

private const val GRID_COLUMNS = 2
