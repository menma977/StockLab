package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.model.Item
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    fun getAll(): Flow<List<Item>>
    suspend fun getById(id: Long): Item?
    suspend fun getByCode(code: String): Item?
    suspend fun insert(item: Item): Long
    suspend fun update(item: Item)
    suspend fun delete(item: Item)
}
