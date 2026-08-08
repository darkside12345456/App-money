package com.despesas.gestor.ui.screens.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despesas.gestor.data.local.dao.ShoppingListSummary
import com.despesas.gestor.data.local.entity.ShoppingItemEntity
import com.despesas.gestor.data.local.entity.ShoppingListEntity
import com.despesas.gestor.data.repository.GestorRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListsViewModel(private val repo: GestorRepository) : ViewModel() {

    val lists: StateFlow<List<ShoppingListSummary>> =
        repo.observeShoppingListSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createList(name: String) {
        viewModelScope.launch { repo.createShoppingList(name) }
    }

    fun deleteList(summary: ShoppingListSummary) {
        viewModelScope.launch {
            repo.deleteShoppingList(
                ShoppingListEntity(summary.id, summary.name, summary.createdAtMillis)
            )
        }
    }
}

data class ShoppingDetailUiState(
    val list: ShoppingListEntity? = null,
    val items: List<ShoppingItemEntity> = emptyList()
)

class ShoppingDetailViewModel(
    private val repo: GestorRepository,
    private val listId: Long
) : ViewModel() {

    val list: StateFlow<ShoppingListEntity?> =
        repo.observeShoppingList(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val items: StateFlow<List<ShoppingItemEntity>> =
        repo.observeShoppingItems(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.addShoppingItem(listId, name.trim()) }
    }

    fun toggle(item: ShoppingItemEntity) {
        viewModelScope.launch { repo.updateShoppingItem(item.copy(checked = !item.checked)) }
    }

    fun delete(item: ShoppingItemEntity) {
        viewModelScope.launch { repo.deleteShoppingItem(item) }
    }
}
