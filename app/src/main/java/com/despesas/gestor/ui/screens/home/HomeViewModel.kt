package com.despesas.gestor.ui.screens.home

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
import kotlinx.coroutines.launch

data class HomeUiState(
    val monthKey: String = Dates.currentMonthKey(),
    val income: Double = 0.0,
    val receiptsTotal: Double = 0.0,
    val fixedTotal: Double = 0.0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val budgets: Map<String, Double> = emptyMap()
) {
    val spent: Double get() = receiptsTotal + fixedTotal
    val remaining: Double get() = income - spent
    val budgetFraction: Float
        get() = if (income > 0) (spent / income).toFloat() else 0f
    val monthLabel: String get() = Dates.monthLabel(monthKey)
    val hasIncome: Boolean get() = income > 0
    val isCurrentMonth: Boolean get() = monthKey == Dates.currentMonthKey()
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: GestorRepository) : ViewModel() {

    val state: StateFlow<HomeUiState> = repo.selectedMonth.flatMapLatest { monthKey ->
        combine(
            repo.observeEffectiveIncome(monthKey),
            repo.observeReceiptsTotal(monthKey),
            repo.observeFixedTotal(monthKey),
            repo.observeCategoryTotals(monthKey),
            repo.observeBudgets()
        ) { income, receipts, fixed, categories, budgets ->
            HomeUiState(
                monthKey = monthKey,
                income = income?.amount ?: 0.0,
                receiptsTotal = receipts,
                fixedTotal = fixed,
                categoryTotals = categories,
                budgets = budgets.associate { it.categoryId to it.amount }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setIncome(amount: Double) {
        viewModelScope.launch { repo.setIncome(repo.selectedMonth.value, amount) }
    }

    fun previousMonth() = repo.shiftMonth(-1)
    fun nextMonth() = repo.shiftMonth(1)
    fun currentMonth() = repo.goToCurrentMonth()
}
