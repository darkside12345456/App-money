package com.despesas.gestor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/** Rotas de navegação da app. */
object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val CATEGORY_DETAIL = "category/{categoryId}"
    const val RECEIPT_DETAIL = "receipt/{receiptId}"
    const val CAPTURE = "capture"
    const val FIXED = "fixed"
    const val BALANCE = "balance"
    const val SHOPPING = "shopping"
    const val SHOPPING_DETAIL = "shopping/{listId}"

    fun categoryDetail(categoryId: String) = "category/$categoryId"
    fun receiptDetail(receiptId: Long) = "receipt/$receiptId"
    fun shoppingDetail(listId: Long) = "shopping/$listId"
}

/** Itens da barra de navegação inferior. */
enum class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(Routes.HOME, "Início", Icons.Outlined.Home),
    CATEGORIES(Routes.CATEGORIES, "Categorias", Icons.Outlined.Category),
    FIXED(Routes.FIXED, "Contas", Icons.Outlined.ReceiptLong),
    SHOPPING(Routes.SHOPPING, "Compras", Icons.Outlined.ShoppingCart)
}
