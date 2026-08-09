package com.despesas.gestor.ui.screens.balance

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
import java.time.YearMonth

data class MonthSpend(val monthKey: String, val total: Double)

data class BalanceUiState(
    val monthKey: String = Dates.currentMonthKey(),
    val income: Double = 0.0,
    val spent: Double = 0.0,
    val remaining: Double = 0.0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val monthlyTotals: List<MonthSpend> = emptyList(),
    val previousSpent: Double? = null
) {
    val vsPrevious: Double? get() = previousSpent?.let { spent - it }
    val monthLabel: String get() = Dates.monthLabel(monthKey)
    val isCurrentMonth: Boolean get() = monthKey == Dates.currentMonthKey()
}

@OptIn(ExperimentalCoroutinesApi::class)
class BalanceViewModel(private val repo: GestorRepository) : ViewModel() {

    val state: StateFlow<BalanceUiState> = repo.selectedMonth.flatMapLatest { monthKey ->
        combine(
            repo.observeEffectiveIncome(monthKey),
            repo.observeReceiptsTotal(monthKey),
            repo.observeFixedTotal(monthKey),
            repo.observeCategoryTotals(monthKey),
            combine(
                repo.observeRecentMonthTotals(12),
                repo.observeRecentFixedMonthTotals(12)
            ) { receipts, fixed -> receipts to fixed }
        ) { income, receiptsTotal, fixedTotal, categories, monthly ->
            val (receiptMonths, fixedMonths) = monthly
            val merged = HashMap<String, Double>()
            receiptMonths.forEach { merged[it.monthKey] = (merged[it.monthKey] ?: 0.0) + it.total }
            fixedMonths.forEach { merged[it.monthKey] = (merged[it.monthKey] ?: 0.0) + it.total }

            // 6 meses até ao mês selecionado, em ordem cronológica.
            val current = YearMonth.parse(monthKey)
            val series = (5 downTo 0).map { back ->
                val m = current.minusMonths(back.toLong()).toString()
                MonthSpend(m, merged[m] ?: 0.0)
            }
            val prevKey = current.minusMonths(1).toString()

            BalanceUiState(
                monthKey = monthKey,
                income = income?.amount ?: 0.0,
                spent = receiptsTotal + fixedTotal,
                remaining = (income?.amount ?: 0.0) - (receiptsTotal + fixedTotal),
                categoryTotals = categories,
                monthlyTotals = series,
                previousSpent = merged[prevKey]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BalanceUiState())

    fun previousMonth() = repo.shiftMonth(-1)
    fun nextMonth() = repo.shiftMonth(1)
    fun currentMonth() = repo.goToCurrentMonth()
}
