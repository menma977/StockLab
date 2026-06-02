package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.R

data class HppCostInput(
    val id: Int,
    val name: String = "",
    val amount: String = "",
)

object HppCostService {
    val fixedCostNames = setOf(
        "buy price",
        "tax",
        "fee",
        "packaging",
        "handling",
        "cargo",
        "production",
    )

    fun parseAmount(value: String): Long {
        return value.toDoubleOrNull()?.toLong() ?: 0L
    }

    fun calculateHppPerUnit(
        buyPrice: String,
        tax: String,
        fee: String,
        packaging: String,
        handling: String,
        cargo: String,
        production: String,
        dynamicCosts: List<HppCostInput>,
    ): Long {
        return parseAmount(buyPrice) +
                parseAmount(tax) +
                parseAmount(fee) +
                parseAmount(packaging) +
                parseAmount(handling) +
                parseAmount(cargo) +
                parseAmount(production) +
                dynamicCosts.sumOf { cost -> parseAmount(cost.amount) }
    }

    fun buildComponents(
        buyPrice: String,
        tax: String,
        fee: String,
        packaging: String,
        handling: String,
        cargo: String,
        production: String,
        dynamicCosts: List<HppCostInput>,
    ): List<ItemHppComponentInput> {
        val fixedCosts = listOf(
            ItemHppComponentInput("Buy Price", parseAmount(buyPrice)),
            ItemHppComponentInput("Tax", parseAmount(tax)),
            ItemHppComponentInput("Fee", parseAmount(fee)),
            ItemHppComponentInput("Packaging", parseAmount(packaging)),
            ItemHppComponentInput("Handling", parseAmount(handling)),
            ItemHppComponentInput("Cargo", parseAmount(cargo)),
            ItemHppComponentInput("Production", parseAmount(production)),
        )
        val extraCosts = dynamicCosts.mapNotNull { cost ->
            val costName = cost.name.trim()
            val costAmount = parseAmount(cost.amount)

            if (costName.isBlank() && costAmount == 0L) {
                null
            } else {
                requireAppMessage(costName.isNotBlank(), R.string.error_extra_cost_name_blank)
                ItemHppComponentInput(costName, costAmount)
            }
        }

        return fixedCosts + extraCosts
    }
}
