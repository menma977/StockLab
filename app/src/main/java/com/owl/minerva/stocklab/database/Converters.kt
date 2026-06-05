package com.owl.minerva.stocklab.database

import androidx.room.TypeConverter
import com.owl.minerva.stocklab.enums.LedgerDirection
import com.owl.minerva.stocklab.enums.UnitType

class Converters {
    @TypeConverter
    fun fromUnitType(value: UnitType): String = value.name

    @TypeConverter
    fun toUnitType(value: String): UnitType = runCatching {
        UnitType.valueOf(value)
    }.getOrDefault(UnitType.PCS)

    @TypeConverter
    fun fromLedgerDirection(value: LedgerDirection): String = value.name

    @TypeConverter
    fun toLedgerDirection(value: String): LedgerDirection = runCatching {
        LedgerDirection.valueOf(value)
    }.getOrDefault(LedgerDirection.IN)
}
