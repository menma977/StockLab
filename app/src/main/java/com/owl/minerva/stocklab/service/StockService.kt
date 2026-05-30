package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.model.Batch
import com.owl.minerva.stocklab.model.Stock
import com.owl.minerva.stocklab.model.StockIn
import com.owl.minerva.stocklab.repository.BatchRepository
import com.owl.minerva.stocklab.repository.StockInRepository
import com.owl.minerva.stocklab.repository.StockRepository
import kotlinx.coroutines.flow.Flow

class StockService(
    private val stockRepository: StockRepository,
    private val batchRepository: BatchRepository,
    private val stockInRepository: StockInRepository,
) {
    fun index(): Flow<List<Stock>> = stockRepository.getAll()

    suspend fun show(id: Long): Stock? = stockRepository.getById(id)

    suspend fun store(
        stock: Stock,
        batch: Batch,
        stockIn: StockIn
    ): StockStoreResult {
        require(batch.itemId > 0) { "Batch item id is required." }
        require(batch.hppId > 0) { "HPP id is required." }
        require(batch.amount > 0) { "Batch amount must be greater than zero." }
        require(batch.totalHpp >= 0) { "Total HPP cannot be negative." }
        require(batch.totalCost >= 0) { "The total cost cannot be negative." }
        require(stock.itemId > 0) { "Item id is required." }
        require(stock.amount >= 0.0) { "Stock amount cannot be negative." }
        require(stockIn.ledgerId > 0) { "Ledger id is required." }
        require(stockIn.amount > 0.0) { "Stock in amount must be greater than zero." }

        val batchId = batchRepository.insert(batch)
        val stockId = stockRepository.insert(stock.copy(batchId = batchId))
        val stockInId = stockInRepository.insert(stockIn.copy(stockId = stockId))

        return StockStoreResult(
            batchId = batchId,
            stockId = stockId,
            stockInId = stockInId
        )
    }

    suspend fun update(stock: Stock) {
        require(stock.id > 0) { "Stock id is required for update." }
        require(stock.amount >= 0.0) { "Stock amount cannot be negative." }
        stockRepository.update(stock.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(stock: Stock) = stockRepository.delete(stock)
}

data class StockStoreResult(
    val batchId: Long,
    val stockId: Long,
    val stockInId: Long,
)
