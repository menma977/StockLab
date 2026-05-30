package com.owl.minerva.stocklab.service

import com.owl.minerva.stocklab.model.Ledger
import com.owl.minerva.stocklab.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow

class LedgerService(
    private val ledgerRepository: LedgerRepository
) {
    fun index(): Flow<List<Ledger>> = ledgerRepository.getAll()

    suspend fun show(id: Long): Ledger? = ledgerRepository.getById(id)

    suspend fun store(ledger: Ledger): Long {
        require(ledger.itemId > 0) { "Item id is required." }
        require(ledger.batchId > 0) { "Batch id is required." }
        require(ledger.stockId > 0) { "Stock id is required." }
        return ledgerRepository.insert(ledger)
    }

    suspend fun update(ledger: Ledger) {
        require(ledger.id > 0) { "Ledger id is required for update." }
        require(ledger.itemId > 0) { "Item id is required." }
        require(ledger.batchId > 0) { "Batch id is required." }
        require(ledger.stockId > 0) { "Stock id is required." }
        ledgerRepository.update(ledger.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(ledger: Ledger) = ledgerRepository.delete(ledger)
}
