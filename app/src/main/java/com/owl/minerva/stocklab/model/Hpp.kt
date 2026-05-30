package com.owl.minerva.stocklab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hpp")
data class Hpp(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var itemId: Long,
    var total: Long,
    var amount: Long,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)