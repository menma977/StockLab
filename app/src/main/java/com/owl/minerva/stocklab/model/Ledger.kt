package com.owl.minerva.stocklab.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owl.minerva.stocklab.enums.LedgerDirection

@Entity(tableName = "ledger")
data class Ledger(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var code: String = "",
    var itemId: Long,
    var batchId: Long,
    var stockId: Long,
    var amount: Long,
    var direction: LedgerDirection = LedgerDirection.IN,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
