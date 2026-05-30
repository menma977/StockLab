package com.owl.minerva.stocklab.service

object AmountFormatService {
    fun format(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
}
