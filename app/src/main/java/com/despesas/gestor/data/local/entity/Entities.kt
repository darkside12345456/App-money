package com.despesas.gestor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Rendimento mensal (ordenado) introduzido pelo utilizador.
 * Guardado por mês para permitir histórico e comparações.
 */
@Entity(tableName = "income")
data class IncomeEntity(
    @PrimaryKey val monthKey: String, // "2026-08"
    val amount: Double
)

/**
 * Uma fatura/talão. O total e a categoria são preenchidos automaticamente
 * pelo OCR, mas podem ser corrigidos pelo utilizador.
 */
@Entity(
    tableName = "receipts",
    indices = [Index("categoryId"), Index("monthKey")]
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val categoryId: String,
    val total: Double,
    val dateMillis: Long,
    val monthKey: String,
    val imagePath: String? = null,
    val rawText: String? = null
)

/** Um item individual dentro de uma fatura (nome + valor). */
@Entity(
    tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("receiptId")]
)
data class ReceiptItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptId: Long,
    val name: String,
    val price: Double,
    val quantity: Double = 1.0
)

/** Despesa fixa / conta recorrente (luz, água, gás, internet, ...). */
@Entity(
    tableName = "fixed_expenses",
    indices = [Index("monthKey")]
)
data class FixedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,            // ex.: "Luz", "Internet"
    val provider: String? = null, // ex.: "EDP", "MEO"
    val amount: Double,
    val dateMillis: Long,
    val monthKey: String,
    val paid: Boolean = false,
    /** Se verdadeiro, pode ser copiada automaticamente para o mês seguinte. */
    val recurring: Boolean = false
)

/** Orçamento (limite) definido pelo utilizador para uma categoria. */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val categoryId: String,
    val amount: Double
)

/** Uma lista de compras. */
@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMillis: Long
)

/** Um item dentro de uma lista de compras. */
@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val checked: Boolean = false
)
