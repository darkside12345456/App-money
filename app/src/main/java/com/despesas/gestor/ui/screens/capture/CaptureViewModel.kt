package com.despesas.gestor.ui.screens.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despesas.gestor.data.local.entity.ReceiptItemEntity
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Um item editável no ecrã de revisão (após o OCR). */
data class EditableItem(
    val uid: Long,
    val name: String,
    val price: String
)

/** Dados da fatura lida, prontos a rever/corrigir antes de gravar. */
data class ReviewData(
    val merchant: String,
    val category: ExpenseCategory,
    val dateMillis: Long,
    val total: String,
    val items: List<EditableItem>,
    val imagePath: String?,
    val rawText: String?,
    val autoItemCount: Int
)

sealed interface CaptureState {
    data object Idle : CaptureState
    data object Processing : CaptureState
    data class Review(val data: ReviewData) : CaptureState
    data class Error(val message: String) : CaptureState
    data object Saved : CaptureState
}

class CaptureViewModel(private val repo: GestorRepository) : ViewModel() {

    private val _state = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val state: StateFlow<CaptureState> = _state.asStateFlow()

    private var uidCounter = 0L
    private fun nextUid() = uidCounter++

    /** Corre o OCR sobre a foto e passa ao ecrã de revisão. */
    fun processImage(uri: Uri, imagePath: String?) {
        _state.value = CaptureState.Processing
        viewModelScope.launch {
            try {
                val parsed = repo.scanReceipt(uri)
                _state.value = CaptureState.Review(
                    ReviewData(
                        merchant = parsed.merchant,
                        category = parsed.category,
                        dateMillis = parsed.dateMillis,
                        total = formatPrice(parsed.total),
                        items = parsed.items.map {
                            EditableItem(nextUid(), it.name, formatPrice(it.price))
                        },
                        imagePath = imagePath,
                        rawText = parsed.rawText,
                        autoItemCount = parsed.items.size
                    )
                )
            } catch (e: Exception) {
                _state.value = CaptureState.Error(
                    e.message ?: "Não foi possível ler a fatura. Tenta outra foto."
                )
            }
        }
    }

    fun reset() {
        _state.value = CaptureState.Idle
    }

    // --- Edição do formulário de revisão --------------------------------------

    private fun updateReview(transform: (ReviewData) -> ReviewData) {
        val current = _state.value
        if (current is CaptureState.Review) {
            _state.value = CaptureState.Review(transform(current.data))
        }
    }

    fun setMerchant(value: String) = updateReview { it.copy(merchant = value) }
    fun setCategory(value: ExpenseCategory) = updateReview { it.copy(category = value) }
    fun setDate(millis: Long) = updateReview { it.copy(dateMillis = millis) }
    fun setTotal(value: String) = updateReview { it.copy(total = sanitize(value)) }

    fun setItemName(uid: Long, name: String) = updateReview { data ->
        data.copy(items = data.items.map { if (it.uid == uid) it.copy(name = name) else it })
    }

    fun setItemPrice(uid: Long, price: String) = updateReview { data ->
        data.copy(items = data.items.map { if (it.uid == uid) it.copy(price = sanitize(price)) else it })
    }

    fun removeItem(uid: Long) = updateReview { data ->
        data.copy(items = data.items.filterNot { it.uid == uid })
    }

    fun addItem() = updateReview { data ->
        data.copy(items = data.items + EditableItem(nextUid(), "", ""))
    }

    /** Total calculado a partir dos itens (ajuda o utilizador). */
    fun itemsSum(): Double {
        val current = _state.value as? CaptureState.Review ?: return 0.0
        return current.data.items.sumOf { parsePrice(it.price) }
    }

    fun save() {
        val data = (_state.value as? CaptureState.Review)?.data ?: return
        viewModelScope.launch {
            val items = data.items
                .filter { it.name.isNotBlank() }
                .map {
                    ReceiptItemEntity(
                        receiptId = 0,
                        name = it.name.trim(),
                        price = parsePrice(it.price)
                    )
                }
            repo.saveReceipt(
                merchant = data.merchant.ifBlank { "Fatura" },
                categoryId = data.category.id,
                total = parsePrice(data.total),
                dateMillis = data.dateMillis,
                items = items,
                imagePath = data.imagePath,
                rawText = data.rawText
            )
            _state.value = CaptureState.Saved
        }
    }

    private fun sanitize(value: String): String =
        value.filter { it.isDigit() || it == ',' || it == '.' }

    private fun parsePrice(value: String): Double =
        value.replace('.', ',').replace(',', '.').toDoubleOrNull() ?: 0.0

    private fun formatPrice(value: Double): String =
        if (value == 0.0) "" else String.format("%.2f", value).replace('.', ',')
}
