package com.owl.minerva.stocklab.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hpp_component")
data class HppComponent(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var hppId: Long,
    var name: String,
    var amount: Long,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)