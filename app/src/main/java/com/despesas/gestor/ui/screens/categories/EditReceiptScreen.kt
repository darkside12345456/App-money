package com.despesas.gestor.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.despesas.gestor.data.local.entity.ReceiptEntity
import com.despesas.gestor.data.local.entity.ReceiptItemEntity
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.domain.model.ExpenseCategory
import com.despesas.gestor.ui.components.AppCard
import com.despesas.gestor.ui.repositoryViewModelFactory
import com.despesas.gestor.ui.screens.capture.EditableItem
import com.despesas.gestor.ui.screens.capture.ReviewData
import com.despesas.gestor.util.Dates
import com.despesas.gestor.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EditState {
    data object Loading : EditState
    data class Editing(val data: ReviewData) : EditState
    data object Saved : EditState
}

class EditReceiptViewModel(
    private val repo: GestorRepository,
    private val receiptId: Long
) : ViewModel() {

    private val _state = MutableStateFlow<EditState>(EditState.Loading)
    val state: StateFlow<EditState> = _state.asStateFlow()

    private var uidCounter = 0L
    private var imagePath: String? = null
    private var rawText: String? = null

    init {
        viewModelScope.launch {
            val loaded = repo.getReceiptWithItems(receiptId)
            if (loaded != null) {
                imagePath = loaded.receipt.imagePath
                rawText = loaded.receipt.rawText
                _state.value = EditState.Editing(
                    ReviewData(
                        merchant = loaded.receipt.merchant,
                        category = ExpenseCategory.fromId(loaded.receipt.categoryId),
                        dateMillis = loaded.receipt.dateMillis,
                        total = formatPrice(loaded.receipt.total),
                        items = loaded.items.map {
                            EditableItem(uidCounter++, it.name, formatPrice(it.price))
                        },
                        imagePath = imagePath,
                        rawText = rawText,
                        autoItemCount = loaded.items.size
                    )
                )
            }
        }
    }

    private fun edit(transform: (ReviewData) -> ReviewData) {
        val current = _state.value
        if (current is EditState.Editing) _state.value = EditState.Editing(transform(current.data))
    }

    fun setMerchant(v: String) = edit { it.copy(merchant = v) }
    fun setCategory(v: ExpenseCategory) = edit { it.copy(category = v) }
    fun setDate(v: Long) = edit { it.copy(dateMillis = v) }
    fun setTotal(v: String) = edit { it.copy(total = sanitize(v)) }
    fun setItemName(uid: Long, v: String) = edit { d ->
        d.copy(items = d.items.map { if (it.uid == uid) it.copy(name = v) else it })
    }
    fun setItemPrice(uid: Long, v: String) = edit { d ->
        d.copy(items = d.items.map { if (it.uid == uid) it.copy(price = sanitize(v)) else it })
    }
    fun removeItem(uid: Long) = edit { d -> d.copy(items = d.items.filterNot { it.uid == uid }) }
    fun addItem() = edit { d -> d.copy(items = d.items + EditableItem(uidCounter++, "", "")) }

    fun itemsSum(): Double =
        (_state.value as? EditState.Editing)?.data?.items?.sumOf { parsePrice(it.price) } ?: 0.0

    fun save() {
        val data = (_state.value as? EditState.Editing)?.data ?: return
        viewModelScope.launch {
            val items = data.items.filter { it.name.isNotBlank() }.map {
                ReceiptItemEntity(receiptId = receiptId, name = it.name.trim(), price = parsePrice(it.price))
            }
            repo.updateReceiptWithItems(
                ReceiptEntity(
                    id = receiptId,
                    merchant = data.merchant.ifBlank { "Fatura" },
                    categoryId = data.category.id,
                    total = parsePrice(data.total),
                    dateMillis = data.dateMillis,
                    monthKey = Dates.monthKey(data.dateMillis),
                    imagePath = imagePath,
                    rawText = rawText
                ),
                items
            )
            _state.value = EditState.Saved
        }
    }

    private fun sanitize(v: String) = v.filter { it.isDigit() || it == ',' || it == '.' }
    private fun parsePrice(v: String) = v.replace('.', ',').replace(',', '.').toDoubleOrNull() ?: 0.0
    private fun formatPrice(v: Double) =
        if (v == 0.0) "" else String.format("%.2f", v).replace('.', ',')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReceiptScreen(
    receiptId: Long,
    onDone: () -> Unit
) {
    val viewModel: EditReceiptViewModel = viewModel(
        key = "edit_$receiptId",
        factory = repositoryViewModelFactory { EditReceiptViewModel(it, receiptId) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state) { if (state is EditState.Saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar fatura") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is EditState.Editing -> {
                val data = s.data
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = data.merchant,
                        onValueChange = viewModel::setMerchant,
                        label = { Text("Comerciante") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    CategoryDropdownEdit(data.category, viewModel::setCategory)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = Dates.formatDate(data.dateMillis),
                            onValueChange = {},
                            label = { Text("Data") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = data.total,
                            onValueChange = viewModel::setTotal,
                            label = { Text("Total (€)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    TextButton(onClick = { showDatePicker = true }) { Text("Alterar data") }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Itens", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = viewModel::addItem) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Adicionar")
                        }
                    }
                    AppCard {
                        Column {
                            data.items.forEach { item ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = item.name,
                                        onValueChange = { viewModel.setItemName(item.uid, it) },
                                        label = { Text("Item") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.6f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = item.price,
                                        onValueChange = { viewModel.setItemPrice(item.uid, it) },
                                        label = { Text("€") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.removeItem(item.uid) }) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Remover",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Soma dos itens: ${Money.format(viewModel.itemsSum())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = viewModel::save,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("Guardar alterações") }
                    Spacer(Modifier.height(24.dp))
                }

                if (showDatePicker) {
                    val pickerState = rememberDatePickerState(initialSelectedDateMillis = data.dateMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                pickerState.selectedDateMillis?.let { viewModel.setDate(it) }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                        }
                    ) { DatePicker(state = pickerState) }
                }
            }
            else -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdownEdit(
    selected: ExpenseCategory,
    onSelect: (ExpenseCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        androidx.compose.material3.ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ExpenseCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = { onSelect(category); expanded = false }
                )
            }
        }
    }
}
