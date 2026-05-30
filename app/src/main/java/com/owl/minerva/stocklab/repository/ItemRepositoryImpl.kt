package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.dao.ItemDao
import com.owl.minerva.stocklab.model.Item
import kotlinx.coroutines.flow.Flow

class ItemRepositoryImpl(
    private val itemDao: ItemDao
) : ItemRepository {
    override fun getAll(): Flow<List<Item>> = itemDao.getAll()
    override suspend fun getById(id: Long): Item? = itemDao.getById(id)
    override suspend fun getByCode(code: String): Item? = itemDao.getByCode(code)
    override suspend fun insert(item: Item): Long = itemDao.insert(item)
    override suspend fun insertAll(items: List<Item>): List<Long> = itemDao.insertAll(items)
    override suspend fun update(item: Item) = itemDao.update(item)
    override suspend fun delete(item: Item) = itemDao.delete(item)
    override suspend fun deleteAll() = itemDao.deleteAll()
}
