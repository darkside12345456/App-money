package com.despesas.gestor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.despesas.gestor.GestorApp
import com.despesas.gestor.data.repository.GestorRepository

/**
 * Cria uma ViewModelProvider.Factory que fornece o [GestorRepository] a partir
 * do contentor de dependências da Application — sem frameworks de DI.
 */
inline fun <reified VM : ViewModel> repositoryViewModelFactory(
    crossinline create: (GestorRepository) -> VM
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as GestorApp
        create(app.container.repository)
    }
}

fun CreationExtras.app(): GestorApp = this[APPLICATION_KEY] as GestorApp
