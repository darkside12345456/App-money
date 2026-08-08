package com.despesas.gestor.ui.screens.fixed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despesas.gestor.data.local.entity.FixedExpenseEntity
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FixedUiState(
    val monthLabel: String,
    val total: Double,
    val expenses: List<FixedExpenseEntity>
)

class FixedExpensesViewModel(private val repo: GestorRepository) : ViewModel() {
    private val monthKey = Dates.currentMonthKey()

    val state: StateFlow<FixedUiState> = combine(
        repo.observeFixedExpenses(monthKey),
        repo.observeFixedTotal(monthKey)
    ) { list, total ->
        FixedUiState(Dates.monthLabel(monthKey), total, list)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FixedUiState(Dates.monthLabel(monthKey), 0.0, emptyList())
    )

    fun add(name: String, provider: String?, amount: Double, dateMillis: Long, paid: Boolean) {
        viewModelScope.launch { repo.addFixedExpense(name, provider, amount, dateMillis, paid) }
    }

    fun togglePaid(expense: FixedExpenseEntity) {
        viewModelScope.launch { repo.updateFixedExpense(expense.copy(paid = !expense.paid)) }
    }

    fun delete(expense: FixedExpenseEntity) {
        viewModelScope.launch { repo.deleteFixedExpense(expense) }
    }
}
