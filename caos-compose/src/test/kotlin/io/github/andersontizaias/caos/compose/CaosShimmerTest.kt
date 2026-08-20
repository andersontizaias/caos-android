package io.github.andersontizaias.caos.compose

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaosShimmerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `active shimmer does not crash`() {
        composeTestRule.setContent {
            Text("Carregando...", modifier = Modifier.caosShimmer(isActive = true))
        }
    }

    @Test
    fun `inactive shimmer does not crash and skips the animated modifier`() {
        composeTestRule.setContent {
            Text("Carregando...", modifier = Modifier.caosShimmer(isActive = false))
        }
    }

    @Test
    fun `defaults to active`() {
        composeTestRule.setContent {
            Text("Carregando...", modifier = Modifier.caosShimmer())
        }
    }
}
