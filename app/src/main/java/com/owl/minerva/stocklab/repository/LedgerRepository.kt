package com.owl.minerva.stocklab.repository

import com.owl.minerva.stocklab.model.Ledger
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun getAll(): Flow<List<Ledger>>
    suspend fun getById(id: Long): Ledger?
    suspend fun countByItemId(itemId: Long): Int
    suspend fun insert(ledger: Ledger): Long
    suspend fun update(ledger: Ledger)
    suspend fun delete(ledger: Ledger)
    suspend fun deleteByItemId(itemId: Long)
}
