package com.owl.minerva.stocklab.dao

import androidx.room.*
import com.owl.minerva.stocklab.model.Batch
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: Batch): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(batches: List<Batch>): List<Long>

    @Update
    suspend fun update(batch: Batch)

    @Delete
    suspend fun delete(batch: Batch)

    @Query("SELECT * FROM batch ORDER BY id DESC")
    fun getAll(): Flow<List<Batch>>

    @Query("SELECT * FROM batch WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Batch?

    @Query("SELECT * FROM batch WHERE itemId = :itemId ORDER BY id DESC")
    suspend fun getByItemId(itemId: Long): List<Batch>

    @Query("SELECT COUNT(*) FROM batch WHERE itemId = :itemId")
    suspend fun countByItemId(itemId: Long): Int

    @Query("SELECT * FROM batch WHERE itemId = :itemId ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getByItemIdPaged(itemId: Long, limit: Int, offset: Int): List<Batch>

    @Query("DELETE FROM batch WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: Long)

    @Query("DELETE FROM batch")
    suspend fun deleteAll()
}
