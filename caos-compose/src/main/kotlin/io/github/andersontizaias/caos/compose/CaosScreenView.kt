package io.github.andersontizaias.caos.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.andersontizaias.caos.core.CaosError
import io.github.andersontizaias.caos.core.CaosParseException
import io.github.andersontizaias.caos.core.CaosParser
import io.github.andersontizaias.caos.core.CaosSchema
import java.io.IOException

/**
 * Loads and renders a Caos screen from a YAML file in the app's assets. Four states: loading /
 * error / empty / rendered.
 *
 * Usage:
 * ```kotlin
 * CaosScreenView(name = "home", store = store) { id, context ->
 *     navigator.push(id)
 * }
 * ```
 *
 * `store` and `onTap` are explicit parameters because `CompositionLocalProvider` needs to wrap
 * its children — it can't be a trailing modifier — so the top-level composable already receives
 * both directly.
 */
@Composable
public fun CaosScreenView(
    name: String,
    store: CaosStore,
    modifier: Modifier = Modifier,
    onTap: CaosTapAction = { _, _ -> },
) {
    val context = LocalContext.current
    var schema by remember(name) { mutableStateOf<CaosSchema?>(null) }
    var parseError by remember(name) { mutableStateOf<CaosError?>(null) }

    LaunchedEffect(name) {
        schema = null
        parseError = null
        try {
            val content =
                context.assets
                    .open("$name.yaml")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            schema = CaosParser.parse(content)
        } catch (exception: CaosParseException) {
            parseError = exception.error
        } catch (
            @Suppress("SwallowedException") exception: IOException,
        ) {
            // A missing asset becomes a fixed, friendly message, not the platform IOException's
            // raw text.
            parseError = CaosError.InvalidYaml(line = 0, reason = "'$name.yaml' not found in assets")
        }
    }

    CompositionLocalProvider(LocalCaosStore provides store, LocalCaosTapAction provides onTap) {
        val currentSchema = schema
        val currentError = parseError
        when {
            currentSchema != null -> {
                val screen = currentSchema.screens.firstOrNull()
                if (screen != null) {
                    CaosContainerView(screen = screen, modifier = modifier)
                } else {
                    EmptyScreenMessage(name = name, modifier = modifier)
                }
            }

            currentError != null -> ErrorScreenMessage(error = currentError, modifier = modifier)

            else ->
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
        }
    }
}

@Composable
private fun EmptyScreenMessage(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "No screen found in YAML '$name'",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(SCREEN_MESSAGE_PADDING.dp),
    )
}

@Composable
private fun ErrorScreenMessage(
    error: CaosError,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(SCREEN_MESSAGE_PADDING.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "⚠", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = error.message,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

private const val SCREEN_MESSAGE_PADDING = 16
