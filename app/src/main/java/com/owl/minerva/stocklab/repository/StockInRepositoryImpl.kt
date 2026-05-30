package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.dao.StockInDao
import com.owl.minerva.stocklab.model.StockIn
import kotlinx.coroutines.flow.Flow

class StockInRepositoryImpl(
    private val stockInDao: StockInDao
) : StockInRepository {
    override fun getAll(): Flow<List<StockIn>> = stockInDao.getAll()
    override suspend fun getById(id: Long): StockIn? = stockInDao.getById(id)
    override suspend fun countByItemId(itemId: Long): Int = stockInDao.countByItemId(itemId)
    override suspend fun insert(stockIn: StockIn): Long = stockInDao.insert(stockIn)
    override suspend fun insertAll(stockIns: List<StockIn>): List<Long> = stockInDao.insertAll(stockIns)
    override suspend fun update(stockIn: StockIn) = stockInDao.update(stockIn)
    override suspend fun delete(stockIn: StockIn) = stockInDao.delete(stockIn)
    override suspend fun deleteByItemId(itemId: Long) = stockInDao.deleteByItemId(itemId)
    override suspend fun deleteAll() = stockInDao.deleteAll()
}
