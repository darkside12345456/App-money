package com.despesas.gestor.ui.screens.shopping

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.despesas.gestor.ui.components.AppCard
import com.despesas.gestor.ui.components.EmptyState
import com.despesas.gestor.ui.components.ThinProgressBar
import com.despesas.gestor.ui.repositoryViewModelFactory

@Composable
fun ShoppingScreen(
    onOpenList: (Long) -> Unit,
    viewModel: ShoppingListsViewModel = viewModel(
        factory = repositoryViewModelFactory { ShoppingListsViewModel(it) }
    )
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nova lista")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("Listas de compras", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            if (lists.isEmpty()) {
                EmptyState(
                    title = "Sem listas",
                    subtitle = "Cria uma lista para organizar as tuas compras.",
                    icon = Icons.Outlined.ShoppingCart
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(lists, key = { it.id }) { summary ->
                        AppCard(modifier = Modifier.clickable { onOpenList(summary.id) }) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            summary.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "${summary.checkedCount}/${summary.itemCount} comprados",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteList(summary) }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Apagar lista",
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                                if (summary.itemCount > 0) {
                                    Spacer(Modifier.height(10.dp))
                                    ThinProgressBar(
                                        fraction = summary.checkedCount.toFloat() / summary.itemCount,
                                        color = MaterialTheme.colorScheme.primary
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

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Nova lista") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da lista") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createList(name.trim())
                        showCreate = false
                    }
                }) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancelar") } }
        )
    }
}
