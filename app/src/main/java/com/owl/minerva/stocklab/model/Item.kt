package com.owl.minerva.stocklab.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owl.minerva.stocklab.enums.UnitType

@Entity(tableName = "item")
data class Item(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var code: String = "",
    var name: String? = null,
    var buyPrice: Double = 0.0,
    var sellPrice: Double = 0.0,
    var currentSellPrice: Double = 0.0,
    var profitTakePercent: Double = 0.0,
    var unit: UnitType = UnitType.PCS,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
