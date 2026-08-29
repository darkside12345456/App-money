package com.despesas.gestor.ui.screens.shopping

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.despesas.gestor.ui.components.AppCard
import com.despesas.gestor.ui.components.EmptyState
import com.despesas.gestor.ui.repositoryViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingDetailScreen(
    listId: Long,
    onBack: () -> Unit
) {
    val viewModel: ShoppingDetailViewModel = viewModel(
        key = "shopping_$listId",
        factory = repositoryViewModelFactory { ShoppingDetailViewModel(it, listId) }
    )
    val list by viewModel.list.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    var newItem by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Ditado por voz: usa o reconhecimento de voz do próprio Android. Cada frase
    // reconhecida é adicionada à lista (separando por vírgulas vários itens).
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        spoken.split(",", ";", " e ", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { viewModel.addItem(it) }
    }

    fun startDictation() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diz os artigos (ex.: leite, pão, ovos)")
        }
        runCatching { voiceLauncher.launch(intent) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.name ?: "Lista") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Campo para adicionar item rapidamente.
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    label = { Text("Adicionar item") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { startDictation() }) {
                    Icon(Icons.Filled.Mic, contentDescription = "Ditar por voz")
                }
                IconButton(
                    onClick = {
                        viewModel.addItem(newItem)
                        newItem = ""
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Adicionar")
                }
            }

            if (items.isEmpty()) {
                EmptyState(
                    title = "Lista vazia",
                    subtitle = "Adiciona itens à tua lista de compras.",
                    icon = Icons.Outlined.ShoppingCart
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.checked,
                                    onCheckedChange = { viewModel.toggle(item) }
                                )
                                Text(
                                    item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (item.checked)
                                        TextDecoration.LineThrough else null,
                                    color = if (item.checked)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.delete(item) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Apagar",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}
