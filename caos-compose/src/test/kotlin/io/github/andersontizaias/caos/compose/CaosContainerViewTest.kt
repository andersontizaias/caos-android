package io.github.andersontizaias.caos.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.andersontizaias.caos.core.CaosContainer
import io.github.andersontizaias.caos.core.CaosEdgeInsets
import io.github.andersontizaias.caos.core.CaosProps
import io.github.andersontizaias.caos.core.CaosScreen
import io.github.andersontizaias.caos.core.CaosShard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaosContainerViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun screen(
        type: String,
        shards: List<CaosShard> = emptyList(),
    ): CaosScreen =
        CaosScreen(
            id = "test_$type",
            containerConfig = CaosContainer(type = type, spacing = 8.0),
            shardList = shards,
        )

    @Test
    fun `renders a vertical container without crashing`() {
        composeTestRule.setContent {
            withStore(CaosStore()) { CaosContainerView(screen = screen("vertical")) }
        }
    }

    @Test
    fun `renders a horizontal container without crashing`() {
        composeTestRule.setContent {
            withStore(CaosStore()) { CaosContainerView(screen = screen("horizontal")) }
        }
    }

    @Test
    fun `renders a grid container without crashing`() {
        composeTestRule.setContent {
            withStore(CaosStore()) { CaosContainerView(screen = screen("grid")) }
        }
    }

    @Test
    fun `renders registered and unregistered shards`() {
        val store =
            CaosStore().apply {
                register(type = "Card") { props -> Text(props.string("label") ?: "") }
            }
        val shards =
            listOf(
                CaosShard(type = "Card", id = "c1", props = CaosProps(mapOf("label" to "Hi"))),
                CaosShard(type = "Unknown", id = "c2"),
            )

        composeTestRule.setContent {
            withStore(store) { CaosContainerView(screen = screen("vertical", shards)) }
        }

        composeTestRule.onNodeWithText("Hi").assertExists()
        // O aviso de shard desconhecido só existe no build type debug — espelha o `#if DEBUG` do
        // Swift; ver CaosUnknownShardViewTest para a asserção completa desse comportamento.
        if (BuildConfig.DEBUG) {
            composeTestRule.onNodeWithText("⚠ Shard 'Unknown' não registrado").assertExists()
        }
    }

    @Test
    fun `applies padding without crashing`() {
        val screenWithPadding =
            CaosScreen(
                id = "padded",
                containerConfig =
                    CaosContainer(
                        type = "vertical",
                        spacing = 8.0,
                        padding = CaosEdgeInsets(top = 16.0, left = 16.0, bottom = 16.0, right = 16.0),
                    ),
            )

        composeTestRule.setContent {
            withStore(CaosStore()) { CaosContainerView(screen = screenWithPadding) }
        }
    }

    @Composable
    private fun withStore(
        store: CaosStore,
        content: @Composable () -> Unit,
    ) {
        CompositionLocalProvider(LocalCaosStore provides store, content = content)
    }
}
