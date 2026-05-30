package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.model.Hpp
import kotlinx.coroutines.flow.Flow

interface HppRepository {
    fun getAll(): Flow<List<Hpp>>
    suspend fun getById(id: Long): Hpp?
    suspend fun getLatestByItemId(itemId: Long): Hpp?
    suspend fun insert(hpp: Hpp): Long
    suspend fun insertAll(hpps: List<Hpp>): List<Long>
    suspend fun update(hpp: Hpp)
    suspend fun delete(hpp: Hpp)
    suspend fun deleteByItemId(itemId: Long)
    suspend fun deleteAll()
}
