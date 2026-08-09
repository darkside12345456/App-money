package com.despesas.gestor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.despesas.gestor.ui.navigation.AppBottomBar
import com.despesas.gestor.ui.navigation.Routes
import com.despesas.gestor.ui.screens.balance.BalanceScreen
import com.despesas.gestor.ui.screens.capture.CaptureScreen
import com.despesas.gestor.ui.screens.categories.CategoriesScreen
import com.despesas.gestor.ui.screens.categories.CategoryDetailScreen
import com.despesas.gestor.ui.screens.categories.EditReceiptScreen
import com.despesas.gestor.ui.screens.categories.ReceiptDetailScreen
import com.despesas.gestor.ui.screens.fixed.FixedExpensesScreen
import com.despesas.gestor.ui.screens.home.HomeScreen
import com.despesas.gestor.ui.screens.settings.SettingsScreen
import com.despesas.gestor.ui.screens.shopping.ShoppingDetailScreen
import com.despesas.gestor.ui.screens.shopping.ShoppingScreen

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topLevelRoutes = setOf(
        Routes.HOME, Routes.CATEGORIES, Routes.FIXED, Routes.SHOPPING
    )
    val showBottomBar = currentRoute in topLevelRoutes

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onSelect = ::navigateTab,
                    onCapture = { navController.navigate(Routes.CAPTURE) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            appGraph(navController)
        }
    }
}

private fun NavGraphBuilder.appGraph(
    navController: androidx.navigation.NavHostController
) {
    composable(Routes.HOME) {
        HomeScreen(
            onCapture = { navController.navigate(Routes.CAPTURE) },
            onOpenBalance = { navController.navigate(Routes.BALANCE) },
            onOpenCategory = { navController.navigate(Routes.categoryDetail(it)) },
            onOpenSettings = { navController.navigate(Routes.SETTINGS) }
        )
    }

    composable(Routes.CATEGORIES) {
        CategoriesScreen(
            onOpenCategory = { navController.navigate(Routes.categoryDetail(it)) }
        )
    }

    composable(
        route = Routes.CATEGORY_DETAIL,
        arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
    ) { entry ->
        val categoryId = entry.arguments?.getString("categoryId").orEmpty()
        CategoryDetailScreen(
            categoryId = categoryId,
            onBack = { navController.popBackStack() },
            onOpenReceipt = { navController.navigate(Routes.receiptDetail(it)) }
        )
    }

    composable(
        route = Routes.RECEIPT_DETAIL,
        arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
    ) { entry ->
        val receiptId = entry.arguments?.getLong("receiptId") ?: 0L
        ReceiptDetailScreen(
            receiptId = receiptId,
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate(Routes.receiptEdit(it)) }
        )
    }

    composable(
        route = Routes.RECEIPT_EDIT,
        arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
    ) { entry ->
        val receiptId = entry.arguments?.getLong("receiptId") ?: 0L
        EditReceiptScreen(
            receiptId = receiptId,
            onDone = { navController.popBackStack() }
        )
    }

    composable(Routes.SETTINGS) {
        SettingsScreen(onBack = { navController.popBackStack() })
    }

    composable(Routes.CAPTURE) {
        CaptureScreen(
            onClose = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
    }

    composable(Routes.FIXED) {
        FixedExpensesScreen()
    }

    composable(Routes.BALANCE) {
        BalanceScreen(onBack = { navController.popBackStack() })
    }

    composable(Routes.SHOPPING) {
        ShoppingScreen(
            onOpenList = { navController.navigate(Routes.shoppingDetail(it)) }
        )
    }

    composable(
        route = Routes.SHOPPING_DETAIL,
        arguments = listOf(navArgument("listId") { type = NavType.LongType })
    ) { entry ->
        val listId = entry.arguments?.getLong("listId") ?: 0L
        ShoppingDetailScreen(
            listId = listId,
            onBack = { navController.popBackStack() }
        )
    }
}
