package com.despesas.gestor

import android.app.Application
import com.despesas.gestor.data.local.AppDatabase
import com.despesas.gestor.data.ai.GeminiReceiptReader
import com.despesas.gestor.data.ocr.OcrService
import com.despesas.gestor.data.repository.GestorRepository
import com.despesas.gestor.data.sync.CoupleSyncManager
import com.despesas.gestor.util.AppPrefs

/**
 * Application com um contentor de dependências manual e simples.
 * Evita frameworks de DI e mantém tudo explícito e fácil de seguir.
 */
class GestorApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Retoma a sincronização de casal se estava ligada.
        runCatching { container.coupleSync.bootstrap() }
    }
}

class AppContainer(app: GestorApp) {
    private val database = AppDatabase.get(app)
    private val ocrService = OcrService(app)
    val prefs = AppPrefs(app)
    private val aiReader = GeminiReceiptReader(app, prefs)
    val repository = GestorRepository(app, database, ocrService, aiReader)
    val coupleSync = CoupleSyncManager(app, repository, prefs)
}
