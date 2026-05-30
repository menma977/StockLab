package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.model.HppComponent
import kotlinx.coroutines.flow.Flow

interface HppComponentRepository {
    fun getAll(): Flow<List<HppComponent>>
    suspend fun getById(id: Long): HppComponent?
    suspend fun getByHppId(hppId: Long): List<HppComponent>
    suspend fun insert(hppComponent: HppComponent): Long
    suspend fun insertAll(hppComponents: List<HppComponent>): List<Long>
    suspend fun update(hppComponent: HppComponent)
    suspend fun delete(hppComponent: HppComponent)
    suspend fun deleteByItemId(itemId: Long)
    suspend fun deleteAll()
}
