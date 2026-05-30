package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.enums.LedgerDirection
import com.owl.minerva.stocklab.model.*
import com.owl.minerva.stocklab.repository.*
import kotlinx.coroutines.flow.Flow

class ItemService(
    private val itemRepository: ItemRepository,
    private val hppRepository: HppRepository,
    private val hppComponentRepository: HppComponentRepository,
    private val batchRepository: BatchRepository,
    private val stockRepository: StockRepository,
    private val ledgerRepository: LedgerRepository,
    private val batchCostRepository: BatchCostRepository,
    private val stockInRepository: StockInRepository,
    private val stockOutRepository: StockOutRepository,
) {
    fun index(): Flow<List<Item>> = itemRepository.getAll()

    suspend fun show(id: Long): Item? = itemRepository.getById(id)

    suspend fun store(
        item: Item,
        initialStockAmount: Double,
        hppComponents: List<ItemHppComponentInput>,
    ): ItemStoreResult {
        require(!item.name.isNullOrBlank()) { "Item name cannot be blank." }
        require(item.buyPrice > 0.0) { "Buy price must be greater than zero." }
        require(initialStockAmount > 0.0) { "Initial stock amount must be greater than zero." }

        val batchAmount = initialStockAmount.toLong()
        require(batchAmount > 0) { "Batch amount must be greater than zero." }

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

        val itemCode = generateUniqueItemCode(item.name.orEmpty())
        val reusableHppPerUnit = validComponents.sumOf { component -> component.amount }
        val reusableTotalHpp = reusableHppPerUnit * batchAmount
        val itemId = itemRepository.insert(item.copy(code = itemCode))
        val hppId = hppRepository.insert(
            Hpp(
                itemId = itemId,
                total = reusableTotalHpp,
                amount = reusableHppPerUnit,
            ),
        )
        val hppComponentIds = validComponents.map { component ->
            hppComponentRepository.insert(
                HppComponent(
                    hppId = hppId,
                    name = component.name,
                    amount = component.amount,
                ),
            )
        }
        val batchId = batchRepository.insert(
            Batch(
                code = RecordCodeGenerator.batchCode(itemCode, 1),
                itemId = itemId,
                hppId = hppId,
                amount = batchAmount,
                totalHpp = 0,
                totalCost = 0,
            ),
        )
        val batchSnapshotComponents = validComponents.zip(hppComponentIds).map { (component, hppComponentId) ->
            BatchCost(
                batchId = batchId,
                hppComponentId = hppComponentId,
                name = component.name,
                amount = component.amount,
            )
        }
        val batchCostIds = batchSnapshotComponents.map { batchCost ->
            batchCostRepository.insert(batchCost)
        }
        val batchTotalHpp = reusableTotalHpp
        val insertedBatch = batchRepository.getById(batchId)
            ?: error("Created batch was not found.")
        batchRepository.update(
            insertedBatch.copy(
                totalHpp = batchTotalHpp,
                totalCost = batchTotalHpp,
            ),
        )
        val stockId = stockRepository.insert(
            Stock(
                itemId = itemId,
                batchId = batchId,
                amount = initialStockAmount,
            ),
        )
        val ledgerId = ledgerRepository.insert(
            Ledger(
                code = RecordCodeGenerator.ledgerCode(itemCode, 1),
                itemId = itemId,
                batchId = batchId,
                stockId = stockId,
                amount = reusableTotalHpp,
                direction = LedgerDirection.IN,
            ),
        )
        val stockInId = stockInRepository.insert(
            StockIn(
                code = RecordCodeGenerator.stockInCode(itemCode, 1),
                stockId = stockId,
                ledgerId = ledgerId,
                amount = initialStockAmount,
                note = "Initial stock",
            ),
        )

        return ItemStoreResult(
            itemId = itemId,
            hppId = hppId,
            hppComponentIds = hppComponentIds,
            batchId = batchId,
            stockId = stockId,
            ledgerId = ledgerId,
            batchCostIds = batchCostIds,
            stockInId = stockInId,
        )
    }

    suspend fun update(item: Item) {
        require(item.id > 0) { "Item id is required for update." }
        require(!item.name.isNullOrBlank()) { "Item name cannot be blank." }
        itemRepository.update(item.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(item: Item) {
        require(item.id > 0) { "Item id is required for delete." }

        stockOutRepository.deleteByItemId(item.id)
        stockInRepository.deleteByItemId(item.id)
        ledgerRepository.deleteByItemId(item.id)
        stockRepository.deleteByItemId(item.id)
        batchCostRepository.deleteByItemId(item.id)
        batchRepository.deleteByItemId(item.id)
        hppComponentRepository.deleteByItemId(item.id)
        hppRepository.deleteByItemId(item.id)
        itemRepository.delete(item)
    }

    private suspend fun generateUniqueItemCode(name: String): String {
        repeat(10) {
            val code = RecordCodeGenerator.itemCode(name)
            if (itemRepository.getByCode(code) == null) {
                return code
            }
        }

        error("Unable to generate a unique item code.")
    }
}

data class ItemHppComponentInput(
    val name: String,
    val amount: Long,
)

data class ItemStoreResult(
    val itemId: Long,
    val hppId: Long,
    val hppComponentIds: List<Long>,
    val batchId: Long,
    val stockId: Long,
    val ledgerId: Long,
    val batchCostIds: List<Long>,
    val stockInId: Long,
)
