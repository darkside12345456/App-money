package com.despesas.gestor.ui.screens.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.despesas.gestor.domain.model.ExpenseCategory
import com.despesas.gestor.ui.components.AppCard
import com.despesas.gestor.ui.components.BarValue
import com.despesas.gestor.ui.components.LegendDot
import com.despesas.gestor.ui.components.MonthBar
import com.despesas.gestor.ui.components.MonthlyBarChart
import com.despesas.gestor.ui.components.Segment
import com.despesas.gestor.ui.components.StackedCategoryBar
import com.despesas.gestor.ui.components.color
import com.despesas.gestor.ui.repositoryViewModelFactory
import com.despesas.gestor.util.Dates
import com.despesas.gestor.util.Money
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceScreen(
    onBack: () -> Unit,
    viewModel: BalanceViewModel = viewModel(
        factory = repositoryViewModelFactory { BalanceViewModel(it) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pt = Locale("pt", "PT")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Balanço mensal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            MonthBar(
                monthLabel = state.monthLabel,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                isCurrentMonth = state.isCurrentMonth,
                onCurrent = viewModel::currentMonth
            )
            Spacer(Modifier.height(12.dp))

            // Resumo do mês
            AppCard {
                Column {
                    SummaryRow("Ordenado", Money.format(state.income))
                    Spacer(Modifier.height(8.dp))
                    SummaryRow("Total gasto", Money.format(state.spent))
                    Spacer(Modifier.height(8.dp))
                    SummaryRow(
                        "Sobra",
                        Money.format(state.remaining),
                        highlight = true
                    )
                    val diff = state.vsPrevious
                    if (diff != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val up = diff > 0
                            Icon(
                                if (up) Icons.AutoMirrored.Filled.TrendingUp
                                else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (up) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (up) "Gastaste ${Money.format(diff)} a mais que no mês anterior"
                                else "Gastaste ${Money.format(-diff)} a menos que no mês anterior",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Comparação entre meses", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            AppCard {
                val bars = state.monthlyTotals.map { ms ->
                    val label = YearMonth.parse(ms.monthKey)
                        .month.getDisplayName(TextStyle.SHORT, pt)
                        .replaceFirstChar { it.uppercase() }
                    BarValue(label, ms.total)
                }
                MonthlyBarChart(values = bars)
            }

            Spacer(Modifier.height(16.dp))
            if (state.categoryTotals.isNotEmpty()) {
                Text("Repartição por categoria", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                AppCard {
                    Column {
                        val segments = state.categoryTotals.map {
                            val c = ExpenseCategory.fromId(it.categoryId)
                            Segment(c.displayName, it.total, c.color())
                        }
                        StackedCategoryBar(segments)
                        Spacer(Modifier.height(16.dp))
                        val totalCat = state.categoryTotals.sumOf { it.total }
                        state.categoryTotals.forEach { ct ->
                            val c = ExpenseCategory.fromId(ct.categoryId)
                            val pct = if (totalCat > 0) (ct.total / totalCat * 100).toInt() else 0
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendDot(
                                    color = c.color(),
                                    label = c.displayName,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$pct%  ·  ${Money.format(ct.total)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (highlight) MaterialTheme.typography.titleLarge
            else MaterialTheme.typography.titleMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
