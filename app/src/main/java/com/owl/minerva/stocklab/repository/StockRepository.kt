package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.model.Stock
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun getAll(): Flow<List<Stock>>
    suspend fun getById(id: Long): Stock?
    suspend fun insert(stock: Stock): Long
    suspend fun getAvailableByItemId(itemId: Long): List<Stock>
    suspend fun update(stock: Stock)
    suspend fun delete(stock: Stock)
    suspend fun deleteByItemId(itemId: Long)
}
