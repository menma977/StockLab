package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.model.StockIn
import com.owl.minerva.stocklab.repository.StockInRepository
import kotlinx.coroutines.flow.Flow

class StockInService(
    private val stockInRepository: StockInRepository
) {
    fun index(): Flow<List<StockIn>> = stockInRepository.getAll()

    suspend fun show(id: Long): StockIn? = stockInRepository.getById(id)

    suspend fun store(stockIn: StockIn): Long {
        require(stockIn.stockId > 0) { "Stock id is required." }
        require(stockIn.ledgerId > 0) { "Ledger id is required." }
        require(stockIn.amount > 0.0) { "Stock in amount must be greater than zero." }
        return stockInRepository.insert(stockIn)
    }

    suspend fun update(stockIn: StockIn) {
        require(stockIn.id > 0) { "Stock in id is required for update." }
        require(stockIn.stockId > 0) { "Stock id is required." }
        require(stockIn.ledgerId > 0) { "Ledger id is required." }
        require(stockIn.amount > 0.0) { "Stock in amount must be greater than zero." }
        stockInRepository.update(stockIn.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(stockIn: StockIn) = stockInRepository.delete(stockIn)
}
