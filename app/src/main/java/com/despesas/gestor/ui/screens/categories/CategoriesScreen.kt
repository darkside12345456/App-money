package com.despesas.gestor.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despesas.gestor.data.local.dao.CategoryTotal
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class CategoriesUiState(
    val monthKey: String,
    val total: Double,
    val categories: List<CategoryTotal>,
    val budgets: Map<String, Double> = emptyMap()
) {
    val monthLabel: String get() = Dates.monthLabel(monthKey)
    val isCurrentMonth: Boolean get() = monthKey == Dates.currentMonthKey()
}

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel(private val repo: GestorRepository) : ViewModel() {

    val state: StateFlow<CategoriesUiState> = repo.selectedMonth.flatMapLatest { monthKey ->
        combine(
            repo.observeCategoryTotals(monthKey),
            repo.observeBudgets()
        ) { list, budgets ->
            CategoriesUiState(
                monthKey = monthKey,
                total = list.sumOf { it.total },
                categories = list,
                budgets = budgets.associate { it.categoryId to it.amount }
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CategoriesUiState(Dates.currentMonthKey(), 0.0, emptyList())
    )

    fun previousMonth() = repo.shiftMonth(-1)
    fun nextMonth() = repo.shiftMonth(1)
    fun currentMonth() = repo.goToCurrentMonth()
}
