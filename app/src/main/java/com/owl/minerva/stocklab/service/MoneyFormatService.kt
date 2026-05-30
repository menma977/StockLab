package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.enums.AppCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

object MoneyFormatService {
    fun format(value: Double, currency: AppCurrency): String {
        return NumberFormat.getCurrencyInstance(currency.locale).apply {
            this.currency = currency.currency
        }.format(value)
    }

    fun format(value: Long, currency: AppCurrency): String = format(value.toDouble(), currency)

    fun formatCompact(value: Long, currency: AppCurrency): String {
        val absValue = kotlin.math.abs(value)
        val sign = if (value < 0) "-" else ""
        val divisor = when {
            absValue >= 1_000_000_000L -> 1_000_000_000L to "B"
            absValue >= 1_000_000L -> 1_000_000L to "M"
            absValue >= 1_000L -> 1_000L to "K"
            else -> return format(value, currency)
        }
        val scaled = BigDecimal(absValue)
            .divide(BigDecimal(divisor.first), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

        return "$sign${currency.currency.getSymbol(currency.locale)}$scaled${divisor.second}"
    }
}
