package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.dao.HppDao
import com.owl.minerva.stocklab.model.Hpp
import kotlinx.coroutines.flow.Flow

class HppRepositoryImpl(
    private val hppDao: HppDao
) : HppRepository {
    override fun getAll(): Flow<List<Hpp>> = hppDao.getAll()
    override suspend fun getById(id: Long): Hpp? = hppDao.getById(id)
    override suspend fun getLatestByItemId(itemId: Long): Hpp? = hppDao.getLatestByItemId(itemId)
    override suspend fun insert(hpp: Hpp): Long = hppDao.insert(hpp)
    override suspend fun update(hpp: Hpp) = hppDao.update(hpp)
    override suspend fun delete(hpp: Hpp) = hppDao.delete(hpp)
    override suspend fun deleteByItemId(itemId: Long) = hppDao.deleteByItemId(itemId)
}
