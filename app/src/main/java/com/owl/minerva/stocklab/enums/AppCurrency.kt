package com.owl.minerva.stocklab.enums

import androidx.annotation.StringRes
import com.owl.minerva.stocklab.R
import java.util.*

enum class AppCurrency(
    @get:StringRes val displayNameRes: Int,
    val currency: Currency,
    val locale: Locale,
) {
    USD(
        displayNameRes = R.string.currency_us_dollar,
        currency = Currency.getInstance("USD"),
        locale = Locale.US,
    ),
    IDR(
        displayNameRes = R.string.currency_indonesian_rupiah,
        currency = Currency.getInstance("IDR"),
        locale = Locale.Builder().setLanguage("id").setRegion("ID").build(),
    ),
    SGD(
        displayNameRes = R.string.currency_singapore_dollar,
        currency = Currency.getInstance("SGD"),
        locale = Locale.Builder().setLanguage("en").setRegion("SG").build(),
    ),
    JPY(
        displayNameRes = R.string.currency_japanese_yen,
        currency = Currency.getInstance("JPY"),
        locale = Locale.JAPAN,
    ),
    CNY(
        displayNameRes = R.string.currency_chinese_yuan,
        currency = Currency.getInstance("CNY"),
        locale = Locale.Builder().setLanguage("zh").setRegion("CN").build(),
    ),
    EUR(
        displayNameRes = R.string.currency_euro,
        currency = Currency.getInstance("EUR"),
        locale = Locale.Builder().setLanguage("de").setRegion("DE").build(),
    ),
    KRW(
        displayNameRes = R.string.currency_south_korean_won,
        currency = Currency.getInstance("KRW"),
        locale = Locale.KOREA,
    ),
    INR(
        displayNameRes = R.string.currency_indian_rupee,
        currency = Currency.getInstance("INR"),
        locale = Locale.Builder().setLanguage("en").setRegion("IN").build(),
    ),
    VND(
        displayNameRes = R.string.currency_vietnamese_dong,
        currency = Currency.getInstance("VND"),
        locale = Locale.Builder().setLanguage("vi").setRegion("VN").build(),
    ),
    THB(
        displayNameRes = R.string.currency_thai_baht,
        currency = Currency.getInstance("THB"),
        locale = Locale.Builder().setLanguage("th").setRegion("TH").build(),
    ),
}
