package io.github.andersontizaias.caos.sample

import java.text.NumberFormat
import java.util.Locale

/** Fonte de dados de exemplo, do Quick Start do README. */
internal object UserSession {
    val formattedBalance: String
        get() = currencyFormat.format(BALANCE)

    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    private const val BALANCE = 2540.75
}
