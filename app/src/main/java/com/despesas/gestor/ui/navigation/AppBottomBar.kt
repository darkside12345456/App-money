package com.despesas.gestor.ui.navigation

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Barra de navegação inferior minimalista com um botão central destacado para
 * capturar uma fatura — o gesto mais frequente, a poucos toques de distância.
 */
@Composable
fun AppBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    onCapture: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BarItem(BottomTab.HOME, currentRoute, onSelect, Modifier.weight(1f))
            BarItem(BottomTab.CATEGORIES, currentRoute, onSelect, Modifier.weight(1f))
            CaptureButton(onCapture, Modifier.weight(1f))
            BarItem(BottomTab.FIXED, currentRoute, onSelect, Modifier.weight(1f))
            BarItem(BottomTab.SHOPPING, currentRoute, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BarItem(
    tab: BottomTab,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = currentRoute == tab.route
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier.clickable { onSelect(tab.route) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(tab.label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun CaptureButton(onCapture: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onCapture() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PhotoCamera,
                contentDescription = "Nova fatura",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// Ícone reutilizável não usado diretamente aqui, mas mantido para clareza.
@Suppress("unused")
private val cameraIcon: ImageVector = Icons.Outlined.PhotoCamera
