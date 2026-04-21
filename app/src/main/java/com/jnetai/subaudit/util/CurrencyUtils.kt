package com.jnetai.subaudit.util

object CurrencyUtils {
    data class CurrencyInfo(val code: String, val symbol: String, val name: String)

    val CURRENCIES = listOf(
        CurrencyInfo("GBP", "£", "British Pound"),
        CurrencyInfo("USD", "$", "US Dollar"),
        CurrencyInfo("EUR", "€", "Euro")
    )

    fun getSymbol(code: String): String {
        return CURRENCIES.find { it.code == code }?.symbol ?: "£"
    }

    fun formatAmount(amount: Double, currencyCode: String): String {
        val symbol = getSymbol(currencyCode)
        return "$symbol${String.format("%.2f", amount)}"
    }
}