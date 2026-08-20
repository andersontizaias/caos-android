package io.github.andersontizaias.caos.compose

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaosUnknownShardViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows a visible warning for unregistered shard types in debug builds`() {
        // This behavior only exists in the debug build type — skip (don't fail) when run
        // against the release variant, where BuildConfig.DEBUG = false.
        assumeTrue(BuildConfig.DEBUG)

        composeTestRule.setContent {
            CaosUnknownShardView(type = "Missing")
        }

        composeTestRule.onNodeWithText("⚠ Shard 'Missing' not registered").assertExists()
    }
}
