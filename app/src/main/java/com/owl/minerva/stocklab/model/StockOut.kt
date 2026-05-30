package com.owl.minerva.stocklab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_out")
data class StockOut(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var code: String = "",
    var stockId: Long,
    var ledgerId: Long,
    var amount: Double = 0.0,
    var note: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
