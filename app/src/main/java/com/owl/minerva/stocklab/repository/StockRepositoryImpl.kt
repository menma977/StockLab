package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.dao.StockDao
import com.owl.minerva.stocklab.model.Stock
import kotlinx.coroutines.flow.Flow

class StockRepositoryImpl(
    private val stockDao: StockDao
) : StockRepository {
    override fun getAll(): Flow<List<Stock>> = stockDao.getAll()
    override suspend fun getById(id: Long): Stock? = stockDao.getById(id)
    override suspend fun insert(stock: Stock): Long = stockDao.insert(stock)
    override suspend fun insertAll(stocks: List<Stock>): List<Long> = stockDao.insertAll(stocks)
    override suspend fun getAvailableByItemId(itemId: Long): List<Stock> = stockDao.getAvailableByItemId(itemId)
    override suspend fun update(stock: Stock) = stockDao.update(stock)
    override suspend fun delete(stock: Stock) = stockDao.delete(stock)
    override suspend fun deleteByItemId(itemId: Long) = stockDao.deleteByItemId(itemId)
    override suspend fun deleteAll() = stockDao.deleteAll()
}
