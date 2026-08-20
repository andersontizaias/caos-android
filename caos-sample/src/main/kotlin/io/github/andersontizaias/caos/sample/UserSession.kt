package io.github.andersontizaias.caos.sample

import java.text.NumberFormat
import java.util.Locale

/** Example data source, from the README's Quick Start. */
internal object UserSession {
    val formattedBalance: String
        get() = currencyFormat.format(BALANCE)

    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private const val BALANCE = 2540.75
}
