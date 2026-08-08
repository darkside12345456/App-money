package com.despesas.gestor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.despesas.gestor.data.local.dao.FixedExpenseDao
import com.despesas.gestor.data.local.dao.IncomeDao
import com.despesas.gestor.data.local.dao.ReceiptDao
import com.despesas.gestor.data.local.dao.ShoppingDao
import com.despesas.gestor.data.local.entity.FixedExpenseEntity
import com.despesas.gestor.data.local.entity.IncomeEntity
import com.despesas.gestor.data.local.entity.ReceiptEntity
import com.despesas.gestor.data.local.entity.ReceiptItemEntity
import com.despesas.gestor.data.local.entity.ShoppingItemEntity
import com.despesas.gestor.data.local.entity.ShoppingListEntity

@Database(
    entities = [
        IncomeEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        FixedExpenseEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun fixedExpenseDao(): FixedExpenseDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestor.db"
                ).build().also { INSTANCE = it }
            }
    }
}
