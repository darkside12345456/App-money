package com.despesas.gestor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.despesas.gestor.util.Money

/** Um valor para o gráfico de barras de comparação mensal. */
data class BarValue(val label: String, val value: Double)

/**
 * Gráfico de barras verticais simples, desenhado só com composables (sem
 * bibliotecas externas). Usado para comparar o gasto total entre meses.
 */
@Composable
fun MonthlyBarChart(
    values: List<BarValue>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    if (values.isEmpty()) return
    val max = (values.maxOfOrNull { it.value } ?: 0.0).coerceAtLeast(1.0)
    Row(
        modifier = modifier.fillMaxWidth().height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEach { bar ->
            val fraction = (bar.value / max).toFloat().coerceIn(0.02f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    Money.format(bar.value),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 4.dp)
                        .height((110 * fraction).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(barColor)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    bar.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Um segmento da barra de repartição por categoria. */
data class Segment(val label: String, val value: Double, val color: Color)

/**
 * Barra horizontal empilhada que mostra a repartição do gasto por categoria.
 */
@Composable
fun StackedCategoryBar(
    segments: List<Segment>,
    modifier: Modifier = Modifier
) {
    val total = segments.sumOf { it.value }
    if (total <= 0.0) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        segments.forEach { seg ->
            val weight = (seg.value / total).toFloat().coerceAtLeast(0.001f)
            Box(
                Modifier
                    .weight(weight)
                    .fillMaxWidth()
                    .background(seg.color)
            )
        }
    }
}
