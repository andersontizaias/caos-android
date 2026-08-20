package io.github.andersontizaias.caos.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.andersontizaias.caos.compose.CaosScreenView
import io.github.andersontizaias.caos.compose.CaosStore

/**
 * Espelha o `@main struct MyApp: App` do Quick Start do README do
 * [Caos](https://github.com/andersontizaias/Caos) (Swift): registra o shard `BalanceCard` e a
 * chave de dado `user.balance`, depois renderiza `home.yaml` via [CaosScreenView].
 */
class MainActivity : ComponentActivity() {
    private val store =
        CaosStore().apply {
            register(type = "BalanceCard") { props -> BalanceCardView(props) }
            register(key = "user.balance") { UserSession.formattedBalance }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                CaosScreenView(
                    name = "home",
                    store = store,
                    onTap = { id, context -> Log.d(TAG, "Tapped shard: $id $context") },
                )
            }
        }
    }

    private companion object {
        const val TAG = "CaosSample"
    }
}
