package com.despesas.gestor.data.backup

/**
 * Modelos simples (sem dependências de Room) que representam o conteúdo de uma
 * cópia de segurança. Mantê-los independentes das entidades permite testar o
 * codec em testes unitários de JVM.
 */
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val income: List<IncomeDto> = emptyList(),
    val receipts: List<ReceiptDto> = emptyList(),
    val items: List<ReceiptItemDto> = emptyList(),
    val fixed: List<FixedDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    val shoppingLists: List<ShoppingListDto> = emptyList(),
    val shoppingItems: List<ShoppingItemDto> = emptyList()
)

data class IncomeDto(val monthKey: String, val amount: Double)

data class ReceiptDto(
    val id: Long,
    val merchant: String,
    val categoryId: String,
    val total: Double,
    val dateMillis: Long,
    val monthKey: String,
    val imagePath: String?,
    val rawText: String?
)

data class ReceiptItemDto(
    val id: Long,
    val receiptId: Long,
    val name: String,
    val price: Double,
    val quantity: Double
)

data class FixedDto(
    val id: Long,
    val name: String,
    val provider: String?,
    val amount: Double,
    val dateMillis: Long,
    val monthKey: String,
    val paid: Boolean,
    val recurring: Boolean
)

data class BudgetDto(val categoryId: String, val amount: Double)

data class ShoppingListDto(val id: Long, val name: String, val createdAtMillis: Long)

data class ShoppingItemDto(
    val id: Long,
    val listId: Long,
    val name: String,
    val checked: Boolean
)
