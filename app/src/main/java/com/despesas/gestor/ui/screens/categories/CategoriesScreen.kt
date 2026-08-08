package com.despesas.gestor.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despesas.gestor.data.local.dao.CategoryTotal
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CategoriesUiState(
    val monthLabel: String,
    val total: Double,
    val categories: List<CategoryTotal>
)

class CategoriesViewModel(repo: GestorRepository) : ViewModel() {
    private val monthKey = Dates.currentMonthKey()

    val state: StateFlow<CategoriesUiState> =
        repo.observeCategoryTotals(monthKey).map { list ->
            CategoriesUiState(
                monthLabel = Dates.monthLabel(monthKey),
                total = list.sumOf { it.total },
                categories = list
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CategoriesUiState(Dates.monthLabel(monthKey), 0.0, emptyList())
        )
}
