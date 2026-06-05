package com.owl.minerva.stocklab.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
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
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS batch_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        code TEXT NOT NULL,
                        itemId INTEGER NOT NULL,
                        hppId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        totalHpp INTEGER NOT NULL,
                        totalCost INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO batch_new (
                        id,
                        code,
                        itemId,
                        hppId,
                        amount,
                        totalHpp,
                        totalCost,
                        createdAt,
                        updatedAt
                    )
                    SELECT
                        id,
                        code,
                        itemId,
                        hppId,
                        CAST(amount AS REAL),
                        totalHpp,
                        totalCost,
                        createdAt,
                        updatedAt
                    FROM batch
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE batch")
                db.execSQL("ALTER TABLE batch_new RENAME TO batch")
            }
        }
    }
}
