package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.dao.BatchCostDao
import com.owl.minerva.stocklab.model.BatchCost
import kotlinx.coroutines.flow.Flow

class BatchCostRepositoryImpl(
    private val batchCostDao: BatchCostDao
) : BatchCostRepository {
    override fun getAll(): Flow<List<BatchCost>> = batchCostDao.getAll()
    override suspend fun getById(id: Long): BatchCost? = batchCostDao.getById(id)
    override suspend fun insert(batchCost: BatchCost): Long = batchCostDao.insert(batchCost)
    override suspend fun update(batchCost: BatchCost) = batchCostDao.update(batchCost)
    override suspend fun delete(batchCost: BatchCost) = batchCostDao.delete(batchCost)
    override suspend fun deleteByItemId(itemId: Long) = batchCostDao.deleteByItemId(itemId)
}
