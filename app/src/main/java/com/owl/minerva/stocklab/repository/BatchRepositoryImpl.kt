package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.dao.BatchDao
import com.owl.minerva.stocklab.model.Batch
import kotlinx.coroutines.flow.Flow

class BatchRepositoryImpl(
    private val batchDao: BatchDao
) : BatchRepository {
    override fun getAll(): Flow<List<Batch>> = batchDao.getAll()
    override suspend fun getById(id: Long): Batch? = batchDao.getById(id)
    override suspend fun countByItemId(itemId: Long): Int = batchDao.countByItemId(itemId)
    override suspend fun insert(batch: Batch): Long = batchDao.insert(batch)
    override suspend fun insertAll(batches: List<Batch>): List<Long> = batchDao.insertAll(batches)
    override suspend fun update(batch: Batch) = batchDao.update(batch)
    override suspend fun delete(batch: Batch) = batchDao.delete(batch)
    override suspend fun deleteByItemId(itemId: Long) = batchDao.deleteByItemId(itemId)
    override suspend fun deleteAll() = batchDao.deleteAll()
}
