package com.owl.minerva.stocklab.dao

import androidx.room.*
import com.owl.minerva.stocklab.model.StockIn
import kotlinx.coroutines.flow.Flow

@Dao
interface StockInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stockIn: StockIn): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stockIns: List<StockIn>): List<Long>

    @Update
    suspend fun update(stockIn: StockIn)

    @Delete
    suspend fun delete(stockIn: StockIn)

    @Query("SELECT * FROM stock_in ORDER BY id DESC")
    fun getAll(): Flow<List<StockIn>>

    @Query("SELECT * FROM stock_in WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): StockIn?

    @Query(
        """
        SELECT stock_in.*
        FROM stock_in
        INNER JOIN stock ON stock.id = stock_in.stockId
        WHERE stock.itemId = :itemId
        ORDER BY stock_in.id DESC
        """
    )
    suspend fun getByItemId(itemId: Long): List<StockIn>

    @Query(
        """
        SELECT COUNT(*)
        FROM stock_in
        INNER JOIN stock ON stock.id = stock_in.stockId
        WHERE stock.itemId = :itemId
        """
    )
    suspend fun countByItemId(itemId: Long): Int

    @Query(
        """
        SELECT stock_in.*
        FROM stock_in
        INNER JOIN stock ON stock.id = stock_in.stockId
        WHERE stock.itemId = :itemId
        ORDER BY stock_in.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getByItemIdPaged(itemId: Long, limit: Int, offset: Int): List<StockIn>

    @Query(
        """
        DELETE FROM stock_in
        WHERE stockId IN (
            SELECT id FROM stock WHERE itemId = :itemId
        )
        """
    )
    suspend fun deleteByItemId(itemId: Long)

    @Query("DELETE FROM stock_in")
    suspend fun deleteAll()
}
