package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.dao.LedgerDao
import com.owl.minerva.stocklab.model.Ledger
import kotlinx.coroutines.flow.Flow

class LedgerRepositoryImpl(
    private val ledgerDao: LedgerDao
) : LedgerRepository {
    override fun getAll(): Flow<List<Ledger>> = ledgerDao.getAll()
    override suspend fun getById(id: Long): Ledger? = ledgerDao.getById(id)
    override suspend fun countByItemId(itemId: Long): Int = ledgerDao.countByItemId(itemId)
    override suspend fun insert(ledger: Ledger): Long = ledgerDao.insert(ledger)
    override suspend fun update(ledger: Ledger) = ledgerDao.update(ledger)
    override suspend fun delete(ledger: Ledger) = ledgerDao.delete(ledger)
    override suspend fun deleteByItemId(itemId: Long) = ledgerDao.deleteByItemId(itemId)
}
