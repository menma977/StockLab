package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.model.BatchCost
import com.owl.minerva.stocklab.repository.BatchCostRepository
import kotlinx.coroutines.flow.Flow

class BatchCostService(
    private val batchCostRepository: BatchCostRepository
) {
    fun index(): Flow<List<BatchCost>> = batchCostRepository.getAll()

    suspend fun show(id: Long): BatchCost? = batchCostRepository.getById(id)

    suspend fun store(batchCost: BatchCost): Long {
        require(batchCost.batchId > 0) { "Batch id is required." }
        require(batchCost.hppComponentId > 0) { "HPP component id is required." }
        require(batchCost.name.isNotBlank()) { "Batch cost name cannot be blank." }
        require(batchCost.amount >= 0) { "Batch cost amount cannot be negative." }
        return batchCostRepository.insert(batchCost)
    }

    suspend fun update(batchCost: BatchCost) {
        require(batchCost.id > 0) { "Batch cost id is required for update." }
        require(batchCost.batchId > 0) { "Batch id is required." }
        require(batchCost.hppComponentId > 0) { "HPP component id is required." }
        require(batchCost.name.isNotBlank()) { "Batch cost name cannot be blank." }
        require(batchCost.amount >= 0) { "Batch cost amount cannot be negative." }
        batchCostRepository.update(batchCost.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(batchCost: BatchCost) = batchCostRepository.delete(batchCost)
}
