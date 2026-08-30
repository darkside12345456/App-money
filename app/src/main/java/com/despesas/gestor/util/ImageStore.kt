package com.despesas.gestor.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Guarda as fotos das faturas no armazenamento interno da app (privado, nunca
 * na nuvem). Fornece também Uris via FileProvider para a câmara escrever.
 */
object ImageStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }

    /** Cria um ficheiro vazio para a câmara e devolve (ficheiro, uri). */
    fun newCaptureTarget(context: Context): Pair<File, Uri> {
        val file = File(dir(context), "receipt_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return file to uri
    }

    /** Copia uma imagem escolhida (ex.: da galeria) para o armazenamento interno. */
    fun copyFrom(context: Context, uri: Uri): String? = runCatching {
        val file = File(dir(context), "receipt_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
