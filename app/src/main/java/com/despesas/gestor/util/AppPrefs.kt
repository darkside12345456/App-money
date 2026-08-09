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
    }
}
