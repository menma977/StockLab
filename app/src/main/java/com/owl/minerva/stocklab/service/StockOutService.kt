package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.R
import com.owl.minerva.stocklab.enums.LedgerDirection
import com.owl.minerva.stocklab.model.Ledger
import com.owl.minerva.stocklab.model.StockOut
import com.owl.minerva.stocklab.repository.ItemRepository
import com.owl.minerva.stocklab.repository.LedgerRepository
import com.owl.minerva.stocklab.repository.StockOutRepository
import com.owl.minerva.stocklab.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToLong

class StockOutService(
    private val stockOutRepository: StockOutRepository,
    private val itemRepository: ItemRepository? = null,
    private val stockRepository: StockRepository? = null,
    private val ledgerRepository: LedgerRepository? = null,
) {
    fun index(): Flow<List<StockOut>> = stockOutRepository.getAll()

    suspend fun show(id: Long): StockOut? = stockOutRepository.getById(id)

    suspend fun store(stockOut: StockOut): Long {
        requireAppMessage(stockOut.stockId > 0, R.string.error_stock_id_required)
        requireAppMessage(stockOut.ledgerId > 0, R.string.error_ledger_id_required)
        requireAppMessage(stockOut.amount > 0.0, R.string.error_stock_out_amount_greater_than_zero)
        return stockOutRepository.insert(stockOut)
    }

    suspend fun update(stockOut: StockOut) {
        requireAppMessage(stockOut.id > 0, R.string.error_stock_out_id_required)
        requireAppMessage(stockOut.stockId > 0, R.string.error_stock_id_required)
        requireAppMessage(stockOut.ledgerId > 0, R.string.error_ledger_id_required)
        requireAppMessage(stockOut.amount > 0.0, R.string.error_stock_out_amount_greater_than_zero)
        stockOutRepository.update(stockOut.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(stockOut: StockOut) = stockOutRepository.delete(stockOut)

    suspend fun sell(
        itemId: Long,
        quantity: Double,
        currentSellPrice: Double,
    ): StockOutSellResult {
        requireAppMessage(itemId > 0, R.string.error_product_required)
        requireAppMessage(quantity > 0.0, R.string.error_quantity_greater_than_zero)
        requireAppMessage(currentSellPrice > 0.0, R.string.error_current_sell_price_greater_than_zero)

        val itemRepository = requireNotNull(itemRepository) { "Item repository is required." }
        val stockRepository = requireNotNull(stockRepository) { "Stock repository is required." }
        val ledgerRepository = requireNotNull(ledgerRepository) { "Ledger repository is required." }
        val item = itemRepository.getById(itemId)
            ?: throw AppMessageException(R.string.error_product_not_found_period)
        val itemCode = item.code.ifBlank { RecordCodeGenerator.itemCode(item.name.orEmpty()) }
        val availableStocks = stockRepository.getAvailableByItemId(itemId)
        val availableQuantity = availableStocks.sumOf { stock -> stock.amount }
        requireAppMessage(availableQuantity >= quantity, R.string.error_not_enough_stock)

        itemRepository.update(
            item.copy(
                currentSellPrice = currentSellPrice,
                updatedAt = System.currentTimeMillis(),
            ),
        )

        var remainingQuantity = quantity
        var ledgerSequence = ledgerRepository.countByItemId(itemId) + 1
        var stockOutSequence = stockOutRepository.countByItemId(itemId) + 1
        val splitResults = mutableListOf<StockOutSellSplitResult>()
        availableStocks.forEach { stock ->
            if (remainingQuantity <= 0.0) {
                return@forEach
            }

            val soldQuantity = minOf(stock.amount, remainingQuantity)
            val revenue = (soldQuantity * currentSellPrice).roundToLong()
            stockRepository.update(
                stock.copy(
                    amount = stock.amount - soldQuantity,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            val ledgerId = ledgerRepository.insert(
                Ledger(
                    code = RecordCodeGenerator.ledgerCode(itemCode, ledgerSequence),
                    itemId = itemId,
                    batchId = stock.batchId,
                    stockId = stock.id,
                    amount = revenue,
                    direction = LedgerDirection.OUT,
                ),
            )
            val stockOutId = stockOutRepository.insert(
                StockOut(
                    code = RecordCodeGenerator.stockOutCode(itemCode, stockOutSequence),
                    stockId = stock.id,
                    ledgerId = ledgerId,
                    amount = soldQuantity,
                    note = "Sell",
                ),
            )
            splitResults.add(
                StockOutSellSplitResult(
                    stockId = stock.id,
                    ledgerId = ledgerId,
                    stockOutId = stockOutId,
                    quantity = soldQuantity,
                    revenue = revenue,
                ),
            )
            ledgerSequence += 1
            stockOutSequence += 1
            remainingQuantity -= soldQuantity
        }

        return StockOutSellResult(
            itemId = itemId,
            quantity = quantity,
            currentSellPrice = currentSellPrice,
            splits = splitResults,
        )
    }
}

data class StockOutSellResult(
    val itemId: Long,
    val quantity: Double,
    val currentSellPrice: Double,
    val splits: List<StockOutSellSplitResult>,
)

data class StockOutSellSplitResult(
    val stockId: Long,
    val ledgerId: Long,
    val stockOutId: Long,
    val quantity: Double,
    val revenue: Long,
)
