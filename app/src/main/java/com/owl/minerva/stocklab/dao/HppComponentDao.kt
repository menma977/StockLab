package com.owl.minerva.stocklab.dao

import androidx.room.*
import com.owl.minerva.stocklab.model.HppComponent
import kotlinx.coroutines.flow.Flow

@Dao
interface HppComponentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hppComponent: HppComponent): Long

    @Update
    suspend fun update(hppComponent: HppComponent)

    @Delete
    suspend fun delete(hppComponent: HppComponent)

    @Query("SELECT * FROM hpp_component ORDER BY id DESC")
    fun getAll(): Flow<List<HppComponent>>

    @Query("SELECT * FROM hpp_component WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HppComponent?

    @Query("SELECT * FROM hpp_component WHERE hppId = :hppId ORDER BY id ASC")
    suspend fun getByHppId(hppId: Long): List<HppComponent>

    @Query(
        """
        DELETE FROM hpp_component
        WHERE hppId IN (
            SELECT id FROM hpp WHERE itemId = :itemId
        )
        """
    )
    suspend fun deleteByItemId(itemId: Long)

}
