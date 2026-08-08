package com.despesas.gestor.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.despesas.gestor.domain.model.ExpenseCategory
import com.despesas.gestor.ui.components.AppCard
import com.despesas.gestor.ui.components.CategoryAvatar
import com.despesas.gestor.ui.components.LegendDot
import com.despesas.gestor.ui.components.Segment
import com.despesas.gestor.ui.components.StackedCategoryBar
import com.despesas.gestor.ui.components.ThinProgressBar
import com.despesas.gestor.ui.components.color
import com.despesas.gestor.ui.repositoryViewModelFactory
import com.despesas.gestor.util.Money
import androidx.compose.foundation.text.KeyboardOptions as FKeyboardOptions

@Composable
fun HomeScreen(
    onCapture: () -> Unit,
    onOpenBalance: () -> Unit,
    onOpenCategory: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = repositoryViewModelFactory { HomeViewModel(it) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingIncome by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            state.monthLabel,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "Balanço do mês",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        BalanceCard(state, onEditIncome = { editingIncome = true })

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.PhotoCamera,
                label = "Nova fatura",
                onClick = onCapture
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.BarChart,
                label = "Ver balanço",
                onClick = onOpenBalance
            )
        }

        Spacer(Modifier.height(16.dp))
        if (state.categoryTotals.isNotEmpty()) {
            Text("Gastos por categoria", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            CategoryBreakdown(state, onOpenCategory)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (editingIncome) {
        IncomeDialog(
            current = state.income,
            onDismiss = { editingIncome = false },
            onConfirm = {
                viewModel.setIncome(it)
                editingIncome = false
            }
        )
    }
}

@Composable
private fun BalanceCard(state: HomeUiState, onEditIncome: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Sobra do ordenado",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                Money.format(state.remaining),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(16.dp))
            ThinProgressBar(
                fraction = state.budgetFraction,
                color = MaterialTheme.colorScheme.onPrimary,
                track = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat(
                    label = "Ordenado",
                    value = Money.format(state.income),
                    editable = true,
                    onClick = onEditIncome
                )
                MiniStat(label = "Gasto", value = Money.format(state.spent))
            }
            if (!state.hasIncome) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Toca em \"Ordenado\" para definir o teu rendimento mensal.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    editable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
            if (editable) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Editar ordenado",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CategoryBreakdown(state: HomeUiState, onOpenCategory: (String) -> Unit) {
    AppCard {
        Column {
            val segments = state.categoryTotals.map {
                val cat = ExpenseCategory.fromId(it.categoryId)
                Segment(cat.displayName, it.total, cat.color())
            }
            StackedCategoryBar(segments)
            Spacer(Modifier.height(16.dp))
            state.categoryTotals.forEach { ct ->
                val cat = ExpenseCategory.fromId(ct.categoryId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCategory(cat.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryAvatar(cat, size = 36)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cat.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${ct.receiptCount} fatura(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(Money.format(ct.total), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun IncomeDialog(
    current: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember {
        mutableStateOf(if (current > 0) current.toString().replace('.', ',') else "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rendimento mensal") },
        text = {
            Column {
                Text(
                    "Introduz o teu ordenado. É usado como base do orçamento do mês.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Valor (€)") },
                    singleLine = true,
                    keyboardOptions = FKeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = text.replace('.', ',').replace(',', '.').toDoubleOrNull() ?: 0.0
                onConfirm(value)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
