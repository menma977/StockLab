package com.owl.minerva.stocklab.dao

import androidx.room.*
import com.owl.minerva.stocklab.model.StockOut
import kotlinx.coroutines.flow.Flow

@Dao
interface StockOutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stockOut: StockOut): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stockOuts: List<StockOut>): List<Long>

    @Update
    suspend fun update(stockOut: StockOut)

    @Delete
    suspend fun delete(stockOut: StockOut)

    @Query("SELECT * FROM stock_out ORDER BY id DESC")
    fun getAll(): Flow<List<StockOut>>

    @Query("SELECT * FROM stock_out WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): StockOut?

    @Query(
        """
        SELECT stock_out.*
        FROM stock_out
        INNER JOIN stock ON stock.id = stock_out.stockId
        WHERE stock.itemId = :itemId
        ORDER BY stock_out.id DESC
        """
    )
    suspend fun getByItemId(itemId: Long): List<StockOut>

    @Query(
        """
        SELECT COUNT(*)
        FROM stock_out
        INNER JOIN stock ON stock.id = stock_out.stockId
        WHERE stock.itemId = :itemId
        """
    )
    suspend fun countByItemId(itemId: Long): Int

    @Query(
        """
        SELECT stock_out.*
        FROM stock_out
        INNER JOIN stock ON stock.id = stock_out.stockId
        WHERE stock.itemId = :itemId
        ORDER BY stock_out.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getByItemIdPaged(itemId: Long, limit: Int, offset: Int): List<StockOut>

    @Query(
        """
        SELECT COALESCE(SUM(ledger.amount), 0)
        FROM stock_out
        INNER JOIN ledger ON ledger.id = stock_out.ledgerId
        WHERE stock_out.createdAt >= :startAt
            AND stock_out.createdAt < :endAt
        """
    )
    fun getRevenueBetween(startAt: Long, endAt: Long): Flow<Long>

    @Query(
        """
        SELECT COALESCE(
            SUM(ledger.amount - (stock_out.amount * batch_cost_summary.hppPerUnit)),
            0
        )
        FROM stock_out
        INNER JOIN ledger ON ledger.id = stock_out.ledgerId
        INNER JOIN stock ON stock.id = stock_out.stockId
        INNER JOIN (
            SELECT batchId, SUM(amount) AS hppPerUnit
            FROM batch_cost
            GROUP BY batchId
        ) AS batch_cost_summary ON batch_cost_summary.batchId = stock.batchId
        WHERE stock_out.createdAt >= :startAt
            AND stock_out.createdAt < :endAt
        """
    )
    fun getProfitBetween(startAt: Long, endAt: Long): Flow<Long>

    @Query(
        """
        DELETE FROM stock_out
        WHERE stockId IN (
            SELECT id FROM stock WHERE itemId = :itemId
        )
        """
    )
    suspend fun deleteByItemId(itemId: Long)

    @Query("DELETE FROM stock_out")
    suspend fun deleteAll()
}
