package com.owl.minerva.stocklab.service

object PricingService {
    fun calculateSellPrice(
        hppPerUnit: Double,
        profitTakePercent: Double,
    ): Double {
        return hppPerUnit * (1.0 + profitTakePercent / 100.0)
    }
}
