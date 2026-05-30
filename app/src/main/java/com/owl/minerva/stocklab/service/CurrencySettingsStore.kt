package com.owl.minerva.stocklab.service

import android.content.Context
import com.owl.minerva.stocklab.enums.AppCurrency

class CurrencySettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun getCurrency(): AppCurrency {
        val value = preferences.getString(KEY_CURRENCY, AppCurrency.USD.name)
        return AppCurrency.entries.firstOrNull { currency -> currency.name == value }
            ?: AppCurrency.USD
    }

    fun setCurrency(currency: AppCurrency) {
        preferences.edit()
            .putString(KEY_CURRENCY, currency.name)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "stock_lab_settings"
        const val KEY_CURRENCY = "currency"
    }
}
