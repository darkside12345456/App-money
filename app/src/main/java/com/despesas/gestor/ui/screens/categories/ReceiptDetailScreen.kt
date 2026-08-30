package com.despesas.gestor.ui.screens.categories

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.despesas.gestor.data.local.dao.ReceiptWithItems
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.domain.model.ExpenseCategory
import com.despesas.gestor.ui.components.AppCard
import com.despesas.gestor.ui.components.CategoryAvatar
import com.despesas.gestor.ui.repositoryViewModelFactory
import com.despesas.gestor.util.Dates
import com.despesas.gestor.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ReceiptDetailViewModel(
    private val repo: GestorRepository,
    receiptId: Long
) : ViewModel() {
    val receipt: StateFlow<ReceiptWithItems?> =
        repo.observeReceiptWithItems(receiptId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(onDone: () -> Unit) {
        val current = receipt.value?.receipt ?: return
        viewModelScope.launch {
            repo.deleteReceipt(current)
            com.despesas.gestor.util.ImageStore.delete(current.imagePath)
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receiptId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val viewModel: ReceiptDetailViewModel = viewModel(
        key = "receipt_$receiptId",
        factory = repositoryViewModelFactory { ReceiptDetailViewModel(it, receiptId) }
    )
    val data by viewModel.receipt.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data?.receipt?.merchant ?: "Fatura") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(receiptId) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Apagar")
                    }
                }
            )
        }
    ) { padding ->
        val receiptData = data
        if (receiptData == null) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { Text("A carregar…") }
            return@Scaffold
        }

        val category = ExpenseCategory.fromId(receiptData.receipt.categoryId)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(category)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(category.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                Dates.formatDate(receiptData.receipt.dateMillis),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                Money.format(receiptData.receipt.total),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Itens (${receiptData.items.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (receiptData.items.isEmpty()) {
                item {
                    Text(
                        "O OCR não identificou itens individuais nesta fatura.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item {
                    AppCard {
                        Column {
                            receiptData.items.forEachIndexed { index, it ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(it.name, style = MaterialTheme.typography.bodyLarge)
                                        if (it.quantity != 1.0) {
                                            Text(
                                                "Qtd: ${it.quantity}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        Money.format(it.price),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                if (index < receiptData.items.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            }

            val imagePath = receiptData.receipt.imagePath
            if (imagePath != null && File(imagePath).exists()) {
                item {
                    Text("Foto da fatura", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = "Foto da fatura",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Apagar fatura?") },
            text = { Text("Esta ação remove a fatura e os seus itens.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}
