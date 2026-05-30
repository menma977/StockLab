package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.dao.StockOutDao
import com.owl.minerva.stocklab.model.StockOut
import kotlinx.coroutines.flow.Flow

class StockOutRepositoryImpl(
    private val stockOutDao: StockOutDao
) : StockOutRepository {
    override fun getAll(): Flow<List<StockOut>> = stockOutDao.getAll()
    override suspend fun getById(id: Long): StockOut? = stockOutDao.getById(id)
    override suspend fun countByItemId(itemId: Long): Int = stockOutDao.countByItemId(itemId)
    override suspend fun insert(stockOut: StockOut): Long = stockOutDao.insert(stockOut)
    override suspend fun insertAll(stockOuts: List<StockOut>): List<Long> = stockOutDao.insertAll(stockOuts)
    override suspend fun update(stockOut: StockOut) = stockOutDao.update(stockOut)
    override suspend fun delete(stockOut: StockOut) = stockOutDao.delete(stockOut)
    override suspend fun deleteByItemId(itemId: Long) = stockOutDao.deleteByItemId(itemId)
    override suspend fun deleteAll() = stockOutDao.deleteAll()
}
