package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.model.StockIn
import kotlinx.coroutines.flow.Flow

interface StockInRepository {
    fun getAll(): Flow<List<StockIn>>
    suspend fun getById(id: Long): StockIn?
    suspend fun countByItemId(itemId: Long): Int
    suspend fun insert(stockIn: StockIn): Long
    suspend fun update(stockIn: StockIn)
    suspend fun delete(stockIn: StockIn)
    suspend fun deleteByItemId(itemId: Long)
}
