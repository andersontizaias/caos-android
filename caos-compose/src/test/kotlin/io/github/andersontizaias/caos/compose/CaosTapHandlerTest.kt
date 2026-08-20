package io.github.andersontizaias.caos.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaosTapHandlerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `default tap action does nothing`() {
        var called = false
        composeTestRule.setContent {
            LocalCaosTapAction.current("anyId", emptyMap())
            called = true
            Text("ok")
        }
        assertEquals(true, called)
    }

    @Test
    fun `provided tap action is visible to descendants`() {
        var receivedId = ""
        composeTestRule.setContent {
            CompositionLocalProvider(LocalCaosTapAction provides { id, _ -> receivedId = id }) {
                LocalCaosTapAction.current("shard_1", emptyMap())
                Text("ok")
            }
        }
        assertEquals("shard_1", receivedId)
    }
}
