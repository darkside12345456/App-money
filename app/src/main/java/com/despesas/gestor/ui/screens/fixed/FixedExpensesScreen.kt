package com.despesas.gestor.ui.screens.fixed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despesas.gestor.data.local.entity.FixedExpenseEntity
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FixedUiState(
    val monthKey: String,
    val total: Double,
    val expenses: List<FixedExpenseEntity>
) {
    val monthLabel: String get() = Dates.monthLabel(monthKey)
    val isCurrentMonth: Boolean get() = monthKey == Dates.currentMonthKey()
}

@OptIn(ExperimentalCoroutinesApi::class)
class FixedExpensesViewModel(private val repo: GestorRepository) : ViewModel() {

    val state: StateFlow<FixedUiState> = repo.selectedMonth.flatMapLatest { monthKey ->
        combine(
            repo.observeFixedExpenses(monthKey),
            repo.observeFixedTotal(monthKey)
        ) { list, total ->
            FixedUiState(monthKey, total, list)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FixedUiState(Dates.currentMonthKey(), 0.0, emptyList())
    )

    fun add(
        name: String,
        provider: String?,
        amount: Double,
        dateMillis: Long,
        paid: Boolean,
        recurring: Boolean
    ) {
        viewModelScope.launch {
            repo.addFixedExpense(name, provider, amount, dateMillis, paid, recurring)
        }
    }

    fun togglePaid(expense: FixedExpenseEntity) {
        viewModelScope.launch { repo.updateFixedExpense(expense.copy(paid = !expense.paid)) }
    }

    fun delete(expense: FixedExpenseEntity) {
        viewModelScope.launch { repo.deleteFixedExpense(expense) }
    }

    /** Copia contas recorrentes do mês anterior para o mês selecionado. */
    fun copyRecurring(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val created = repo.copyRecurringInto(repo.selectedMonth.value)
            onResult(created)
        }
    }

    fun previousMonth() = repo.shiftMonth(-1)
    fun nextMonth() = repo.shiftMonth(1)
    fun currentMonth() = repo.goToCurrentMonth()
}
