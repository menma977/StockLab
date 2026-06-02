package com.owl.minerva.stocklab.enums

import java.util.*

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
    CNY(
        displayName = "Chinese Yuan",
        currency = Currency.getInstance("CNY"),
        locale = Locale.Builder().setLanguage("zh").setRegion("CN").build(),
    ),
    EUR(
        displayName = "Euro",
        currency = Currency.getInstance("EUR"),
        locale = Locale.Builder().setLanguage("de").setRegion("DE").build(),
    ),
    KRW(
        displayName = "South Korean Won",
        currency = Currency.getInstance("KRW"),
        locale = Locale.KOREA,
    ),
    INR(
        displayName = "Indian Rupee",
        currency = Currency.getInstance("INR"),
        locale = Locale.Builder().setLanguage("en").setRegion("IN").build(),
    ),
    VND(
        displayName = "Vietnamese Dong",
        currency = Currency.getInstance("VND"),
        locale = Locale.Builder().setLanguage("vi").setRegion("VN").build(),
    ),
    THB(
        displayName = "Thai Baht",
        currency = Currency.getInstance("THB"),
        locale = Locale.Builder().setLanguage("th").setRegion("TH").build(),
    ),
}
