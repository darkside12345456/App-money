package com.despesas.gestor.data.sync

import android.content.Context
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.util.AppPrefs
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface SyncStatus {
    data object Off : SyncStatus
    data object Connecting : SyncStatus
    data class Connected(val code: String, val lastSyncMillis: Long?) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

/**
 * Sincronização opcional "de casal" via Firebase Firestore.
 *
 * Modelo simples e adequado a duas pessoas: toda a base de dados é serializada
 * (o mesmo formato da cópia de segurança) e guardada num único documento
 * `households/{código}`. Quando um telemóvel altera dados, envia o retrato
 * completo; o outro recebe em tempo real e substitui os dados locais.
 * Regra de conflito: vence a última escrita (aceitável para 2 utilizadores).
 *
 * Fica totalmente inativo enquanto a partilha não for ligada, pelo que não
 * afeta quem não a usa.
 */
class CoupleSyncManager(
    context: Context,
    private val repo: GestorRepository,
    private val prefs: AppPrefs
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deviceId get() = prefs.deviceId

    private var registration: ListenerRegistration? = null
    private var pushJob: Job? = null
    private var connectJob: Job? = null

    @Volatile private var applyingRemote = false

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Off)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** Reinicia a ligação ao arrancar a app, se a partilha estava ligada. */
    fun bootstrap() {
        if (prefs.cloudSyncEnabled && !prefs.householdCode.isNullOrBlank()) restart()
    }

    fun enable(code: String) {
        val clean = code.trim()
        prefs.cloudSyncEnabled = true
        prefs.householdCode = clean
        restart()
    }

    fun disable() {
        prefs.cloudSyncEnabled = false
        stop()
        _status.value = SyncStatus.Off
    }

    private fun restart() {
        stop()
        val code = prefs.householdCode?.trim().orEmpty()
        if (code.isEmpty()) return
        _status.value = SyncStatus.Connecting
        connectJob = scope.launch {
            try {
                connect(code)
            } catch (e: Exception) {
                _status.value = SyncStatus.Error(e.message ?: "Falha na ligação")
            }
        }
    }

    private fun stop() {
        registration?.remove()
        registration = null
        pushJob?.cancel()
        pushJob = null
        connectJob?.cancel()
        connectJob = null
    }

    @OptIn(FlowPreview::class)
    private suspend fun connect(code: String) {
        val app = FirebaseConfig.app(appContext)
        val auth = FirebaseAuth.getInstance(app)
        if (auth.currentUser == null) auth.signInAnonymously().await()
        val db = FirebaseFirestore.getInstance(app)
        val doc = db.collection("households").document(code)

        // Adotar dados existentes do par, ou semear com os nossos se estiver vazio.
        val snap = doc.get().await()
        val remoteData = snap.getString("data")
        val remoteBy = snap.getString("updatedBy")
        if (!remoteData.isNullOrBlank() && remoteBy != deviceId) {
            applyRemote(remoteData)
        } else if (remoteData.isNullOrBlank()) {
            pushNow(doc)
        }

        // Ouvir alterações do par em tempo real.
        registration = doc.addSnapshotListener { s, e ->
            if (e != null || s == null || !s.exists()) return@addSnapshotListener
            if (s.getString("updatedBy") == deviceId) return@addSnapshotListener
            val data = s.getString("data") ?: return@addSnapshotListener
            scope.launch { applyRemote(data); _status.value = SyncStatus.Connected(code, now()) }
        }

        // Enviar quando o utilizador altera algo localmente (com atraso).
        pushJob = scope.launch {
            repo.dataChanged.debounce(1500L).collect {
                if (!applyingRemote) runCatching { pushNow(doc) }
            }
        }

        _status.value = SyncStatus.Connected(code, now())
    }

    private suspend fun applyRemote(json: String) {
        applyingRemote = true
        runCatching { repo.importBackup(json) }
        delay(300)
        applyingRemote = false
    }

    private suspend fun pushNow(doc: DocumentReference) {
        val json = repo.exportBackup()
        val payload = mapOf(
            "data" to json,
            "updatedBy" to deviceId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()
        _status.value = (_status.value as? SyncStatus.Connected)?.copy(lastSyncMillis = now())
            ?: _status.value
    }

    private fun now() = System.currentTimeMillis()

    // --- Task -> coroutine -----------------------------------------------------
    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
}
