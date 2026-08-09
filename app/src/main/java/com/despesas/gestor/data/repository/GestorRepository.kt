package com.despesas.gestor.data.repository

import android.content.Context
import android.net.Uri
import com.despesas.gestor.data.backup.BackupCodec
import com.despesas.gestor.data.backup.BackupData
import com.despesas.gestor.data.backup.BudgetDto
import com.despesas.gestor.data.backup.FixedDto
import com.despesas.gestor.data.backup.IncomeDto
import com.despesas.gestor.data.backup.ReceiptDto
import com.despesas.gestor.data.backup.ReceiptItemDto
import com.despesas.gestor.data.backup.ShoppingItemDto
import com.despesas.gestor.data.backup.ShoppingListDto
import com.despesas.gestor.data.local.AppDatabase
import com.despesas.gestor.data.local.dao.CategoryTotal
import com.despesas.gestor.data.local.dao.MonthTotal
import com.despesas.gestor.data.local.dao.ReceiptWithItems
import com.despesas.gestor.data.local.entity.BudgetEntity
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth

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
    private val budgetDao = db.budgetDao()

    // --- Mês selecionado (partilhado entre ecrãs) ------------------------------
    private val _selectedMonth = MutableStateFlow(Dates.currentMonthKey())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    fun showMonth(monthKey: String) { _selectedMonth.value = monthKey }

    fun shiftMonth(delta: Long) {
        _selectedMonth.value = YearMonth.parse(_selectedMonth.value).plusMonths(delta).toString()
    }

    fun goToCurrentMonth() { _selectedMonth.value = Dates.currentMonthKey() }

    // --- Rendimento ------------------------------------------------------------
    fun observeIncome(monthKey: String): Flow<IncomeEntity?> = incomeDao.observe(monthKey)

    /** Rendimento do mês, ou o mais recente anterior (carry-over automático). */
    fun observeEffectiveIncome(monthKey: String): Flow<IncomeEntity?> =
        incomeDao.observeEffective(monthKey)

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

    suspend fun getReceiptWithItems(id: Long): ReceiptWithItems? =
        receiptDao.getReceiptWithItems(id)

    /** Atualiza uma fatura existente e substitui os seus itens. */
    suspend fun updateReceiptWithItems(
        receipt: ReceiptEntity,
        items: List<ReceiptItemEntity>
    ) {
        val fixed = receipt.copy(monthKey = Dates.monthKey(receipt.dateMillis))
        receiptDao.updateReceiptWithItems(fixed, items)
    }

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
        paid: Boolean,
        recurring: Boolean
    ) = fixedDao.insert(
        FixedExpenseEntity(
            name = name,
            provider = provider,
            amount = amount,
            dateMillis = dateMillis,
            monthKey = Dates.monthKey(dateMillis),
            paid = paid,
            recurring = recurring
        )
    )

    suspend fun updateFixedExpense(expense: FixedExpenseEntity) = fixedDao.update(expense)
    suspend fun deleteFixedExpense(expense: FixedExpenseEntity) = fixedDao.delete(expense)

    /**
     * Copia as contas marcadas como recorrentes do mês anterior mais recente
     * para [monthKey] (por pagar). Devolve quantas foram criadas.
     */
    suspend fun copyRecurringInto(monthKey: String): Int {
        val previous = fixedDao.getRecurringBefore(monthKey)
        if (previous.isEmpty()) return 0
        val monthStart = Dates.startOfDayMillis(YearMonth.parse(monthKey).atDay(1))
        val newOnes = previous.map {
            it.copy(
                id = 0,
                monthKey = monthKey,
                dateMillis = monthStart,
                paid = false
            )
        }
        fixedDao.insertAll(newOnes)
        return newOnes.size
    }

    // --- Orçamentos ------------------------------------------------------------
    fun observeBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeAll()
    suspend fun setBudget(categoryId: String, amount: Double) {
        if (amount <= 0.0) budgetDao.delete(categoryId)
        else budgetDao.upsert(BudgetEntity(categoryId, amount))
    }

    // --- Cópia de segurança ----------------------------------------------------

    /** Serializa toda a base de dados para uma string JSON. */
    suspend fun exportBackup(): String {
        val data = BackupData(
            version = BackupCodec.CURRENT_VERSION,
            exportedAt = System.currentTimeMillis(),
            income = incomeDao.getAll().map { IncomeDto(it.monthKey, it.amount) },
            receipts = receiptDao.getAllReceipts().map {
                ReceiptDto(it.id, it.merchant, it.categoryId, it.total, it.dateMillis, it.monthKey, it.imagePath, it.rawText)
            },
            items = receiptDao.getAllItems().map {
                ReceiptItemDto(it.id, it.receiptId, it.name, it.price, it.quantity)
            },
            fixed = fixedDao.getAll().map {
                FixedDto(it.id, it.name, it.provider, it.amount, it.dateMillis, it.monthKey, it.paid, it.recurring)
            },
            budgets = budgetDao.getAll().map { BudgetDto(it.categoryId, it.amount) },
            shoppingLists = shoppingDao.getAllLists().map { ShoppingListDto(it.id, it.name, it.createdAtMillis) },
            shoppingItems = shoppingDao.getAllItems().map { ShoppingItemDto(it.id, it.listId, it.name, it.checked) }
        )
        return BackupCodec.encode(data)
    }

    /** Substitui todos os dados pelos de uma cópia de segurança. */
    suspend fun importBackup(json: String) {
        val data = BackupCodec.decode(json)
        db.clearAllTables()
        incomeDao.insertAll(data.income.map { IncomeEntity(it.monthKey, it.amount) })
        receiptDao.insertReceiptsRestore(data.receipts.map {
            ReceiptEntity(it.id, it.merchant, it.categoryId, it.total, it.dateMillis, it.monthKey, it.imagePath, it.rawText)
        })
        receiptDao.insertItemsRestore(data.items.map {
            ReceiptItemEntity(it.id, it.receiptId, it.name, it.price, it.quantity)
        })
        fixedDao.insertAll(data.fixed.map {
            FixedExpenseEntity(it.id, it.name, it.provider, it.amount, it.dateMillis, it.monthKey, it.paid, it.recurring)
        })
        budgetDao.insertAll(data.budgets.map { BudgetEntity(it.categoryId, it.amount) })
        shoppingDao.insertListsRestore(data.shoppingLists.map {
            ShoppingListEntity(it.id, it.name, it.createdAtMillis)
        })
        shoppingDao.insertItemsRestore(data.shoppingItems.map {
            ShoppingItemEntity(it.id, it.listId, it.name, it.checked)
        })
    }

    /** Contas por pagar do mês, para notificações. */
    suspend fun unpaidBills(monthKey: String): List<FixedExpenseEntity> =
        fixedDao.getAll().filter { it.monthKey == monthKey && !it.paid }

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
