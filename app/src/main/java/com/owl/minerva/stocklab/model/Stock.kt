package com.owl.minerva.stocklab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock")
data class Stock(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var itemId: Long,
    var batchId: Long,
    var amount: Double = 0.0,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)