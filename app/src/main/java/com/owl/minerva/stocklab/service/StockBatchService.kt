package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.enums.LedgerDirection
import com.owl.minerva.stocklab.model.*
import com.owl.minerva.stocklab.repository.*
import kotlinx.coroutines.flow.Flow

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
        require(itemId > 0) { "Item id is required." }
        require(amount > 0.0) { "Stock amount must be greater than zero." }
        profitTakePercent?.let { percent ->
            require(percent >= 0.0) { "Profit take cannot be negative." }
        }

        val batchAmount = amount.toLong()
        require(batchAmount > 0) { "Batch amount must be greater than zero." }
        val item = itemRepository.getById(itemId)
            ?: throw IllegalArgumentException("Item was not found.")
        val itemCode = item.code.ifBlank { RecordCodeGenerator.itemCode(item.name.orEmpty()) }

        val hppInputComponents = hppComponents?.let { components ->
            validateHppComponents(components)
        }
        val hpp = if (hppInputComponents != null) {
            val hppPerUnit = hppInputComponents.sumOf { component -> component.amount }
            val hppIdFromInput = hppRepository.insert(
                Hpp(
                    itemId = itemId,
                    total = hppPerUnit * batchAmount,
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
            } ?: throw IllegalArgumentException("Reusable HPP template was not found.")
            val reusableComponents = hppComponentRepository.getByHppId(reusableHpp.id)
            require(reusableComponents.isNotEmpty()) { "Reusable HPP components are required." }
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
        val batchTotalCost = batchTotalHpp * batchAmount
        val insertedBatch = batchRepository.getById(batchId)
            ?: error("Created batch was not found.")
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
        require(stock.id > 0) { "Stock id is required for update." }
        require(stock.amount >= 0.0) { "Stock amount cannot be negative." }
        stockRepository.update(stock.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(stock: Stock) = stockRepository.delete(stock)

    private fun validateHppComponents(
        hppComponents: List<ItemHppComponentInput>,
    ): List<ItemHppComponentInput> {
        val validComponents = hppComponents.map { component ->
            ItemHppComponentInput(
                name = component.name.trim(),
                amount = component.amount,
            )
        }
        require(validComponents.isNotEmpty()) { "At least one HPP component is required." }
        validComponents.forEach { component ->
            require(component.name.isNotBlank()) { "HPP component name cannot be blank." }
            require(component.amount >= 0) { "HPP component amount cannot be negative." }
        }

        val buyPriceComponent = validComponents.firstOrNull { component ->
            component.name.equals("Buy Price", ignoreCase = true)
        } ?: throw IllegalArgumentException("Buy price is required.")
        require(buyPriceComponent.amount > 0) { "Buy price must be greater than zero." }

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
