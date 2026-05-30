package com.owl.minerva.stocklab.enums

import java.util.Currency
import java.util.Locale

enum class AppCurrency(
    val displayName: String,
    val currency: Currency,
    val locale: Locale,
) {
    USD(
        displayName = "US Dollar",
        currency = Currency.getInstance("USD"),
        locale = Locale.US,
    ),
    IDR(
        displayName = "Indonesian Rupiah",
        currency = Currency.getInstance("IDR"),
        locale = Locale.Builder().setLanguage("id").setRegion("ID").build(),
    ),
    SGD(
        displayName = "Singapore Dollar",
        currency = Currency.getInstance("SGD"),
        locale = Locale.Builder().setLanguage("en").setRegion("SG").build(),
    ),
    JPY(
        displayName = "Japanese Yen",
        currency = Currency.getInstance("JPY"),
        locale = Locale.JAPAN,
    ),
}
