package io.github.andersontizaias.caos.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaosEnvironmentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `LocalCaosStore resolves the provided store to descendants`() {
        val store =
            CaosStore().apply {
                register(key = "greeting") { "hi" }
            }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalCaosStore provides store) {
                Text(LocalCaosStore.current.resolve<String>("greeting") ?: "missing")
            }
        }

        composeTestRule.onNodeWithText("hi").assertExists()
    }

    @Test(expected = IllegalStateException::class)
    fun `LocalCaosStore has no default value`() {
        composeTestRule.setContent {
            Text(LocalCaosStore.current.resolve<String>("anything") ?: "missing")
        }
    }
}
