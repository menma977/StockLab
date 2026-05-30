package com.owl.minerva.stocklab.dao

import androidx.room.*
import com.owl.minerva.stocklab.model.Stock
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stock: Stock): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<Stock>): List<Long>

    @Update
    suspend fun update(stock: Stock)

    @Delete
    suspend fun delete(stock: Stock)

    @Query("SELECT * FROM stock ORDER BY id DESC")
    fun getAll(): Flow<List<Stock>>

    @Query("SELECT * FROM stock WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Stock?

    @Query("SELECT * FROM stock ORDER BY id DESC")
    suspend fun getAllSnapshot(): List<Stock>

    @Query("SELECT * FROM stock WHERE itemId = :itemId AND amount > 0 ORDER BY id ASC")
    suspend fun getAvailableByItemId(itemId: Long): List<Stock>

    @Query("DELETE FROM stock WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: Long)

    @Query("DELETE FROM stock")
    suspend fun deleteAll()
}
