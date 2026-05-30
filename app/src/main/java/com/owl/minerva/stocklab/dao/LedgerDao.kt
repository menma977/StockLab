package com.owl.minerva.stocklab.dao

import androidx.room.*
import com.owl.minerva.stocklab.model.Ledger
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ledger: Ledger): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ledgers: List<Ledger>): List<Long>

    @Update
    suspend fun update(ledger: Ledger)

    @Delete
    suspend fun delete(ledger: Ledger)

    @Query("SELECT * FROM ledger ORDER BY id DESC")
    fun getAll(): Flow<List<Ledger>>

    @Query("SELECT * FROM ledger WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Ledger?

    @Query("SELECT * FROM ledger WHERE itemId = :itemId ORDER BY id DESC")
    suspend fun getByItemId(itemId: Long): List<Ledger>

    @Query("SELECT COUNT(*) FROM ledger WHERE itemId = :itemId")
    suspend fun countByItemId(itemId: Long): Int

    @Query("SELECT * FROM ledger WHERE itemId = :itemId ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getByItemIdPaged(itemId: Long, limit: Int, offset: Int): List<Ledger>

    @Query("SELECT * FROM ledger ORDER BY id DESC LIMIT :limit")
    fun getLatest(limit: Int): Flow<List<Ledger>>

    @Query(
        """
        SELECT COALESCE(
            SUM(
                CASE direction
                    WHEN 'OUT' THEN amount
                    ELSE -amount
                END
            ),
            0
        )
        FROM ledger
        WHERE createdAt >= :startAt
            AND createdAt < :endAt
        """
    )
    fun getTotalBetween(startAt: Long, endAt: Long): Flow<Long>

    @Query("DELETE FROM ledger WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: Long)

    @Query("DELETE FROM ledger")
    suspend fun deleteAll()
}
