package io.github.andersontizaias.caos.compose

import androidx.compose.runtime.Composable
import io.github.andersontizaias.caos.core.CaosProps
import kotlinx.coroutines.flow.StateFlow

/**
 * Model central da arquitetura Caos (padrão MV, sem ViewModel). Gerencia o registro de shards,
 * providers síncronos e `StateFlow`s reativos.
 *
 * Em Compose, uma função `@Composable` já é a unidade de composição — todo shard se registra
 * como lambda via [register], sem precisar de um protocolo por tipo nem de reflection.
 */
public class CaosStore {
    private val shardRegistry = mutableMapOf<String, @Composable (CaosProps) -> Unit>()

    // internal + @PublishedApi: precisam ser visíveis do corpo inline de `resolve` (função
    // pública reified), inclusive por código de outro módulo que chame `resolve` — inline copia o
    // corpo da função pro call site, então membros `private` não seriam acessíveis ali.
    @PublishedApi
    internal val providers: MutableMap<String, () -> Any?> = mutableMapOf()

    @PublishedApi
    internal val flows: MutableMap<String, StateFlow<Any?>> = mutableMapOf()

    // MARK: - Registro de shards

    /** Registra o composable responsável por renderizar shards do tipo [type]. */
    public fun register(
        type: String,
        content: @Composable (props: CaosProps) -> Unit,
    ) {
        shardRegistry[type] = content
    }

    /** Renderiza o shard [type]. Se não houver registro, renderiza [CaosUnknownShardView]. */
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

    // MARK: - Registro de dados

    /** Registra um provider síncrono para uma chave. */
    public fun register(
        key: String,
        provider: () -> Any?,
    ) {
        providers[key] = provider
    }

    /** Registra um `StateFlow` reativo para uma chave. */
    public fun register(
        key: String,
        flow: StateFlow<Any?>,
    ) {
        flows[key] = flow
    }

    // MARK: - Resolução

    /**
     * Resolve o valor atual de uma chave de forma síncrona.
     *
     * Providers têm prioridade sobre flows: se uma chave tem provider registrado, o resultado
     * dele é retornado (convertido ou `null`), mesmo que um flow também exista para a mesma
     * chave.
     */
    public inline fun <reified T> resolve(key: String): T? {
        if (providers.containsKey(key)) {
            return providers.getValue(key).invoke() as? T
        }
        return flows[key]?.value as? T
    }

    /**
     * Retorna o [StateFlow] registrado para [key], tipado como `T`.
     *
     * O cast é necessário porque o registro interno é heterogêneo (`StateFlow<Any?>`) — o
     * chamador é responsável por pedir o mesmo tipo usado no `register(key, flow)` original.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T> flowFor(key: String): StateFlow<T?>? = flows[key] as? StateFlow<T?>
}
