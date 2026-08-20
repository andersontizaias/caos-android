package io.github.andersontizaias.caos.compose

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.andersontizaias.caos.core.CaosError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaosScreenViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders a registered shard from a valid YAML asset`() {
        val store =
            CaosStore().apply {
                register(type = "BalanceCard") { props -> Text(props.string("title") ?: "") }
            }

        composeTestRule.setContent {
            CaosScreenView(name = "home", store = store)
        }

        composeTestRule.onNodeWithText("Available balance").assertExists()
    }

    @Test
    fun `shows an empty state message when the YAML has no screens`() {
        composeTestRule.setContent {
            CaosScreenView(name = "empty_screens", store = CaosStore())
        }

        composeTestRule.onNodeWithText("No screen found in YAML 'empty_screens'").assertExists()
    }

    @Test
    fun `shows an error state message when the YAML asset is missing`() {
        composeTestRule.setContent {
            CaosScreenView(name = "does_not_exist", store = CaosStore())
        }

        val expectedMessage =
            CaosError.InvalidYaml(line = 0, reason = "'does_not_exist.yaml' not found in assets")
        composeTestRule.onNodeWithText(expectedMessage.message).assertExists()
    }

    @Test
    fun `invokes onTap with the shard id when a shard dispatches a tap`() {
        var tappedId = ""
        val store =
            CaosStore().apply {
                register(type = "BalanceCard") { _ ->
                    val onTap = LocalCaosTapAction.current
                    Text(
                        text = "tap me",
                        modifier = Modifier.clickable { onTap("card_balance", emptyMap()) },
                    )
                }
            }

        composeTestRule.setContent {
            CaosScreenView(name = "home", store = store, onTap = { id, _ -> tappedId = id })
        }

        composeTestRule.onNodeWithText("tap me").performClick()
        assertEquals("card_balance", tappedId)
    }
}
