package com.owl.minerva.stocklab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch_cost")
data class BatchCost(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var batchId: Long,
    var hppComponentId: Long,
    var name: String,
    var amount: Long,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)