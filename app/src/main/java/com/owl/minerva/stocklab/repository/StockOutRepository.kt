package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.model.StockOut
import kotlinx.coroutines.flow.Flow

interface StockOutRepository {
    fun getAll(): Flow<List<StockOut>>
    suspend fun getById(id: Long): StockOut?
    suspend fun countByItemId(itemId: Long): Int
    suspend fun insert(stockOut: StockOut): Long
    suspend fun insertAll(stockOuts: List<StockOut>): List<Long>
    suspend fun update(stockOut: StockOut)
    suspend fun delete(stockOut: StockOut)
    suspend fun deleteByItemId(itemId: Long)
    suspend fun deleteAll()
}
