package com.despesas.gestor.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despesas.gestor.data.local.entity.BudgetEntity
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.data.sync.CoupleSyncManager
import com.despesas.gestor.data.sync.SyncStatus
import com.despesas.gestor.util.AppPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: GestorRepository,
    private val prefs: AppPrefs,
    private val coupleSync: CoupleSyncManager
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = coupleSync.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncStatus.Off)

    val householdCode: String? get() = prefs.householdCode
    val cloudSyncEnabled: Boolean get() = prefs.cloudSyncEnabled

    fun enableSync(code: String) = coupleSync.enable(code)
    fun disableSync() = coupleSync.disable()

    val budgets: StateFlow<List<BudgetEntity>> =
        repo.observeBudgets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appLockEnabled: StateFlow<Boolean> =
        prefs.observeBoolean(AppPrefs.KEY_APP_LOCK, false)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), prefs.appLockEnabled)

    val notificationsEnabled: StateFlow<Boolean> =
        prefs.observeBoolean(AppPrefs.KEY_BILL_NOTIF, false)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), prefs.billNotificationsEnabled)

    val aiReceiptEnabled: StateFlow<Boolean> =
        prefs.observeBoolean(AppPrefs.KEY_AI_RECEIPT, false)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), prefs.aiReceiptEnabled)

    val geminiApiKey: String? get() = prefs.geminiApiKey

    fun setAiReceiptEnabled(enabled: Boolean) { prefs.aiReceiptEnabled = enabled }
    fun setGeminiApiKey(key: String) { prefs.geminiApiKey = key.trim().ifBlank { null } }

    fun setBudget(categoryId: String, amount: Double) {
        viewModelScope.launch { repo.setBudget(categoryId, amount) }
    }

    fun setAppLock(enabled: Boolean) { prefs.appLockEnabled = enabled }

    fun setNotifications(enabled: Boolean) { prefs.billNotificationsEnabled = enabled }

    suspend fun exportJson(): String = repo.exportBackup()

    suspend fun importJson(json: String) = repo.importBackup(json)
}
