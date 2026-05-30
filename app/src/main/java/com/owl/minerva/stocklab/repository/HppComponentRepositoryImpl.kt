package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.dao.HppComponentDao
import com.owl.minerva.stocklab.model.HppComponent
import kotlinx.coroutines.flow.Flow

class HppComponentRepositoryImpl(
    private val hppComponentDao: HppComponentDao
) : HppComponentRepository {
    override fun getAll(): Flow<List<HppComponent>> = hppComponentDao.getAll()
    override suspend fun getById(id: Long): HppComponent? = hppComponentDao.getById(id)
    override suspend fun getByHppId(hppId: Long): List<HppComponent> = hppComponentDao.getByHppId(hppId)
    override suspend fun insert(hppComponent: HppComponent): Long = hppComponentDao.insert(hppComponent)
    override suspend fun insertAll(hppComponents: List<HppComponent>): List<Long> = hppComponentDao.insertAll(hppComponents)
    override suspend fun update(hppComponent: HppComponent) = hppComponentDao.update(hppComponent)
    override suspend fun delete(hppComponent: HppComponent) = hppComponentDao.delete(hppComponent)
    override suspend fun deleteByItemId(itemId: Long) = hppComponentDao.deleteByItemId(itemId)
    override suspend fun deleteAll() = hppComponentDao.deleteAll()
}
