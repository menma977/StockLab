package com.owl.minerva.stocklab.dao

import androidx.room.*
import com.owl.minerva.stocklab.model.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT * FROM item ORDER BY id DESC")
    fun getAll(): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Item?

    @Query("SELECT * FROM item WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): Item?

}
