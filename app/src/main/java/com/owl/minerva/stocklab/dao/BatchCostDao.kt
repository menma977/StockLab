package com.owl.minerva.stocklab.dao

import androidx.room.*
import com.owl.minerva.stocklab.model.BatchCost
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchCostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batchCost: BatchCost): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(batchCosts: List<BatchCost>): List<Long>

    @Update
    suspend fun update(batchCost: BatchCost)

    @Delete
    suspend fun delete(batchCost: BatchCost)

    @Query("SELECT * FROM batch_cost ORDER BY id DESC")
    fun getAll(): Flow<List<BatchCost>>

    @Query("SELECT * FROM batch_cost WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BatchCost?

    @Query("SELECT * FROM batch_cost WHERE batchId = :batchId ORDER BY id ASC")
    suspend fun getByBatchId(batchId: Long): List<BatchCost>

    @Query(
        """
        DELETE FROM batch_cost
        WHERE batchId IN (
            SELECT id FROM batch WHERE itemId = :itemId
        )
        """
    )
    suspend fun deleteByItemId(itemId: Long)

    @Query("DELETE FROM batch_cost")
    suspend fun deleteAll()
}
