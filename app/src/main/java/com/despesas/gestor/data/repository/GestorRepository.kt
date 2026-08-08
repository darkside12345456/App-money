package com.despesas.gestor.data.repository

import android.content.Context
import android.net.Uri
import com.despesas.gestor.data.local.AppDatabase
import com.despesas.gestor.data.local.dao.CategoryTotal
import com.despesas.gestor.data.local.dao.MonthTotal
import com.despesas.gestor.data.local.dao.ReceiptWithItems
import com.despesas.gestor.data.local.entity.FixedExpenseEntity
import com.despesas.gestor.data.local.entity.IncomeEntity
import com.despesas.gestor.data.local.entity.ReceiptEntity
import com.despesas.gestor.data.local.entity.ReceiptItemEntity
import com.despesas.gestor.data.local.entity.ShoppingItemEntity
import com.despesas.gestor.data.local.entity.ShoppingListEntity
import com.despesas.gestor.data.ocr.OcrService
import com.despesas.gestor.data.ocr.ParsedReceipt
import com.despesas.gestor.util.Dates
import kotlinx.coroutines.flow.Flow

/**
 * Ponto único de acesso a dados: base de dados local (Room) + OCR.
 * Os ViewModels dependem apenas desta classe.
 */
class GestorRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val ocr: OcrService
) {
    private val incomeDao = db.incomeDao()
    private val receiptDao = db.receiptDao()
    private val fixedDao = db.fixedExpenseDao()
    private val shoppingDao = db.shoppingDao()

    // --- Rendimento ------------------------------------------------------------
    fun observeIncome(monthKey: String): Flow<IncomeEntity?> = incomeDao.observe(monthKey)
    suspend fun setIncome(monthKey: String, amount: Double) =
        incomeDao.upsert(IncomeEntity(monthKey, amount))

    // --- Faturas / OCR ---------------------------------------------------------

    /** Corre o OCR sobre a foto e devolve a fatura já estruturada (sem gravar). */
    suspend fun scanReceipt(uri: Uri): ParsedReceipt {
        val lines = ocr.recognize(uri)
        return com.despesas.gestor.data.ocr.ReceiptParser.parse(lines)
    }

    /** Grava uma fatura (e os seus itens) na base de dados. */
    suspend fun saveReceipt(
        merchant: String,
        categoryId: String,
        total: Double,
        dateMillis: Long,
        items: List<ReceiptItemEntity>,
        imagePath: String?,
        rawText: String?
    ): Long {
        val receipt = ReceiptEntity(
            merchant = merchant,
            categoryId = categoryId,
            total = total,
            dateMillis = dateMillis,
            monthKey = Dates.monthKey(dateMillis),
            imagePath = imagePath,
            rawText = rawText
        )
        return receiptDao.insertReceiptWithItems(receipt, items)
    }

    suspend fun updateReceipt(receipt: ReceiptEntity) = receiptDao.updateReceipt(receipt)
    suspend fun deleteReceipt(receipt: ReceiptEntity) = receiptDao.deleteReceipt(receipt)

    fun observeCategoryTotals(monthKey: String): Flow<List<CategoryTotal>> =
        receiptDao.observeCategoryTotals(monthKey)

    fun observeReceiptsForCategory(monthKey: String, categoryId: String): Flow<List<ReceiptEntity>> =
        receiptDao.observeReceiptsForCategory(monthKey, categoryId)

    fun observeReceiptWithItems(id: Long): Flow<ReceiptWithItems?> =
        receiptDao.observeReceiptWithItems(id)

    fun observeReceiptsTotal(monthKey: String): Flow<Double> =
        receiptDao.observeMonthTotal(monthKey)

    fun observeRecentMonthTotals(limit: Int = 6): Flow<List<MonthTotal>> =
        receiptDao.observeRecentMonthTotals(limit)

    // --- Despesas fixas --------------------------------------------------------
    fun observeFixedExpenses(monthKey: String): Flow<List<FixedExpenseEntity>> =
        fixedDao.observe(monthKey)

    fun observeFixedTotal(monthKey: String): Flow<Double> = fixedDao.observeMonthTotal(monthKey)

    fun observeRecentFixedMonthTotals(limit: Int = 6): Flow<List<MonthTotal>> =
        fixedDao.observeRecentMonthTotals(limit)

    suspend fun addFixedExpense(
        name: String,
        provider: String?,
        amount: Double,
        dateMillis: Long,
        paid: Boolean
    ) = fixedDao.insert(
        FixedExpenseEntity(
            name = name,
            provider = provider,
            amount = amount,
            dateMillis = dateMillis,
            monthKey = Dates.monthKey(dateMillis),
            paid = paid
        )
    )

    suspend fun updateFixedExpense(expense: FixedExpenseEntity) = fixedDao.update(expense)
    suspend fun deleteFixedExpense(expense: FixedExpenseEntity) = fixedDao.delete(expense)

    // --- Listas de compras -----------------------------------------------------
    fun observeShoppingLists(): Flow<List<ShoppingListEntity>> = shoppingDao.observeLists()
    fun observeShoppingListSummaries(): Flow<List<com.despesas.gestor.data.local.dao.ShoppingListSummary>> =
        shoppingDao.observeListSummaries()
    fun observeShoppingList(id: Long): Flow<ShoppingListEntity?> = shoppingDao.observeList(id)
    fun observeShoppingItems(listId: Long): Flow<List<ShoppingItemEntity>> =
        shoppingDao.observeItems(listId)
    fun observeItemCount(listId: Long): Flow<Int> = shoppingDao.observeItemCount(listId)
    fun observeCheckedCount(listId: Long): Flow<Int> = shoppingDao.observeCheckedCount(listId)

    suspend fun createShoppingList(name: String): Long =
        shoppingDao.insertList(ShoppingListEntity(name = name, createdAtMillis = System.currentTimeMillis()))
    suspend fun deleteShoppingList(list: ShoppingListEntity) = shoppingDao.deleteList(list)
    suspend fun addShoppingItem(listId: Long, name: String) =
        shoppingDao.insertItem(ShoppingItemEntity(listId = listId, name = name))
    suspend fun updateShoppingItem(item: ShoppingItemEntity) = shoppingDao.updateItem(item)
    suspend fun deleteShoppingItem(item: ShoppingItemEntity) = shoppingDao.deleteItem(item)
}
