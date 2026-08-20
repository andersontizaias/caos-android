package io.github.andersontizaias.caos.compose

import androidx.compose.runtime.Composable
import io.github.andersontizaias.caos.core.CaosProps
import kotlinx.coroutines.flow.StateFlow

/**
 * Central model of the Caos architecture (MV pattern, no ViewModel). Manages shard registration,
 * synchronous providers, and reactive `StateFlow`s.
 *
 * In Compose, a `@Composable` function is already the unit of composition — every shard
 * registers as a lambda via [register], with no need for a per-type protocol or reflection.
 */
public class CaosStore {
    private val shardRegistry = mutableMapOf<String, @Composable (CaosProps) -> Unit>()

    // internal + @PublishedApi: need to be visible from `resolve`'s inline body (a public
    // reified function), including from code in another module that calls `resolve` — inline
    // copies the function body to the call site, so `private` members wouldn't be accessible
    // there.
    @PublishedApi
    internal val providers: MutableMap<String, () -> Any?> = mutableMapOf()

    @PublishedApi
    internal val flows: MutableMap<String, StateFlow<Any?>> = mutableMapOf()

    // MARK: - Shard registration

    /** Registers the composable responsible for rendering shards of type [type]. */
    public fun register(
        type: String,
        content: @Composable (props: CaosProps) -> Unit,
    ) {
        shardRegistry[type] = content
    }

    /** Renders the shard [type]. If unregistered, renders [CaosUnknownShardView]. */
    @Composable
    public fun Render(
        type: String,
        props: CaosProps,
    ) {
        val content = shardRegistry[type]
        if (content != null) {
            content(props)
        } else {
            CaosUnknownShardView(type = type)
        }
    }

    // MARK: - Data registration

    /** Registers a synchronous provider for a key. */
    public fun register(
        key: String,
        provider: () -> Any?,
    ) {
        providers[key] = provider
    }

    /** Registers a reactive `StateFlow` for a key. */
    public fun register(
        key: String,
        flow: StateFlow<Any?>,
    ) {
        flows[key] = flow
    }

    // MARK: - Resolution

    /**
     * Resolves the current value of a key synchronously.
     *
     * Providers take priority over flows: if a key has a registered provider, its result is
     * returned (converted, or `null`), even if a flow also exists for the same key.
     */
    public inline fun <reified T> resolve(key: String): T? {
        if (providers.containsKey(key)) {
            return providers.getValue(key).invoke() as? T
        }
        return flows[key]?.value as? T
    }

    /**
     * Returns the [StateFlow] registered for [key], typed as `T`.
     *
     * The cast is necessary because the internal registry is heterogeneous (`StateFlow<Any?>`)
     * — the caller is responsible for requesting the same type used in the original
     * `register(key, flow)` call.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T> flowFor(key: String): StateFlow<T?>? = flows[key] as? StateFlow<T?>
}
