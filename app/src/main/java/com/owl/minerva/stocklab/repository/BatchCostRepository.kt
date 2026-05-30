package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.model.BatchCost
import kotlinx.coroutines.flow.Flow

interface BatchCostRepository {
    fun getAll(): Flow<List<BatchCost>>
    suspend fun getById(id: Long): BatchCost?
    suspend fun insert(batchCost: BatchCost): Long
    suspend fun update(batchCost: BatchCost)
    suspend fun delete(batchCost: BatchCost)
    suspend fun deleteByItemId(itemId: Long)
}
