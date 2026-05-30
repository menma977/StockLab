package com.owl.minerva.stocklab.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.owl.minerva.stocklab.dao.*
import com.owl.minerva.stocklab.model.*

@Database(
    entities = [
        Item::class,
        Batch::class,
        Hpp::class,
        HppComponent::class,
        Ledger::class,
        Stock::class,
        StockIn::class,
        StockOut::class,
        BatchCost::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StockLabDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun batchDao(): BatchDao
    abstract fun hppDao(): HppDao
    abstract fun hppComponentDao(): HppComponentDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun stockDao(): StockDao
    abstract fun stockInDao(): StockInDao
    abstract fun stockOutDao(): StockOutDao
    abstract fun batchCostDao(): BatchCostDao

    companion object {
        private const val DATABASE_NAME = "stock_lab.db"

        @Volatile
        private var instance: StockLabDatabase? = null

        fun getInstance(context: Context): StockLabDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockLabDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
