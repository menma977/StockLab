package com.owl.minerva.stocklab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch")
data class Batch(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var code: String = "",
    var itemId: Long,
    var hppId: Long,
    var amount: Double,
    var totalHpp: Long,
    var totalCost: Long,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
