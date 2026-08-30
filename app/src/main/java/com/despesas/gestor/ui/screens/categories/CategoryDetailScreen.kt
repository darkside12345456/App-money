package com.despesas.gestor.ui.screens.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.despesas.gestor.data.local.entity.ReceiptEntity
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.domain.model.ExpenseCategory
import com.despesas.gestor.ui.components.AppCard
import com.despesas.gestor.ui.components.CategoryAvatar
import com.despesas.gestor.ui.components.EmptyState
import com.despesas.gestor.ui.repositoryViewModelFactory
import com.despesas.gestor.util.Dates
import com.despesas.gestor.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CategoryDetailViewModel(
    repo: GestorRepository,
    val categoryId: String
) : ViewModel() {

    val receipts: StateFlow<List<ReceiptEntity>> =
        repo.selectedMonth
            .flatMapLatest { mk -> repo.observeReceiptsForCategory(mk, categoryId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val total: StateFlow<Double> =
        receipts.map { list -> list.sumOf { it.total } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: String,
    onBack: () -> Unit,
    onOpenReceipt: (Long) -> Unit
) {
    val category = ExpenseCategory.fromId(categoryId)
    val viewModel: CategoryDetailViewModel = viewModel(
        key = "cat_$categoryId",
        factory = repositoryViewModelFactory { CategoryDetailViewModel(it, categoryId) }
    )
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val total by viewModel.total.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Total gasto",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(Money.format(total), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            if (receipts.isEmpty()) {
                EmptyState(
                    title = "Sem faturas",
                    subtitle = "Ainda não há faturas nesta categoria este mês.",
                    icon = Icons.Outlined.ReceiptLong
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(receipts, key = { it.id }) { receipt ->
                        AppCard(modifier = Modifier.clickable { onOpenReceipt(receipt.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryAvatar(category, size = 40)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        receipt.merchant,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        Dates.formatDate(receipt.dateMillis),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    Money.format(receipt.total),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
