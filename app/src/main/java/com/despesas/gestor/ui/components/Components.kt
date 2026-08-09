package com.despesas.gestor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.despesas.gestor.domain.model.ExpenseCategory

/** Ícone e cor associados a cada categoria. */
fun ExpenseCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.SUPERMERCADO -> Icons.Filled.ShoppingBasket
    ExpenseCategory.RESTAURACAO -> Icons.Filled.Restaurant
    ExpenseCategory.TRANSPORTES -> Icons.Filled.DirectionsCar
    ExpenseCategory.SAUDE -> Icons.Filled.LocalHospital
    ExpenseCategory.VESTUARIO -> Icons.Filled.Checkroom
    ExpenseCategory.CASA -> Icons.Filled.Home
    ExpenseCategory.LAZER -> Icons.Filled.SportsEsports
    ExpenseCategory.CONTAS -> Icons.Filled.Receipt
    ExpenseCategory.OUTROS -> Icons.Filled.Category
}

fun ExpenseCategory.color(): Color = when (this) {
    ExpenseCategory.SUPERMERCADO -> Color(0xFF19A188)
    ExpenseCategory.RESTAURACAO -> Color(0xFFE0733B)
    ExpenseCategory.TRANSPORTES -> Color(0xFF3F72C4)
    ExpenseCategory.SAUDE -> Color(0xFFDC5B6B)
    ExpenseCategory.VESTUARIO -> Color(0xFF9257C0)
    ExpenseCategory.CASA -> Color(0xFFC49A3F)
    ExpenseCategory.LAZER -> Color(0xFF2FA3B5)
    ExpenseCategory.CONTAS -> Color(0xFF6E7B87)
    ExpenseCategory.OUTROS -> Color(0xFF8A9691)
}

@Composable
fun CategoryAvatar(category: ExpenseCategory, size: Int = 44) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(category.color().copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = category.displayName,
            tint = category.color(),
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

/** Barra de navegação entre meses: `‹  Agosto de 2026  ›`. */
@Composable
fun MonthBar(
    monthLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentMonth: Boolean = true,
    onCurrent: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.material3.IconButton(onClick = onPrevious) {
            Icon(
                androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Mês anterior"
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(monthLabel, style = MaterialTheme.typography.titleMedium)
            if (!isCurrentMonth && onCurrent != null) {
                Text(
                    "Voltar ao mês atual",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onCurrent() }
                )
            }
        }
        androidx.compose.material3.IconButton(onClick = onNext) {
            Icon(
                androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Mês seguinte"
            )
        }
    }
}

/** Cartão simples com cantos arredondados usado por toda a app. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Outlined.Inbox,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Barra de progresso fina (ex.: proporção do orçamento usada). */
@Composable
fun ThinProgressBar(
    fraction: Float,
    color: Color,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Int = 8
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        label = "progress"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Rótulo de percentagem por categoria numa linha. */
@Composable
fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
