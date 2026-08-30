package com.despesas.gestor.ui.screens.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.despesas.gestor.domain.model.ExpenseCategory
import com.despesas.gestor.ui.components.AppCard
import com.despesas.gestor.ui.components.CategoryAvatar
import com.despesas.gestor.ui.components.EmptyState
import com.despesas.gestor.ui.components.MonthBar
import com.despesas.gestor.ui.repositoryViewModelFactory
import com.despesas.gestor.util.Money

@Composable
fun CategoriesScreen(
    onOpenCategory: (String) -> Unit,
    viewModel: CategoriesViewModel = viewModel(
        factory = repositoryViewModelFactory { CategoriesViewModel(it) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Categorias", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        MonthBar(
            monthLabel = state.monthLabel,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            isCurrentMonth = state.isCurrentMonth,
            onCurrent = viewModel::currentMonth
        )
        Text(
            "Total: ${Money.format(state.total)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        if (state.categories.isEmpty()) {
            EmptyState(
                title = "Sem despesas este mês",
                subtitle = "Tira uma foto a uma fatura para começar a organizar por categorias.",
                icon = Icons.Outlined.Category
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.categories, key = { it.categoryId }) { ct ->
                    val cat = ExpenseCategory.fromId(ct.categoryId)
                    AppCard(modifier = Modifier.clickable { onOpenCategory(cat.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryAvatar(cat)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cat.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${ct.receiptCount} fatura(s)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                Money.format(ct.total),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
