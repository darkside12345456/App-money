package com.despesas.gestor.ui.screens.fixed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.despesas.gestor.ui.components.AppCard
import com.despesas.gestor.ui.components.EmptyState
import com.despesas.gestor.ui.components.MonthBar
import com.despesas.gestor.ui.repositoryViewModelFactory
import com.despesas.gestor.util.Dates
import com.despesas.gestor.util.Money
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedExpensesScreen(
    viewModel: FixedExpensesViewModel = viewModel(
        factory = repositoryViewModelFactory { FixedExpensesViewModel(it) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar conta")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("Despesas fixas", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            MonthBar(
                monthLabel = state.monthLabel,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                isCurrentMonth = state.isCurrentMonth,
                onCurrent = viewModel::currentMonth
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total: ${Money.format(state.total)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = {
                    viewModel.copyRecurring { created ->
                        scope.launch {
                            snackbar.showSnackbar(
                                if (created > 0) "$created conta(s) recorrente(s) copiada(s)"
                                else "Sem contas recorrentes para copiar"
                            )
                        }
                    }
                }) {
                    Icon(Icons.Outlined.Repeat, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar recorrentes")
                }
            }
            Spacer(Modifier.height(12.dp))

            if (state.expenses.isEmpty()) {
                EmptyState(
                    title = "Sem contas registadas",
                    subtitle = "Regista as tuas contas de luz, água, gás, internet, etc.",
                    icon = Icons.Outlined.ReceiptLong
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.expenses, key = { it.id }) { expense ->
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.togglePaid(expense) }) {
                                    Icon(
                                        if (expense.paid) Icons.Outlined.CheckCircle
                                        else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = "Pago",
                                        tint = if (expense.paid) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        expense.name +
                                            (expense.provider?.let { " · $it" } ?: ""),
                                        style = MaterialTheme.typography.titleMedium,
                                        textDecoration = if (expense.paid)
                                            TextDecoration.LineThrough else null
                                    )
                                    Text(
                                        Dates.formatDate(expense.dateMillis) +
                                            if (expense.recurring) "  ·  🔁 recorrente" else "",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    Money.format(expense.amount),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(onClick = { viewModel.delete(expense) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Apagar",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAdd) {
        AddFixedExpenseDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, provider, amount, date, paid, recurring ->
                viewModel.add(name, provider, amount, date, paid, recurring)
                showAdd = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddFixedExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Double, Long, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf(false) }
    var recurring by remember { mutableStateOf(true) }
    val dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val presets = listOf("Luz", "Água", "Gás", "Internet", "Telemóvel", "Renda")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova conta") },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = { name = preset },
                            label = { Text(preset) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome (ex.: Luz)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Fornecedor (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Valor (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = paid,
                        onClick = { paid = !paid },
                        label = { Text("Já paga") }
                    )
                    FilterChip(
                        selected = recurring,
                        onClick = { recurring = !recurring },
                        label = { Text("Recorrente") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = amount.replace('.', ',').replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && value > 0) {
                        onConfirm(name.trim(), provider.trim().ifBlank { null }, value, dateMillis, paid, recurring)
                    }
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
