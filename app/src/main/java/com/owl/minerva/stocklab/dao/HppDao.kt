package com.owl.minerva.stocklab.dao

import androidx.room.*
import com.owl.minerva.stocklab.model.Hpp
import kotlinx.coroutines.flow.Flow

@Dao
interface HppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hpp: Hpp): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hpps: List<Hpp>): List<Long>

    @Update
    suspend fun update(hpp: Hpp)

    @Delete
    suspend fun delete(hpp: Hpp)

    @Query("SELECT * FROM hpp ORDER BY id DESC")
    fun getAll(): Flow<List<Hpp>>

    @Query("SELECT * FROM hpp WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Hpp?

    @Query("SELECT * FROM hpp WHERE itemId = :itemId ORDER BY id DESC LIMIT 1")
    suspend fun getLatestByItemId(itemId: Long): Hpp?

    @Query("DELETE FROM hpp WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: Long)

    @Query("DELETE FROM hpp")
    suspend fun deleteAll()
}
