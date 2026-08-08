package com.despesas.gestor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.despesas.gestor.data.local.entity.FixedExpenseEntity
import com.despesas.gestor.data.local.entity.IncomeEntity
import com.despesas.gestor.data.local.entity.ReceiptEntity
import com.despesas.gestor.data.local.entity.ReceiptItemEntity
import com.despesas.gestor.data.local.entity.ShoppingItemEntity
import com.despesas.gestor.data.local.entity.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

/** Total gasto agrupado por categoria (resultado de agregação). */
data class CategoryTotal(
    val categoryId: String,
    val total: Double,
    val receiptCount: Int
)

/** Total gasto num dado mês (para comparação entre meses). */
data class MonthTotal(
    val monthKey: String,
    val total: Double
)

/** Uma lista de compras com contagem de itens (total e marcados). */
data class ShoppingListSummary(
    val id: Long,
    val name: String,
    val createdAtMillis: Long,
    val itemCount: Int,
    val checkedCount: Int
)

/** Uma fatura com todos os seus itens. */
data class ReceiptWithItems(
    @Embedded val receipt: ReceiptEntity,
    @Relation(parentColumn = "id", entityColumn = "receiptId")
    val items: List<ReceiptItemEntity>
)

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income WHERE monthKey = :monthKey")
    fun observe(monthKey: String): Flow<IncomeEntity?>

    @Query("SELECT * FROM income WHERE monthKey = :monthKey")
    suspend fun get(monthKey: String): IncomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(income: IncomeEntity)
}

@Dao
interface ReceiptDao {
    @Insert
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Insert
    suspend fun insertItems(items: List<ReceiptItemEntity>)

    @Transaction
    suspend fun insertReceiptWithItems(
        receipt: ReceiptEntity,
        items: List<ReceiptItemEntity>
    ): Long {
        val id = insertReceipt(receipt)
        insertItems(items.map { it.copy(receiptId = id) })
        return id
    }

    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    @Query(
        """
        SELECT categoryId AS categoryId,
               SUM(total) AS total,
               COUNT(*) AS receiptCount
        FROM receipts
        WHERE monthKey = :monthKey
        GROUP BY categoryId
        ORDER BY total DESC
        """
    )
    fun observeCategoryTotals(monthKey: String): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM receipts WHERE monthKey = :monthKey AND categoryId = :categoryId ORDER BY dateMillis DESC")
    fun observeReceiptsForCategory(monthKey: String, categoryId: String): Flow<List<ReceiptEntity>>

    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observeReceiptWithItems(id: Long): Flow<ReceiptWithItems?>

    @Query("SELECT COALESCE(SUM(total), 0) FROM receipts WHERE monthKey = :monthKey")
    fun observeMonthTotal(monthKey: String): Flow<Double>

    @Query(
        """
        SELECT monthKey AS monthKey, SUM(total) AS total
        FROM receipts
        GROUP BY monthKey
        ORDER BY monthKey DESC
        LIMIT :limit
        """
    )
    fun observeRecentMonthTotals(limit: Int): Flow<List<MonthTotal>>

    @Query("SELECT * FROM receipts WHERE monthKey = :monthKey ORDER BY dateMillis DESC")
    fun observeAllReceipts(monthKey: String): Flow<List<ReceiptEntity>>
}

@Dao
interface FixedExpenseDao {
    @Insert
    suspend fun insert(expense: FixedExpenseEntity): Long

    @Update
    suspend fun update(expense: FixedExpenseEntity)

    @Delete
    suspend fun delete(expense: FixedExpenseEntity)

    @Query("SELECT * FROM fixed_expenses WHERE monthKey = :monthKey ORDER BY dateMillis DESC")
    fun observe(monthKey: String): Flow<List<FixedExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM fixed_expenses WHERE monthKey = :monthKey")
    fun observeMonthTotal(monthKey: String): Flow<Double>

    @Query(
        """
        SELECT monthKey AS monthKey, SUM(amount) AS total
        FROM fixed_expenses
        GROUP BY monthKey
        ORDER BY monthKey DESC
        LIMIT :limit
        """
    )
    fun observeRecentMonthTotals(limit: Int): Flow<List<MonthTotal>>
}

@Dao
interface ShoppingDao {
    @Insert
    suspend fun insertList(list: ShoppingListEntity): Long

    @Update
    suspend fun updateList(list: ShoppingListEntity)

    @Delete
    suspend fun deleteList(list: ShoppingListEntity)

    @Query("SELECT * FROM shopping_lists ORDER BY createdAtMillis DESC")
    fun observeLists(): Flow<List<ShoppingListEntity>>

    @Query(
        """
        SELECT l.id AS id, l.name AS name, l.createdAtMillis AS createdAtMillis,
               (SELECT COUNT(*) FROM shopping_items i WHERE i.listId = l.id) AS itemCount,
               (SELECT COUNT(*) FROM shopping_items i WHERE i.listId = l.id AND i.checked = 1) AS checkedCount
        FROM shopping_lists l
        ORDER BY l.createdAtMillis DESC
        """
    )
    fun observeListSummaries(): Flow<List<ShoppingListSummary>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    fun observeList(id: Long): Flow<ShoppingListEntity?>

    @Insert
    suspend fun insertItem(item: ShoppingItemEntity): Long

    @Update
    suspend fun updateItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteItem(item: ShoppingItemEntity)

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY checked ASC, id ASC")
    fun observeItems(listId: Long): Flow<List<ShoppingItemEntity>>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE listId = :listId")
    fun observeItemCount(listId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE listId = :listId AND checked = 1")
    fun observeCheckedCount(listId: Long): Flow<Int>
}
