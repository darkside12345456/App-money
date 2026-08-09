package com.despesas.gestor.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.despesas.gestor.domain.model.ExpenseCategory
import com.despesas.gestor.ui.components.AppCard
import com.despesas.gestor.ui.components.CategoryAvatar
import com.despesas.gestor.ui.containerViewModelFactory
import com.despesas.gestor.util.Money
import com.despesas.gestor.util.notifications.BillReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = containerViewModelFactory { SettingsViewModel(it.repository, it.prefs) }
    )
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val appLock by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsEnabled.collectAsStateWithLifecycle()

    var message by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<ExpenseCategory?>(null) }

    // Ativa as notificações após conceder (ou dispensar) a permissão.
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.setNotifications(true)
        BillReminderScheduler.schedule(context)
    }

    fun enableNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setNotifications(true)
            BillReminderScheduler.schedule(context)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = viewModel.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }
            }.onSuccess { message = "Cópia de segurança exportada." }
                .onFailure { message = "Falha ao exportar: ${it.message}" }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: error("ficheiro vazio")
                viewModel.importJson(json)
            }.onSuccess { message = "Dados importados com sucesso." }
                .onFailure { message = "Falha ao importar: ${it.message}" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Definições") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- Orçamentos ---
            Text("Orçamentos por categoria", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Define um limite mensal e recebe um aviso quando o ultrapassas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            AppCard {
                Column {
                    val budgetMap = budgets.associate { it.categoryId to it.amount }
                    val cats = ExpenseCategory.entries
                    cats.forEachIndexed { index, cat ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryAvatar(cat, size = 36)
                            Spacer(Modifier.width(12.dp))
                            Text(cat.displayName, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            val value = budgetMap[cat.id]
                            Text(
                                if (value != null) Money.format(value) else "—",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (value != null) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outline
                            )
                            TextButton(onClick = { editing = cat }) { Text("Definir") }
                        }
                        if (index < cats.lastIndex) HorizontalDivider()
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            // --- Cópia de segurança ---
            Text("Cópia de segurança", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Os dados ficam só no telemóvel. Exporta um ficheiro para não os perderes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { exportLauncher.launch("despesas-backup.json") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Importar")
                }
            }

            Spacer(Modifier.height(24.dp))
            // --- Segurança e avisos ---
            Text("Segurança e avisos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            AppCard {
                Column {
                    SettingSwitch(
                        title = "Bloqueio com biometria",
                        subtitle = "Pede impressão digital / rosto ao abrir a app.",
                        checked = appLock,
                        onCheckedChange = viewModel::setAppLock
                    )
                    HorizontalDivider()
                    SettingSwitch(
                        title = "Lembrete de contas por pagar",
                        subtitle = "Aviso diário se houver contas do mês por pagar.",
                        checked = notifications,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                enableNotifications()
                            } else {
                                viewModel.setNotifications(false)
                                BillReminderScheduler.cancel(context)
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    val cat = editing
    if (cat != null) {
        BudgetDialog(
            category = cat,
            current = budgets.firstOrNull { it.categoryId == cat.id }?.amount,
            onDismiss = { editing = null },
            onConfirm = { amount ->
                viewModel.setBudget(cat.id, amount)
                editing = null
            }
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
            text = { Text(it) }
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BudgetDialog(
    category: ExpenseCategory,
    current: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember {
        mutableStateOf(current?.let { String.format("%.2f", it).replace('.', ',') } ?: "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Orçamento · ${category.displayName}") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                label = { Text("Limite mensal (€) — 0 para remover") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val v = text.replace('.', ',').replace(',', '.').toDoubleOrNull() ?: 0.0
                onConfirm(v)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
