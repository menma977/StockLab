package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.R
import com.owl.minerva.stocklab.enums.LedgerDirection
import com.owl.minerva.stocklab.model.*
import com.owl.minerva.stocklab.repository.*
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToLong

class StockBatchService(
    private val itemRepository: ItemRepository,
    private val stockRepository: StockRepository,
    private val batchRepository: BatchRepository,
    private val stockInRepository: StockInRepository,
    private val hppRepository: HppRepository,
    private val hppComponentRepository: HppComponentRepository,
    private val ledgerRepository: LedgerRepository,
    private val batchCostRepository: BatchCostRepository,
) {
    fun index(): Flow<List<Stock>> = stockRepository.getAll()

    suspend fun show(id: Long): Stock? = stockRepository.getById(id)

    suspend fun store(
        itemId: Long,
        amount: Double,
        hppComponents: List<ItemHppComponentInput>? = null,
        profitTakePercent: Double? = null,
        hppId: Long? = null,
    ): StockBatchStoreResult {
        requireAppMessage(itemId > 0, R.string.error_item_id_required)
        requireAppMessage(amount > 0.0, R.string.error_stock_amount_greater_than_zero)
        profitTakePercent?.let { percent ->
            requireAppMessage(percent >= 0.0, R.string.error_profit_take_negative)
        }

        val batchAmount = amount
        requireAppMessage(batchAmount > 0.0, R.string.error_batch_amount_greater_than_zero)
        val item = itemRepository.getById(itemId)
            ?: throw AppMessageException(R.string.error_item_not_found)
        val itemCode = item.code.ifBlank { RecordCodeGenerator.itemCode(item.name.orEmpty()) }

        val hppInputComponents = hppComponents?.let { components ->
            validateHppComponents(components)
        }
        val hpp = if (hppInputComponents != null) {
            val hppPerUnit = hppInputComponents.sumOf { component -> component.amount }
            val hppIdFromInput = hppRepository.insert(
                Hpp(
                    itemId = itemId,
                    total = calculateTotalCost(hppPerUnit, batchAmount),
                    amount = hppPerUnit,
                ),
            )
            val insertedComponents = hppInputComponents.map { component ->
                val hppComponentId = hppComponentRepository.insert(
                    HppComponent(
                        hppId = hppIdFromInput,
                        name = component.name,
                        amount = component.amount,
                    ),
                )
                HppComponent(
                    id = hppComponentId,
                    hppId = hppIdFromInput,
                    name = component.name,
                    amount = component.amount,
                )
            }
            val effectiveProfitTakePercent = profitTakePercent ?: item.profitTakePercent
            itemRepository.update(
                item.copy(
                    buyPrice = hppInputComponents.first { component ->
                        component.name.equals("Buy Price", ignoreCase = true)
                    }.amount.toDouble(),
                    profitTakePercent = effectiveProfitTakePercent,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            HppWithComponents(
                id = hppIdFromInput,
                components = insertedComponents,
            )
        } else {
            val reusableHpp = when {
                hppId != null && hppId > 0 -> hppRepository.getById(hppId)
                else -> hppRepository.getLatestByItemId(itemId)
            } ?: throw AppMessageException(R.string.error_reusable_hpp_template_not_found)
            val reusableComponents = hppComponentRepository.getByHppId(reusableHpp.id)
            requireAppMessage(reusableComponents.isNotEmpty(), R.string.error_reusable_hpp_components_required)
            HppWithComponents(
                id = reusableHpp.id,
                components = reusableComponents,
            )
        }

        val batchId = batchRepository.insert(
            Batch(
                code = RecordCodeGenerator.batchCode(itemCode, batchRepository.countByItemId(itemId) + 1),
                itemId = itemId,
                hppId = hpp.id,
                amount = batchAmount,
                totalHpp = 0,
                totalCost = 0,
            ),
        )
        val batchSnapshotComponents = hpp.components.map { component ->
            BatchCost(
                batchId = batchId,
                hppComponentId = component.id,
                name = component.name,
                amount = component.amount,
            )
        }
        val batchCostIds = batchSnapshotComponents.map { batchCost ->
            batchCostRepository.insert(batchCost)
        }
        val batchTotalHpp = batchSnapshotComponents.sumOf { it.amount }
        val batchTotalCost = calculateTotalCost(batchTotalHpp, batchAmount)
        val insertedBatch = batchRepository.getById(batchId)
            ?: throw AppMessageException(R.string.error_unexpected_action)
        batchRepository.update(
            insertedBatch.copy(
                totalHpp = batchTotalCost,
                totalCost = batchTotalCost,
            ),
        )
        val stockId = stockRepository.insert(
            Stock(
                itemId = itemId,
                batchId = batchId,
                amount = amount,
            ),
        )
        val ledgerId = ledgerRepository.insert(
            Ledger(
                code = RecordCodeGenerator.ledgerCode(itemCode, ledgerRepository.countByItemId(itemId) + 1),
                itemId = itemId,
                batchId = batchId,
                stockId = stockId,
                amount = batchTotalCost,
                direction = LedgerDirection.IN,
            ),
        )
        val stockInId = stockInRepository.insert(
            StockIn(
                code = RecordCodeGenerator.stockInCode(itemCode, stockInRepository.countByItemId(itemId) + 1),
                stockId = stockId,
                ledgerId = ledgerId,
                amount = amount,
                note = "Stock added",
            ),
        )

        return StockBatchStoreResult(
            batchId = batchId,
            stockId = stockId,
            stockInId = stockInId,
            ledgerId = ledgerId,
            batchCostIds = batchCostIds,
        )
    }

    suspend fun update(stock: Stock) {
        requireAppMessage(stock.id > 0, R.string.error_stock_id_required)
        requireAppMessage(stock.amount >= 0.0, R.string.error_stock_amount_negative)
        stockRepository.update(stock.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(stock: Stock) = stockRepository.delete(stock)

    private fun calculateTotalCost(
        hppPerUnit: Long,
        quantity: Double,
    ): Long {
        return (hppPerUnit * quantity).roundToLong()
    }

    private fun validateHppComponents(
        hppComponents: List<ItemHppComponentInput>,
    ): List<ItemHppComponentInput> {
        val validComponents = hppComponents.map { component ->
            ItemHppComponentInput(
                name = component.name.trim(),
                amount = component.amount,
            )
        }
        requireAppMessage(validComponents.isNotEmpty(), R.string.error_at_least_one_hpp_component_required)
        validComponents.forEach { component ->
            requireAppMessage(component.name.isNotBlank(), R.string.error_hpp_component_name_blank)
            requireAppMessage(component.amount >= 0, R.string.error_hpp_component_amount_negative)
        }

        val buyPriceComponent = validComponents.firstOrNull { component ->
            component.name.equals("Buy Price", ignoreCase = true)
        } ?: throw AppMessageException(R.string.error_buy_price_required)
        requireAppMessage(buyPriceComponent.amount > 0, R.string.error_buy_price_greater_than_zero)

        return validComponents
    }
}

private data class HppWithComponents(
    val id: Long,
    val components: List<HppComponent>,
)

data class StockBatchStoreResult(
    val batchId: Long,
    val stockId: Long,
    val stockInId: Long,
    val ledgerId: Long,
    val batchCostIds: List<Long>,
)
