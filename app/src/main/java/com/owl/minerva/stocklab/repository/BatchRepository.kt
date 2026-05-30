package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.model.Batch
import kotlinx.coroutines.flow.Flow

interface BatchRepository {
    fun getAll(): Flow<List<Batch>>
    suspend fun getById(id: Long): Batch?
    suspend fun countByItemId(itemId: Long): Int
    suspend fun insert(batch: Batch): Long
    suspend fun update(batch: Batch)
    suspend fun delete(batch: Batch)
    suspend fun deleteByItemId(itemId: Long)
}
