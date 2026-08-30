package com.despesas.gestor.data.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Configuração do Firebase inicializada por código (a partir dos valores do
 * google-services.json), em vez do plugin Gradle. Assim a app compila e corre
 * sem depender do ficheiro no repositório, e o Firebase só é iniciado quando a
 * partilha de casal é ligada.
 *
 * Estes valores não são segredos: vão embutidos em qualquer app Android.
 */
object FirebaseConfig {

    private const val APPLICATION_ID = "1:237436584560:android:775f5ca030dc7ba474eab5"
    private const val API_KEY = "AIzaSyBidGHPsCcuJe06VivNaq2XIA9T1ub9BEA"
    private const val PROJECT_ID = "despesas-8999d"
    private const val SENDER_ID = "237436584560"
    private const val STORAGE_BUCKET = "despesas-8999d.firebasestorage.app"

    private const val APP_NAME = "gestor-despesas"

    /** Inicializa (uma vez) e devolve a FirebaseApp da app. */
    fun app(context: Context): FirebaseApp {
        val existing = FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }
        if (existing != null) return existing

        val options = FirebaseOptions.Builder()
            .setApplicationId(APPLICATION_ID)
            .setApiKey(API_KEY)
            .setProjectId(PROJECT_ID)
            .setGcmSenderId(SENDER_ID)
            .setStorageBucket(STORAGE_BUCKET)
            .build()

        return FirebaseApp.initializeApp(context.applicationContext, options, APP_NAME)
    }
}
