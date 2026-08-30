package com.despesas.gestor.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Definições simples da app guardadas em SharedPreferences (locais).
 */
class AppPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gestor_prefs", Context.MODE_PRIVATE)

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK, value).apply()

    var billNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BILL_NOTIF, false)
        set(value) = prefs.edit().putBoolean(KEY_BILL_NOTIF, value).apply()

    var cloudSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_SYNC, false)
        set(value) = prefs.edit().putBoolean(KEY_CLOUD_SYNC, value).apply()

    var householdCode: String?
        get() = prefs.getString(KEY_HOUSEHOLD, null)
        set(value) = prefs.edit().putString(KEY_HOUSEHOLD, value).apply()

    /** Leitura de faturas com IA (Gemini). Usada só para ler a fatura. */
    var aiReceiptEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_RECEIPT, false)
        set(value) = prefs.edit().putBoolean(KEY_AI_RECEIPT, value).apply()

    var geminiApiKey: String?
        get() = prefs.getString(KEY_GEMINI_KEY, null)
        set(value) = prefs.edit().putString(KEY_GEMINI_KEY, value).apply()

    /** Identificador estável deste telemóvel (para ignorar as próprias escritas). */
    val deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) return existing
            val generated = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            return generated
        }

    /** Emite o valor de uma chave booleana e reage a alterações. */
    fun observeBoolean(key: String, default: Boolean): Flow<Boolean> = callbackFlow {
        trySend(prefs.getBoolean(key, default))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, changed ->
            if (changed == key) trySend(p.getBoolean(key, default))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        const val KEY_APP_LOCK = "app_lock_enabled"
        const val KEY_BILL_NOTIF = "bill_notifications_enabled"
        const val KEY_CLOUD_SYNC = "cloud_sync_enabled"
        const val KEY_HOUSEHOLD = "household_code"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_AI_RECEIPT = "ai_receipt_enabled"
        const val KEY_GEMINI_KEY = "gemini_api_key"
    }
}
