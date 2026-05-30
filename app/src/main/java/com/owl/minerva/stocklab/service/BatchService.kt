package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.model.Batch
import com.owl.minerva.stocklab.repository.BatchRepository
import kotlinx.coroutines.flow.Flow

class BatchService(
    private val batchRepository: BatchRepository
) {
    fun index(): Flow<List<Batch>> = batchRepository.getAll()

    suspend fun show(id: Long): Batch? = batchRepository.getById(id)

    suspend fun store(batch: Batch): Long {
        require(batch.itemId > 0) { "Item id is required." }
        require(batch.hppId > 0) { "HPP id is required." }
        require(batch.amount > 0) { "Batch amount must be greater than zero." }
        require(batch.totalHpp >= 0) { "Total HPP cannot be negative." }
        require(batch.totalCost >= 0) { "Total cost cannot be negative." }
        return batchRepository.insert(batch)
    }

    suspend fun update(batch: Batch) {
        require(batch.id > 0) { "Batch id is required for update." }
        require(batch.itemId > 0) { "Item id is required." }
        require(batch.hppId > 0) { "HPP id is required." }
        require(batch.amount > 0) { "Batch amount must be greater than zero." }
        require(batch.totalHpp >= 0) { "Total HPP cannot be negative." }
        require(batch.totalCost >= 0) { "Total cost cannot be negative." }
        batchRepository.update(batch.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(batch: Batch) = batchRepository.delete(batch)
}
