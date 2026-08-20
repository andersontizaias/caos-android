package io.github.andersontizaias.caos.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Aplica um efeito shimmer de carregamento. Usa apenas o sistema de animação do Compose — sem
 * `Canvas` customizado além do `drawWithContent` necessário pro overlay.
 *
 * Uso:
 * ```kotlin
 * Text("Carregando...", modifier = Modifier.caosShimmer(isActive = isLoading))
 * ```
 */
public fun Modifier.caosShimmer(isActive: Boolean = true): Modifier =
    composed {
        if (!isActive) return@composed this

        val transition = rememberInfiniteTransition(label = "caosShimmer")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "caosShimmerPhase",
        )

        this.drawWithContent {
            drawContent()
            val bandWidth = size.width * BAND_WIDTH_MULTIPLIER
            val offsetX = phase * bandWidth - size.width
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors = ShimmerColors,
                        start = Offset(x = offsetX, y = 0f),
                        end = Offset(x = offsetX + bandWidth, y = 0f),
                    ),
            )
        }
    }

private val ShimmerColors =
    listOf(
        Color(red = 0.88f, green = 0.88f, blue = 0.88f),
        Color(red = 0.78f, green = 0.78f, blue = 0.78f),
        Color(red = 0.88f, green = 0.88f, blue = 0.88f),
    )

private const val SHIMMER_DURATION_MS = 1400
private const val BAND_WIDTH_MULTIPLIER = 3f
